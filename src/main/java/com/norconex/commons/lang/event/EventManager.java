/* Copyright 2018-2020 Norconex Inc.
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

import com.norconex.commons.lang.SLF4JUtil;
import com.norconex.commons.lang.bean.BeanUtil;

/**
 * Manages event listeners and logs events.  New event managers can be
 * constructed with a "parent" event manager. When chained as such,
 * event on a particular event manager are bubbled up to the parents but
 * never down to children. Events are logged by the manager that first fires
 * the event.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class EventManager {

    private static final Logger LOG =
            LoggerFactory.getLogger(EventManager.class);

    private EventManager parentEventManager;

    private final CopyOnWriteArrayList<IEventListener<Event>> listeners =
            new CopyOnWriteArrayList<>();

    public EventManager() {
        this(null);
    }
    /**
     * Wraps a parent event manager.  Events fired on this event manager
     * will be fired on this instance listeners first, then its parent
     * listeners.
     * Modifying the list of listeners in this instance does not impact
     * listeners of the parrent event manager.
     * @param parentEventManager parent event manager
     */
    public EventManager(EventManager parentEventManager) {
        super();
        this.parentEventManager = parentEventManager;
    }

    public void addListener(IEventListener<Event> listener) {
        if (listener != null) {
            this.listeners.addIfAbsent(listener);
        }
    }
    public void addListeners(Collection<IEventListener<Event>> listeners) {
        if (listeners != null) {
            this.listeners.addAllAbsent(listeners);
        }
    }

    public void addListenersFromScan(Object obj) {
        BeanUtil.visitAll(obj, this::addListener, IEventListener.class);
    }

    public List<IEventListener<Event>> getListeners() {
        return Collections.unmodifiableList(this.listeners);
    }
    public boolean removeListener(IEventListener<Event> listener) {
        return this.listeners.remove(listener);
    }
    public boolean removeListeners(
            Collection<IEventListener<Event>> listeners) {
        return this.listeners.removeAll(listeners);
    }
    public void clearListeners() {
        this.listeners.clear();
    }
    public int getListenerCount() {
        return this.listeners.size();
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
        for (IEventListener<Event> listener : listeners) {
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
        //FYI, depending on log4j2 pattern, it is possible only the last part
        //of the logger name is shown (after the dot)
        Logger log =  LoggerFactory.getLogger(event.getClass().getSimpleName()
                + "." + event.getName());
        Level safeLevel = ObjectUtils.defaultIfNull(level, Level.INFO);
        SLF4JUtil.log(log, safeLevel, event.toString(), event.getException());
    }
}
