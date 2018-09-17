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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.apache.commons.lang3.ObjectUtils;
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
public class EventManager {

    //TODO:
    // have a static getInstance method for "global" use?

    private final List<IEventListener<Event<?>>> listeners = new ArrayList<>();

    public void addListener(IEventListener<Event<?>> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }
    public void addListeners(List<IEventListener<Event<?>>> listeners) {
        if (listeners != null) {
            this.listeners.addAll(listeners);
        }
    }
    public List<IEventListener<Event<?>>> getListeners() {
        return Collections.unmodifiableList(listeners);
    }
    public boolean removeListener(IEventListener<Event<?>> listener) {
        return listeners.remove(listener);
    }
    public void clearListeners() {
        listeners.clear();
    }
    public int getListenerCount() {
        return listeners.size();
    }

    //TODO remove  "Listener" word from methods
    //TODO scan(String package);


    public void fire(Event<?> event) {
        fire(event, null);
    }

    public void fire(Event<?> event, Level level) {
        Objects.requireNonNull(event, "Cannot fire a null event.");
        log(event, level);
        for (IEventListener<Event<?>> listener : listeners) {
            listener.accept(event);
        }
    }

    public void log(Event<?> event, Level level) {
        Logger log =  LoggerFactory.getLogger(event.getClass().getSimpleName()
                + "." + event.getName());
        Level safeLevel = ObjectUtils.defaultIfNull(level, Level.INFO);
        SLF4JUtil.log(log, safeLevel, event.toString(), event.getException());// Objects.toString(event.getSource(), ""));
    }

    public void addListenersFromScan(Object obj) {
        BeanUtil.visitAll(obj, this::addListener, IEventListener.class);
    }
}
