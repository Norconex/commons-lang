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
package com.norconex.commons.lang.bean;

import static com.norconex.commons.lang.bean.BeanUtil.getReadMethod;
import static com.norconex.commons.lang.bean.BeanUtil.getWriteMethod;
import static com.norconex.commons.lang.bean.BeanUtil.isReadable;
import static com.norconex.commons.lang.bean.BeanUtil.isWritable;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.ibm.icu.math.BigDecimal;
import com.norconex.commons.lang.bean.BeanUtilTest.MiscAccessorsBean.Fields;
import com.norconex.commons.lang.event.Event;
import com.norconex.commons.lang.event.IEventListener;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import lombok.experimental.FieldNameConstants;

class BeanUtilTest {

    @Test
    void testGetPropertyDescriptorsObject() {
        assertThat(BeanUtil.getPropertyDescriptors(new Bean())
                .stream()
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                    "string",
                    "primitiveInt",
                    "objectInteger",
                    "event",
                    "doubles");
        assertThat(BeanUtil.getPropertyDescriptors((Object) null)).isEmpty();
        // Test no empty args contructor
        assertThat(BeanUtil.getPropertyDescriptors(
                new BigDecimal(0))).isEmpty();
    }
    @Test
    void testGetPropertyDescriptorsClass() {
        assertThat(BeanUtil.getPropertyDescriptors(Bean.class)
                .stream()
                .map(PropertyDescriptor::getName)
                .collect(Collectors.toList()))
            .containsExactlyInAnyOrder(
                    "string",
                    "primitiveInt",
                    "objectInteger",
                    "event",
                    "doubles");
        assertThat(BeanUtil.getPropertyDescriptors((Class<?>) null)).isEmpty();
        // Test no empty args contructor
        assertThat(BeanUtil.getPropertyDescriptors(
                new BigDecimal(0))).isEmpty();
    }

    @Test
    void testCopyPropertiesOverNulls() {
        //--- Base Test ---
        var target = new SubBean();
        target.setObjectInteger(null);

        var source = new SubBean();
        // These two should not be copied because they do not have a setter
        // AND a getter.
        source.readOnly = "read";
        source.writeOnly = "write";
        source.setDate(null);
        source.setPrimitiveInt(678);

        var expected = new SubBean();
        expected.setDate(null);
        expected.setObjectInteger(456);

        // because accessors for "unrelated" are intentionally
        // messing up "string", we exclude it from comparison.
        BeanUtil.copyPropertiesOverNulls(target, source);
        assertThat(target)
            .usingRecursiveComparison()
            .ignoringFields("string")
            .isEqualTo(expected);

        //--- Test when different types ---
        // Expected to be unchanged
        BeanUtil.copyPropertiesOverNulls(target, "Not same type");
        assertThat(target)
            .usingRecursiveComparison()
            .ignoringFields("string")
            .isEqualTo(expected);

        //--- Test when source is null ---
        // Expected to be unchanged
        BeanUtil.copyPropertiesOverNulls(target, null);
        assertThat(target)
            .usingRecursiveComparison()
            .ignoringFields("string")
            .isEqualTo(expected);
    }

    @Test
    void testCopyProperties() {
        //--- Base Test ---
        var target = new SubBean();
        target.setObjectInteger(null);

        var source = new SubBean();
        // These two should not be copied because they do not have a setter
        // AND a getter.
        source.readOnly = "read";
        source.writeOnly = "write";
        source.setDate(null);
        source.setPrimitiveInt(678);

        var expected = new SubBean();
        expected.setDate(null);
        expected.setObjectInteger(456);
        expected.setPrimitiveInt(678);

        // because accessors for "unrelated" are intentionally
        // messing up "string", we exclude it from comparison.
        BeanUtil.copyProperties(target, source);
        assertThat(target)
            .usingRecursiveComparison()
            .ignoringFields("string")
            .isEqualTo(expected);

        //--- Test when different types ---
        // Expected to be unchanged
        BeanUtil.copyProperties(target, "Not same type");
        assertThat(target)
            .usingRecursiveComparison()
            .ignoringFields("string")
            .isEqualTo(expected);

        //--- Test when source is null ---
        // Expected to be unchanged
        BeanUtil.copyProperties(target, null);
        assertThat(target)
            .usingRecursiveComparison()
            .ignoringFields("string")
            .isEqualTo(expected);
    }

