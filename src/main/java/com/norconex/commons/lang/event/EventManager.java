/* Copyright 2018-2022 Norconex Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.norconex.commons.lang.event;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.norconex.commons.lang.ExceptionUtil;
import com.norconex.commons.lang.Slf4jUtil;
import com.norconex.commons.lang.bean.BeanUtil;

import lombok.Getter;
import lombok.Setter;

/**
 * Manages event listeners and logs events.  New event managers can be
 * constructed with a "parent" event manager. When chained as such,
 * event on a particular event manager are bubbled up to the parents but
 * never down to children. Events are logged by the manager in the chain that
 * first fires the event.
 * @since 2.0.0
 */
public class EventManager {

    private static final Logger LOG =
            LoggerFactory.getLogger(EventManager.class);

    private EventManager parentEventManager;

    private final CopyOnWriteArrayList<EventListener<Event>> listeners =
            new CopyOnWriteArrayList<>();

    @Setter
    @Getter
    private boolean stacktraceLoggingDisabled;

    public EventManager() {
        this(null);
    }

    /**
     * Wraps a parent event manager.  Events fired on this event manager
     * will be fired on this instance listeners first, then its parent
     * listeners.
     * Modifying the list of listeners in this instance does not impact
     * listeners of the parent event manager.
     * @param parentEventManager parent event manager
     */
    public EventManager(EventManager parentEventManager) {
        this.parentEventManager = parentEventManager;
    }

    /**
     * Adds an event listener. Event listeners are added by "identity", meaning
     * you can have multiple listeners coexisting even if their test
     * for equality returns <code>true</code>.
     * Adding a <code>null</code> event listener has no effect.
     * @param listener an event listener
     */
    public void addListener(EventListener<Event> listener) {
        if (listener != null) {
            // Purposely compare listeners by identity, not equality/hash.
            for (EventListener<Event> l : listeners) {
                if (listener == l) {
                    return;
                }
            }
            listeners.add(listener);
        }
    }

    /**
     * Adds event listeners. Event listeners are added by "identity", meaning
     * you can have multiple listeners coexisting even if their test
     * for equality returns <code>true</code>.
     * Adding a <code>null</code> collection has no effect.
     * @param listeners event listeners
     */
    public void addListeners(Collection<EventListener<Event>> listeners) {
        if (listeners != null) {
            for (EventListener<Event> l : listeners) {
                addListener(l);
            }
        }
    }

    /**
     * Adds listeners found in an object graph. Recursively
     * queries the object bean properties for non <code>null</code>
     * objects implementing {@link EventListener}.
     * @param obj the object to retrive listeners from
     */
    public void addListenersFromScan(Object obj) {
        BeanUtil.visitAll(obj, this::addListener, EventListener.class);
    }

    /**
     * Returns an unmodifiable list of event listeners in this event manager.
     * @return list of listeners
     */
    public List<EventListener<Event>> getListeners() {
        return Collections.unmodifiableList(listeners);
    }

    /**
     * Removes a listener instance. Removal is done by "identity", meaning
     * you can have listeners that are not removed despite testing
     * <code>true</code> for equality.
     * Removing a <code>null</code> listener has no effect.
     * @param listener the listener instance to remove
     * @return <code>true</code> if a matching instance was removed
     */
    public boolean removeListener(EventListener<Event> listener) {
        if (listener == null) {
            return false;
        }
        return listeners.removeIf(l -> l == listener);
    }

    /**
     * Removes listener instances. Removal is done by "identity", meaning
     * you can have listeners that are not removed despite testing
     * <code>true</code> for equality.
     * Removing a <code>null</code> collection has no effect.
     * @param listeners listener instances to remove
     * @return <code>true</code> if one or more listeners were removed
     */
    public boolean removeListeners(
            Collection<EventListener<Event>> listeners) {
        if (listeners == null) {
            return false;
        }
        boolean anyRemoved = false;
        for (EventListener<Event> l : listeners) {
            if (removeListener(l)) {
                anyRemoved = true;
            }
        }
        return anyRemoved;
    }

    /**
     * Clears all previously added listeners.  Does not affect listeners
     * associated with a parent event manager.
     */
    public void clearListeners() {
        listeners.clear();
    }

    /**
     * Gets the number of event listeners registered with this event manager.
     * Listeners attached to a parent are not counted.
     * @return listener count
     */
    public int getListenerCount() {
        return listeners.size();
    }

    public void fire(Event event) {
        fire(event, null);
    }

    public void fire(Event event, Level level) {
        Objects.requireNonNull(event, "Cannot fire a null event.");
        log(event, level);
        doFire(event);
    }

    public void bindParent(EventManager parentEventManager) {
        Objects.requireNonNull(
                parentEventManager, "'parentEventManager' cannot be null.");
        if (this.parentEventManager != null
                && this.parentEventManager != parentEventManager) {
            throw new IllegalStateException(
                    "This event manager is already bound to a parent.");
        }
        this.parentEventManager = parentEventManager;
    }

    private void doFire(Event event) {
        for (EventListener<Event> listener : listeners) {
            Method method = MethodUtils.getMatchingAccessibleMethod(
                    listener.getClass(), "accept", event.getClass());
            if (method != null) {
                listener.accept(event);
            } else {
                LOG.trace("Listener {} not accepting event \"{}\".",
                        listener.getClass().getSimpleName(), event.getName());
            }
        }
        if (parentEventManager != null) {
            parentEventManager.doFire(event);
        }
    }

    public void log(Event event, Level level) {
        //FYI, depending on logger pattern, it is possible only the last part
        //of the logger name is shown (after the dot)
        Logger log = LoggerFactory.getLogger(event.getClass().getSimpleName()
                + "." + event.getName());
        Level safeLevel = ObjectUtils.defaultIfNull(level, Level.INFO);
        if (stacktraceLoggingDisabled && event.getException() != null) {
            Slf4jUtil.log(log, safeLevel, event.toString()
                    + " Cause:\n{}",
                    ExceptionUtil.getFormattedMessages(event.getException()));
        } else {
            Slf4jUtil.log(
                    log, safeLevel, event.toString(), event.getException());
        }
    }
}
