/* Copyright 2018 Norconex Inc.
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
import java.util.Objects;
import java.util.Set;

import org.apache.commons.collections4.set.ListOrderedSet;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import com.norconex.commons.lang.SLF4JUtil;
import com.norconex.commons.lang.bean.BeanUtil;

/**
 * Manages event listeners and logs events.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
//TODO: have a static getInstance method for "global" use?
public class EventManager {

    private static final Logger LOG =
            LoggerFactory.getLogger(EventManager.class);

    private final EventManager parentEventManager;

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

    private final Set<IEventListener<Event<?>>> listeners =
            new ListOrderedSet<>();

    public void addListener(IEventListener<Event<?>> listener) {
        if (listener != null) {
            this.listeners.add(listener);
        }
    }
    public void addListeners(Collection<IEventListener<Event<?>>> listeners) {
        if (listeners != null) {
            this.listeners.addAll(listeners);
        }
    }

    public void addListenersFromScan(Object obj) {
        BeanUtil.visitAll(obj, this::addListener, IEventListener.class);
    }

    public Set<IEventListener<Event<?>>> getListeners() {
        return Collections.unmodifiableSet(this.listeners);
    }
    public boolean removeListener(IEventListener<Event<?>> listener) {
        return this.listeners.remove(listener);
    }
    public boolean removeListeners(
            Collection<IEventListener<Event<?>>> listeners) {
        return this.listeners.removeAll(listeners);
    }
    public void clearListeners() {
        this.listeners.clear();
    }
    public int getListenerCount() {
        return this.listeners.size();
    }

    //TODO scan(String package);


    public void fire(Event<?> event) {
        fire(event, null);
    }

    public void fire(Event<?> event, Level level) {
        Objects.requireNonNull(event, "Cannot fire a null event.");
        log(event, level);
        doFire(event);
    }

    private void doFire(Event<?> event) {
        for (IEventListener<Event<?>> listener : listeners) {
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

    public void log(Event<?> event, Level level) {
        Logger log =  LoggerFactory.getLogger(event.getClass().getSimpleName()
                + "." + event.getName());
        Level safeLevel = ObjectUtils.defaultIfNull(level, Level.INFO);
        SLF4JUtil.log(log, safeLevel, event.toString(), event.getException());// Objects.toString(event.getSource(), ""));
    }
}