    @Test
    void testClone() {
        var bean = new SubBean();
        bean.writeOnly = "write";
        bean.readOnly = "read";

        var actualBean = BeanUtil.clone(bean);

        // because accessors for "unrelated" are intentionally
        // messing up "string", we exclude it from comparison.
        // we also exclude write/readOnly as they are not compliant accessors.
        assertThat(actualBean)
            .usingRecursiveComparison()
            .ignoringFields("string", "writeOnly", "readOnly")
            .isEqualTo(bean);
        assertThat(actualBean.readOnly).isNull();
        assertThat(actualBean.writeOnly).isNull();

        assertThat((SubBean) BeanUtil.clone(null)).isNull();
        assertThat(BeanUtil.clone("nope")).isEqualTo("nope");
        assertThat(BeanUtil.clone(123)).isEqualTo(123);
    }


    @Test
    void testDiff() {
        var bean1 = new Bean();
        var bean2 = new Bean();
        bean2.setPrimitiveInt(321);
        bean2.setObjectInteger(654);

        var expected =
                "< Bean.objectInteger = 456\n"
              + "> Bean.objectInteger = 654\n"
              + "< Bean.primitiveInt = 123\n"
              + "> Bean.primitiveInt = 321";
        assertThat(BeanUtil.diff(bean1, bean2)).isEqualTo(expected);
    }

    @Test
    void testGetChildren() {
        assertThat(BeanUtil.getChildren(new Root())).hasSize(3);
        assertThat(BeanUtil.getChildren((Class<?>) null)).isEmpty();
    }

    @Test
    void testHasChildren() {
        assertThat(BeanUtil.hasChildren(new Root())).isTrue();
        assertThat(BeanUtil.hasChildren(null)).isFalse();
        assertThat(BeanUtil.hasChildren(new Sub3_1Yes())).isFalse();
    }

    @Test
    void testGetPropertyType() {
        var bean = new Bean();
        assertThat(BeanUtil.getPropertyType(bean, "string"))
                .isEqualTo(String.class);
        assertThat(BeanUtil.getPropertyType(bean, "primitiveInt"))
                .isEqualTo(Integer.TYPE);
        assertThat(BeanUtil.getPropertyType(bean, "objectInteger"))
                .isEqualTo(Integer.class);
        assertThat(BeanUtil.getPropertyType(bean, "event"))
                .isEqualTo(Event.class);
        assertThat(BeanUtil.getPropertyType(bean, "doubles"))
                .isEqualTo(List.class);

        assertThat(BeanUtil.getPropertyType(null, "doubles")).isNull();
        assertThat(BeanUtil.getPropertyType(bean, null)).isNull();
        assertThat(BeanUtil.getPropertyType(bean, "badProperty")).isNull();
    }

    @Test
    void testGetPropertyGenericType() {
        assertThat(BeanUtil.getPropertyGenericType(Bean.class, "doubles"))
            .isEqualTo(Double.class);
        assertThat(BeanUtil.getPropertyGenericType(null, "doubles")).isNull();
        assertThat(BeanUtil.getPropertyGenericType(Bean.class, null)).isNull();
        assertThatExceptionOfType(BeanException.class).isThrownBy(() ->
            BeanUtil.getPropertyGenericType(Bean.class, "badProperty"));
    }

