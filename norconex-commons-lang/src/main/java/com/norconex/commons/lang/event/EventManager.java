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
import com.norconex.commons.lang.bean.BeanGraphUtil;

/**
 * Manages event listeners and logs events.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class EventManager {

    //TODO:
    //fireEvent(event) // default is trace?
    //fireEvent(event, loglevel)
    // have a static getInstance method for "global" use

    private final List<IEventListener<Event<?>>> listeners = new ArrayList<>();

//    private static final int ID_PRINT_WIDTH = 25;

//    public EventManager(
//            ICrawler crawler, List<ICrawlerEventListener> listeners) {
//        this.crawler = crawler;
//        if (listeners != null) {
//            this.listeners.addAll(listeners);
//        }
//    }

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
        SLF4JUtil.log(log, safeLevel, Objects.toString(event.getSource(), ""));
//        if (log.isInfoEnabled()) {
//            log.info("TODO: " + event.getSource());
////            log.info(getLogMessage(event, log.isDebugEnabled()));
//        }
    }

    public void addListenersFromScan(Object obj) {
        BeanGraphUtil.visit(obj, this::addListener, IEventListener.class);
    }

//    private String getLogMessage(Event<?> event, boolean includeSubject) {
//        StringBuilder b = new StringBuilder();
//        b.append(StringUtils.leftPad(event.getName(), ID_PRINT_WIDTH));
//        if (event.getSource() != null) {
//            b.append(": ");
//            b.append(event.getSource());
//        }
//        if (includeSubject) {
//            b.append(" (");
//            b.append(Objects.toString(event.getSubject(),
//                    "No additional information available."));
//            b.append(")");
//        }
//        return b.toString();
//    }

}
