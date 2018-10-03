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

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.mutable.MutableInt;
import org.junit.Assert;
import org.junit.Test;

import com.norconex.commons.lang.event.Event;
import com.norconex.commons.lang.event.IEventListener;
import com.norconex.commons.lang.map.Properties;


/**
 * Bean utility methods.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class BeanUtilTest {

    @Test
    public void testToMap() {
        Map<String, Object> map = BeanUtil.toMap(new Bean());
        Assert.assertEquals("potato", map.get("string"));
        Assert.assertEquals(123, map.get("primitiveInt"));
        Assert.assertEquals(Integer.valueOf(456), map.get("objectInteger"));
        Assert.assertEquals(new Event<>("testEvent", this), map.get("event"));
        Assert.assertEquals(Arrays.asList(0.5d, 1.0d), map.get("doubles"));
        Assert.assertEquals(5, map.size());
    }

    @Test
    public void testToProperties() {
        Properties props = BeanUtil.toProperties(new Bean(), "event");
        Assert.assertEquals("potato", props.getString("string"));
        Assert.assertEquals("123", props.getString("primitiveInt"));
        Assert.assertEquals("456", props.getString("objectInteger"));
        Assert.assertEquals(
                Arrays.asList("0.5", "1.0"), props.getStrings("doubles"));
        Assert.assertEquals(4, props.size());
    }

    @Test
    public void testGetValue() {
        Bean bean = new Bean();
        Assert.assertEquals("potato", BeanUtil.getValue(bean, "string"));
        Assert.assertEquals(123, (int) BeanUtil.getValue(bean, "primitiveInt"));
        Assert.assertEquals(Integer.valueOf(456),
                BeanUtil.getValue(bean, "objectInteger"));
        Assert.assertEquals(new Event<>("testEvent", this),
                BeanUtil.getValue(bean, "event"));
    }

    @Test
    public void testSetSubValue() {
        SubBean bean = new SubBean();
        // set value on super
        BeanUtil.setValue(bean, "string", "potato");
        // set overridden value
        BeanUtil.setValue(bean, "objectInteger", Integer.valueOf(999));
        // set sub value
        BeanUtil.setValue(bean, "date", new Date(946684800000L));
        // set readonly
        try {
            BeanUtil.setValue(bean, "readOnly", "should fail");
            Assert.fail("Setting a readonly property should fail.");
        } catch (BeanException e) {
            //NOOP
        }
        // set writeonly
        BeanUtil.setValue(bean, "writeOnly", "should be OK");

        assertEquals("potato", bean.getString());
        assertEquals(Integer.valueOf(999), bean.getObjectInteger());
        assertEquals(new Date(946684800000L), bean.getDate());
    }

    @Test
    public void testSetValue() {
        Bean bean = new Bean();
        BeanUtil.setValue(bean, "string", "carrot");
        BeanUtil.setValue(bean, "primitiveInt", 777);
        BeanUtil.setValue(bean, "objectInteger", Integer.valueOf(888));
        BeanUtil.setValue(bean, "event", new Event<>("blah", "x"));

        Assert.assertEquals("carrot", bean.getString());
        Assert.assertEquals(777, bean.getPrimitiveInt());
        Assert.assertEquals(Integer.valueOf(888), bean.getObjectInteger());
        Assert.assertEquals(new Event<>("blah", "x"), bean.getEvent());
    }

    @Test
    public void testGetGenericType() {
        Assert.assertEquals(Double.class,
                BeanUtil.getPropertyGenericType(Bean.class, "doubles"));
    }

    @Test
    public void testVisit() {
        Root root = new Root();

        Assert.assertEquals(3, BeanUtil.getChildren(root).size());
        Assert.assertEquals(3,
                BeanUtil.find(root, IEventListener.class).size());

//        for (Object obj : BeanGraphUtil.getChildren(root)) {
//            System.out.println("Root child: " + obj);
//        }
//        for (Object obj : BeanGraphUtil.find(root, IEventListener.class)) {
//            System.out.println("Event listener: " + obj);
//        }
    }

    @Test
    public void testVisitProperties() {
        Root root = new Root();

        MutableInt cnt = new MutableInt();
        BeanUtil.visitAllProperties(root, (obj, pd) -> {
//            System.out.println("Property: " + obj.getClass().getSimpleName()
//                    + " -> " + pd.getName());
            cnt.increment();
        });
        Assert.assertEquals(5, cnt.intValue());

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
        private Sub1Yes sub1Yes;
        private Sub3_1Yes sub3_1Yes = new Sub3_1Yes();
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
        public void setSub1Yes(Sub1Yes sub1Yes) {
            this.sub1Yes = sub1Yes;
        }
        public void setSub3_1Yes(Sub3_1Yes sub3_1Yes) {
            this.sub3_1Yes = sub3_1Yes;
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

    public static class Bean {
        private String string = "potato";
        private int primitiveInt = 123;
        private Integer objectInteger = Integer.valueOf(456);
        private Event<Bean> event = new Event<>("testEvent", this);
        private List<Double> doubles = Arrays.asList(0.5d, 1.0d);
        public String getString() {
            return string;
        }
        public void setString(String string) {
            this.string = string;
        }
        public int getPrimitiveInt() {
            return primitiveInt;
        }
        public void setPrimitiveInt(int primitiveInt) {
            this.primitiveInt = primitiveInt;
        }
        public Integer getObjectInteger() {
            return objectInteger;
        }
        public void setObjectInteger(Integer objectInteger) {
            this.objectInteger = objectInteger;
        }
        public Event<Bean> getEvent() {
            return event;
        }
        public void setEvent(Event<Bean> event) {
            this.event = event;
        }
        public List<Double> getDoubles() {
            return doubles;
        }
        public void setDoubles(List<Double> doubles) {
            this.doubles = doubles;
        }
    }

    public static class SubBean extends Bean {
        private Date date;
        private String readOnly;
        private String writeOnly;
        public Date getDate() {
            return date;
        }
        public void setDate(Date date) {
            this.date = date;
        }
        public String getReadOnly() {
            return readOnly;
        }
        public void setWriteOnly(String writeOnly) {
            this.writeOnly = writeOnly;
        }
        @Override
        public Integer getObjectInteger() {
            return super.getObjectInteger();
        }
        @Override
        public void setObjectInteger(Integer objectInteger) {
            super.setObjectInteger(objectInteger);
        }
        public String getUnrelated() {
            return writeOnly;
        }
        public void setUnrelated(String str) {
            setString(str);
        }
    }
}