    @Test
    void testGetReadMethod() {
        var b = new MiscAccessorsBean();
        assertThat(getReadMethod(b, Fields.normal)).isNotNull();
        assertThat(getReadMethod(b, Fields.normalSetterNoGetter)).isNull();
        assertThat(getReadMethod(b, Fields.normalGetterNoSetter)).isNotNull();
        assertThat(getReadMethod(b, Fields.fluent)).isNotNull();
        assertThat(getReadMethod(b, Fields.fluentSetterNoGetter)).isNull();
        assertThat(getReadMethod(b, Fields.boolNormal)).isNotNull();
        assertThat(getReadMethod(b, Fields.boolSetterNoGetter)).isNull();
        assertThat(getReadMethod(b, Fields.boolGetterNoSetter)).isNotNull();
        assertThat(getReadMethod(b, Fields.boolFluent)).isNotNull();
        assertThat(getReadMethod(b, Fields.boolFluentSetterNoGetter)).isNull();
        assertThat(getReadMethod(b, Fields.compactNormal)).isNotNull();
        assertThat(getReadMethod(b, Fields.compactSetterNoGetter)).isNull();
        assertThat(getReadMethod(b, Fields.compactGetterNoSetter)).isNotNull();
        assertThat(getReadMethod(b, Fields.compactFluent)).isNotNull();
        assertThat(getReadMethod(
                b, Fields.compactFluentSetterNoGetter)).isNull();
        assertThat(getReadMethod((Object) null, Fields.compactFluent)).isNull();
        assertThat(getReadMethod(
                (Class<?>) null, Fields.compactFluent)).isNull();
    }

    @Test
    void testGetWriteMethodObjectString() {
        var b = new MiscAccessorsBean();
        assertThat(getWriteMethod(b, Fields.normal)).isNotNull();
        assertThat(getWriteMethod(b, Fields.normalSetterNoGetter)).isNotNull();
        assertThat(getWriteMethod(b, Fields.normalGetterNoSetter)).isNull();
        assertThat(getWriteMethod(b, Fields.fluent)).isNotNull();
        assertThat(getWriteMethod(b, Fields.fluentSetterNoGetter)).isNotNull();
        assertThat(getWriteMethod(b, Fields.boolNormal)).isNotNull();
        assertThat(getWriteMethod(b, Fields.boolSetterNoGetter)).isNotNull();
        assertThat(getWriteMethod(b, Fields.boolGetterNoSetter)).isNull();
        assertThat(getWriteMethod(b, Fields.boolFluent)).isNotNull();
        assertThat(getWriteMethod(
                b, Fields.boolFluentSetterNoGetter)).isNotNull();
        assertThat(getWriteMethod(b, Fields.compactNormal)).isNotNull();
        assertThat(getWriteMethod(b, Fields.compactSetterNoGetter)).isNotNull();
        assertThat(getWriteMethod(b, Fields.compactGetterNoSetter)).isNull();
        assertThat(getWriteMethod(b, Fields.compactFluent)).isNotNull();
        assertThat(getWriteMethod(
                b, Fields.compactFluentSetterNoGetter)).isNotNull();
        assertThat(getWriteMethod(
                (Object) null, Fields.compactFluent)).isNull();
        assertThat(getWriteMethod(
                (Class<?>) null, Fields.compactFluent)).isNull();
    }

    @Test
    void testGetValueObjectString() {
        var bean = new Bean();
        assertThat((String) BeanUtil.getValue(
                bean, "string")).isEqualTo("potato");
        assertThat((int) BeanUtil.getValue(
                bean, "primitiveInt")).isEqualTo(123);
        assertThat((Integer) BeanUtil.getValue(
                bean, "objectInteger")).isEqualTo(Integer.valueOf(456));
        assertThat((Event) BeanUtil.getValue(bean, "event"))
                .isEqualTo(Event.builder("testEvent", "test").build());

        assertThat((List<?>) BeanUtil.getValue(null, "doubles")).isNull();
        assertThat((String) BeanUtil.getValue(
                Bean.class, (String) null)).isNull();
        assertThat((String) BeanUtil.getValue(
                Bean.class, "badProperty")).isNull();
    }

    @Test
    void testGetValueObjectPropertyDescriptor() throws IntrospectionException {
        var pd = new PropertyDescriptor("string", Bean.class);
        var bean = new Bean();
        assertThat((String) BeanUtil.getValue(bean, pd)).isEqualTo("potato");

        assertThat((String) BeanUtil.getValue(null, pd)).isNull();
        assertThat((String) BeanUtil.getValue(
                Bean.class, (PropertyDescriptor) null)).isNull();
    }

