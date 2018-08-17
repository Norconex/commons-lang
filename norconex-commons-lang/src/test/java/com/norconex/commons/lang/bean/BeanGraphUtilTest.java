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
package com.norconex.commons.lang.bean;

import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.event.Event;
import com.norconex.commons.lang.event.IEventListener;


public class BeanGraphUtilTest {

    @Test
    public void testGraphIterator() {
        Root root = new Root();

        Assert.assertEquals(3, BeanGraphUtil.getChildren(root).size());
        Assert.assertEquals(3,
                BeanGraphUtil.find(root, IEventListener.class).size());

//        for (Object obj : BeanGraphUtil.getChildren(root)) {
//            System.out.println("Root child: " + obj);
//        }
//        for (Object obj : BeanGraphUtil.find(root, IEventListener.class)) {
//            System.out.println("Event listener: " + obj);
//        }
    }

    public static class Root {
        private Sub1Yes sub1yes = new Sub1Yes();
        private Sub2No sub2no = new Sub2No();
        private Sub3Yes sub3yes = new Sub3Yes(sub1yes);
        public Sub1Yes getSub1yes() {
            return sub1yes;
        }
        public void setSub1yes(Sub1Yes sub1yes) {
            this.sub1yes = sub1yes;
        }
        public Sub2No getSub2no() {
            return sub2no;
        }
        public void setSub2no(Sub2No sub2no) {
            this.sub2no = sub2no;
        }
        public Sub3Yes getSub3yes() {
            return sub3yes;
        }
        public void setSub3yes(Sub3Yes sub3yes) {
            this.sub3yes = sub3yes;
        }
    }

    public static class Sub1Yes implements IEventListener<Event<?>> {
        @Override
        public void accept(Event<?> t) {
        }
    }
    public static class Sub2No {
    }
    public static class Sub3Yes implements IEventListener<Event<?>> {
        private final Sub1Yes sub1Yes;
        private final Sub3_1Yes sub3_1Yes = new Sub3_1Yes();
        public Sub3Yes(Sub1Yes sub1Yes) {
            super();
            this.sub1Yes = sub1Yes;
        }
        public Sub1Yes getSub1Yes() {
            return sub1Yes;
        }
        public Sub3_1Yes getSub3_1Yes() {
            return sub3_1Yes;
        }
        @Override
        public void accept(Event<?> t) {
        }
    }

    public static class Sub3_1Yes implements IEventListener<Event<?>> {
        @Override
        public void accept(Event<?> t) {
        }
    }

}