    @Test
    void testIsReadable() {
        var b = new MiscAccessorsBean();
        assertThat(isReadable(b, Fields.normal)).isTrue();
        assertThat(isReadable(b, Fields.normalSetterNoGetter)).isFalse();
        assertThat(isReadable(b, Fields.normalGetterNoSetter)).isTrue();
        assertThat(isReadable(b, Fields.fluent)).isTrue();
        assertThat(isReadable(b, Fields.fluentSetterNoGetter)).isFalse();
        assertThat(isReadable(b, Fields.boolNormal)).isTrue();
        assertThat(isReadable(b, Fields.boolSetterNoGetter)).isFalse();
        assertThat(isReadable(b, Fields.boolGetterNoSetter)).isTrue();
        assertThat(isReadable(b, Fields.boolFluent)).isTrue();
        assertThat(isReadable(b, Fields.boolFluentSetterNoGetter)).isFalse();
        assertThat(isReadable(b, Fields.compactNormal)).isTrue();
        assertThat(isReadable(b, Fields.compactSetterNoGetter)).isFalse();
        assertThat(isReadable(b, Fields.compactGetterNoSetter)).isTrue();
        assertThat(isReadable(b, Fields.compactFluent)).isTrue();
        assertThat(isReadable(b, Fields.compactFluentSetterNoGetter)).isFalse();

        assertThat(isReadable(null, Fields.normal)).isFalse();
        assertThat(isReadable((Object) null, Fields.normal)).isFalse();
        assertThat(isReadable((Class<?>) null, Fields.normal)).isFalse();
        assertThat(isReadable(b, "badProperty")).isFalse();
    }

    @Test
    void testSetValue() {
        // Direct values

        var bean = new Bean();
        BeanUtil.setValue(bean, "string", "carrot");
        BeanUtil.setValue(bean, "primitiveInt", 777);
        BeanUtil.setValue(bean, "objectInteger", Integer.valueOf(888));
        BeanUtil.setValue(bean, "event", Event.builder("blah", "x").build());

        Assertions.assertEquals("carrot", bean.getString());
        Assertions.assertEquals(777, bean.getPrimitiveInt());
        Assertions.assertEquals(Integer.valueOf(888), bean.getObjectInteger());
        Assertions.assertEquals(
                Event.builder("blah", "x").build(), bean.getEvent());

        // Nested values

        var subBean = new SubBean();
        // set value on super
        BeanUtil.setValue(subBean, "string", "potato");
        // set overridden value
        BeanUtil.setValue(subBean, "objectInteger", Integer.valueOf(999));
        // set sub value
        BeanUtil.setValue(subBean, "date", new Date(946684800000L));
        // set readonly
        assertThatNoException().isThrownBy(() -> BeanUtil.setValue(
                subBean, "readOnly", "should fail silently"));

        // set writeonly
        BeanUtil.setValue(subBean, "writeOnly", "should be OK");

        assertEquals("potato", subBean.getString());
        assertEquals(Integer.valueOf(999), subBean.getObjectInteger());
        assertEquals(new Date(946684800000L), subBean.getDate());
    }

    @Test
    void testIsWritable() {
        var b = new MiscAccessorsBean();
        assertThat(isWritable(b, Fields.normal)).isTrue();
        assertThat(isWritable(b, Fields.normalSetterNoGetter)).isTrue();
        assertThat(isWritable(b, Fields.normalGetterNoSetter)).isFalse();
        assertThat(isWritable(b, Fields.fluent)).isTrue();
        assertThat(isWritable(b, Fields.fluentSetterNoGetter)).isTrue();
        assertThat(isWritable(b, Fields.boolNormal)).isTrue();
        assertThat(isWritable(b, Fields.boolSetterNoGetter)).isTrue();
        assertThat(isWritable(b, Fields.boolGetterNoSetter)).isFalse();
        assertThat(isWritable(b, Fields.boolFluent)).isTrue();
        assertThat(isWritable(b, Fields.boolFluentSetterNoGetter)).isTrue();
        assertThat(isWritable(b, Fields.compactNormal)).isTrue();
        assertThat(isWritable(b, Fields.compactSetterNoGetter)).isTrue();
        assertThat(isWritable(b, Fields.compactGetterNoSetter)).isFalse();
        assertThat(isWritable(b, Fields.compactFluent)).isTrue();
        assertThat(isWritable(b, Fields.compactFluentSetterNoGetter)).isTrue();

        assertThat(isWritable(null, Fields.normal)).isFalse();
        assertThat(isWritable((Object) null, Fields.normal)).isFalse();
        assertThat(isWritable((Class<?>) null, Fields.normal)).isFalse();
        assertThat(isWritable(b, "badProperty")).isFalse();
    }

    @Test
    void testToMap() {
        var map = BeanUtil.toMap(new Bean());
        Assertions.assertEquals("potato", map.get("string"));
        Assertions.assertEquals(123, map.get("primitiveInt"));
        Assertions.assertEquals(Integer.valueOf(456), map.get("objectInteger"));
        Assertions.assertEquals(Event.builder(
                "testEvent", "test").build(), map.get("event"));
        Assertions.assertEquals(Arrays.asList(0.5d, 1.0d), map.get("doubles"));
        Assertions.assertEquals(5, map.size());

        assertThat(BeanUtil.toMap(null)).isEmpty();
    }

    @Test
    void testToProperties() {
        var props = BeanUtil.toProperties(new Bean(), "event");
        Assertions.assertEquals("potato", props.getString("string"));
        Assertions.assertEquals("123", props.getString("primitiveInt"));
        Assertions.assertEquals("456", props.getString("objectInteger"));
        Assertions.assertEquals(
                Arrays.asList("0.5", "1.0"), props.getStrings("doubles"));
        Assertions.assertEquals(4, props.size());

        assertThat(BeanUtil.toProperties(null)).isEmpty();
    }

    @Test
    void testFind() {
        assertThat(BeanUtil.find(new Root(), IEventListener.class)).hasSize(3);
    }

    @Test
    void testVisitAllObjectConsumer() {
        List<String> names = new ArrayList<>();
        BeanUtil.visitAll(
                new Root(),
                o -> names.add(o.getClass().getSimpleName()));
        assertThat(names).containsExactly(
                "Root", "Sub1Yes", "Sub2No", "Sub3Yes", "Sub3_1Yes");


        // collections/maps
        Set<String> nameSet = new TreeSet<>();
        BeanUtil.visitAll(
                new CollectionsHolder(),
                o -> nameSet.add(o.getClass().getSimpleName()));
        assertThat(nameSet).containsExactly(
                "ArrayList",
                "CollectionEntry",
                "CollectionsHolder",
                "HashMap",
                "HashSet",
                "String"
        );
    }

    @Test
    void testVisitAllObjectConsumerClass() {
        List<String> names = new ArrayList<>();
        BeanUtil.visitAll(
                new Root(),
                o -> names.add(o.getClass().getSimpleName()),
                IEventListener.class);
        assertThat(names).containsExactly("Sub1Yes", "Sub3Yes", "Sub3_1Yes");
    }

    @Test
    void testVisitObjectPredicate() {
        List<String> names = new ArrayList<>();
        BeanUtil.visit(
                new Root(),
                o -> {
                    names.add(o.getClass().getSimpleName());
                    return !o.getClass().getName().endsWith("No");
                });
        assertThat(names).containsExactly("Root", "Sub1Yes", "Sub2No");
    }

    @Test
    void testVisitObjectPredicateClass() {
        List<String> names = new ArrayList<>();
        BeanUtil.visit(
                new Root(),
                o -> {
                    names.add(o.getClass().getSimpleName());
                    return !"Sub3Yes".equals(o.getClass().getSimpleName());
                },
                IEventListener.class);
        assertThat(names).containsExactly("Sub1Yes", "Sub3Yes");
    }

    @Test
    void testVisitAllPropertiesObjectBiConsumer() {
        List<String> names = new ArrayList<>();
        BeanUtil.visitAllProperties(
                new Root(),
                (o, pd) -> names.add(pd.getName()));
        assertThat(names).containsExactly(
                "sub1yes", "sub2no", "sub3yes", "sub1Yes", "sub3_1Yes");
    }

    @Test
    void testVisitAllPropertiesObjectBiConsumerClass() {
        List<String> names = new ArrayList<>();
        BeanUtil.visitAllProperties(
                new Root(),
                (o, pd) -> names.add(pd.getName()),
                IEventListener.class);
        // Only Sub3Yes qualifies (IEventListener) AND has any properties to
        // return, so returned property can only be from that class.
        assertThat(names).containsExactly("sub1Yes", "sub3_1Yes");
    }

    @Test
    void testVisitPropertiesObjectBiPredicate() {
        List<String> names = new ArrayList<>();
        BeanUtil.visitProperties(
                new Root(),
                (o, pd) -> {
                    names.add(pd.getName());
                    return !"sub3yes".equals(pd.getName());
                });
        assertThat(names).containsExactly("sub1yes", "sub2no", "sub3yes");

        // collections/maps
        List<String> propList = new ArrayList<>();
        BeanUtil.visitProperties(
                new CollectionsHolder(),
                (o, pd) -> {
                    propList.add(pd.getName());
                    return !"entryName".equals(pd.getName());
                });
        assertThat(propList).containsExactly(
                "multiValueMap", "entries", "entryName"
        );
    }

    @Test
    void testVisitPropertiesObjectBiPredicateClass() {
        // Only Sub3Yes qualifies (IEventListener) AND has any properties to
        // return, so returned property can only be from that class.
        List<String> names = new ArrayList<>();
        BeanUtil.visitProperties(
                new Root(),
                (o, pd) -> {
                    names.add(pd.getName());
                    // "sub3yes" should never be encountered, so it continues..
                    return !"sub3yes".equals(pd.getName());
                },
                IEventListener.class);
        assertThat(names).containsExactly("sub1Yes", "sub3_1Yes");


        names.clear();
        BeanUtil.visitProperties(
                new Root(),
                (o, pd) -> {
                    names.add(pd.getName());
                    // "sub1Yes" is encountered, so we stop there
                    return !"sub1Yes".equals(pd.getName());
                },
                IEventListener.class);
        assertThat(names).containsExactly("sub1Yes");
    }


    //--- Test classes ---------------------------------------------------------

    public static class Root implements Serializable {
        private static final long serialVersionUID = 1L;
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

    public static class Sub1Yes implements IEventListener<Event>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public void accept(Event t) {
        }
    }
    public static class Sub2No implements Serializable {
        private static final long serialVersionUID = 1L;
    }
    public static class Sub3Yes implements IEventListener<Event>, Serializable {
        private static final long serialVersionUID = 1L;
        private Sub1Yes sub1Yes;
        private Sub3_1Yes sub3_1Yes = new Sub3_1Yes();
        public Sub3Yes() {}
        public Sub3Yes(Sub1Yes sub1Yes) {
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
        public void accept(Event t) {
        }
    }

    public static class Sub3_1Yes
            implements IEventListener<Event>, Serializable {
        private static final long serialVersionUID = 1L;
        @Override
        public void accept(Event t) {
        }
    }

    @Data
    public static class Bean implements Serializable {
        private static final long serialVersionUID = 1L;
        private String string = "potato";
        private int primitiveInt = 123;
        private Integer objectInteger = 456;
        private Event event = Event.builder("testEvent", "test").build();
        private List<Double> doubles = Arrays.asList(0.5d, 1.0d);
    }

    @ToString
    public static class SubBean extends Bean {
        private static final long serialVersionUID = 1L;
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

    @ToString
    @FieldNameConstants
    public static class MiscAccessorsBean implements Serializable {
        private static final long serialVersionUID = 1L;

        public MiscAccessorsBean() {
            // useless assignments, fix for IDE auto-save messing those up:
            boolSetterNoGetter = false;
            boolFluentSetterNoGetter = false;
        }

        // * getX();  void setX(*)
        private String normal;
        public String getNormal() {
            return normal;
        }
        public void setNormal(String normal) {
            this.normal = normal;
        }

        // -       ;  void setX(*)
        private String normalSetterNoGetter;
        public void setNormalSetterNoGetter(String normalSetterNoGetter) {
            this.normalSetterNoGetter = normalSetterNoGetter;
        }

        // * getX();  -
        private String normalGetterNoSetter;
        public String getNormalGetterNoSetter() {
            return normalGetterNoSetter;
        }

        // * getX();  <this> setX(*)
        private String fluent;
        public String getFluent() {
            return fluent;
        }
        public MiscAccessorsBean setFluent(String fluent) {
            this.fluent = fluent;
            return this;
        }

        // -       ;  <this> setX(*)
        private String fluentSetterNoGetter;
        public MiscAccessorsBean setFluentSetterNoGetter(
                String fluentSetterNoGetter) {
            this.fluentSetterNoGetter = fluentSetterNoGetter;
            return this;
        }

        // * isX() ;  void setX(*)
        private boolean boolNormal;
        public boolean isBoolNormal() {
            return boolNormal;
        }
        public void setBoolNormal(boolean boolNormal) {
            this.boolNormal = boolNormal;
        }

        // -       ;  void setX(*)
        private boolean boolSetterNoGetter;
        public void setBoolSetterNoGetter(boolean boolSetterNoGetter) {
            this.boolSetterNoGetter = boolSetterNoGetter;
        }

        // * isX() ;  -
        private boolean boolGetterNoSetter;
        public boolean isBoolGetterNoSetter() {
            return boolGetterNoSetter;
        }

        // * isX() ;  <this> setX(*)
        private boolean boolFluent;
        public boolean isBoolFluent() {
            return boolFluent;
        }
        public MiscAccessorsBean setBoolFluent(boolean boolFluent) {
            this.boolFluent = boolFluent;
            return this;
        }

        // -       ;  <this> setX(*)
        private boolean boolFluentSetterNoGetter;
        public MiscAccessorsBean setBoolFluentSetterNoGetter(
                boolean boolFluentSetterNoGetter) {
            this.boolFluentSetterNoGetter = boolFluentSetterNoGetter;
            return this;
        }

        // * x()   ;  void x(*)
        private String compactNormal;
        public String compactNormal() {
            return compactNormal;
        }
        public void compactNormal(String compactNormal) {
            this.compactNormal = compactNormal;
        }

        // -       ;  void x(*)
        private String compactSetterNoGetter;
        public void compactSetterNoGetter(String compactSetterNoGetter) {
            this.compactSetterNoGetter = compactSetterNoGetter;
        }

        // * x()   ;  -
        private String compactGetterNoSetter;
        public String compactGetterNoSetter() {
            return compactGetterNoSetter;
        }

        // * x()   ;  <this> x(*)
        private String compactFluent;
        public String compactFluent() {
            return compactFluent;
        }
        public MiscAccessorsBean compactFluent(String compactFluent) {
            this.compactFluent = compactFluent;
            return this;
        }

        // -       ;  <this> x(*)
        private String compactFluentSetterNoGetter;
        public MiscAccessorsBean compactFluentSetterNoGetter(
                String compactFluentSetterNoGetter) {
            this.compactFluentSetterNoGetter = compactFluentSetterNoGetter;
            return this;
        }
    }

    @Data
    public static class CollectionsHolder implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<String, List<CollectionEntry>> multiValueMap =
                new HashMap<>();
        private final Set<CollectionEntry> entries = new HashSet<>();

        public CollectionsHolder() {
            multiValueMap.put("key1", Arrays.asList(
                    new CollectionEntry("mapValue1aName", "mapValue1aValue")));
            multiValueMap.put("key2", null);
            entries.add(new CollectionEntry("setName1", "setValue1"));
            entries.add(new CollectionEntry("setName2", null));
        }
    }
    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CollectionEntry implements Serializable {
        private static final long serialVersionUID = 1L;
        private String entryName;
        private String entryValue;
    }
}
