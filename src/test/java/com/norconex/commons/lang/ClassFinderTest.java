/* Copyright 2019-2022 Norconex Inc.
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
package com.norconex.commons.lang;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.norconex.commons.lang.convert.CharsetConverter;
import com.norconex.commons.lang.convert.DurationConverter;
import com.norconex.commons.lang.convert.IConverter;
import com.norconex.commons.lang.convert.LocaleConverter;

/**
 * Class-scanning tests
 * @author Pascal Essiembre
 */
class ClassFinderTest {

    @Test
    void testFindSubTypesClass() {
        List<Class<? extends IConverter>> types =
                ClassFinder.findSubTypes(IConverter.class);
        assertThat(types).contains(
                DurationConverter.class,
                CharsetConverter.class,
                LocaleConverter.class);
    }

    @Test
    void testSubTypesClassPredicate() {
        String toFind = ".DurationConverter";
        List<Class<? extends IConverter>> types = ClassFinder.findSubTypes(
                IConverter.class, s -> s.endsWith(toFind));
        assertThat(types).hasSize(1);
        assertThat(types.get(0)).isEqualTo(DurationConverter.class);
    }

    @Test
    void testFindSubTypesListClass() {
        List<Class<? extends IConverter>> types = ClassFinder.findSubTypes(
                Arrays.asList(new File("target/classes")), IConverter.class);
        assertThat(types).contains(
                DurationConverter.class,
                CharsetConverter.class,
                LocaleConverter.class);
    }

    @Test
    void testFindSubTypesListClassPredicate() {
        String toFind = ".DurationConverter";
        List<Class<? extends IConverter>> types = ClassFinder.findSubTypes(
                Arrays.asList(new File("target/classes")),
                IConverter.class,
                s -> s.endsWith(toFind));
        assertThat(types).hasSize(1);
        assertThat(types.get(0)).isEqualTo(DurationConverter.class);

        assertThat(ClassFinder.findSubTypes(
                (List<File>) null, IConverter.class)).isEmpty();
        assertThat(ClassFinder.findSubTypes(
                Arrays.asList(new File("target/classes")), null)).isEmpty();
    }

    @Test
    void testFindSubTypesFileClass() {
        List<Class<? extends IConverter>> types = ClassFinder.findSubTypes(
                new File("target/classes"), IConverter.class);
        assertThat(types).contains(
                DurationConverter.class,
                CharsetConverter.class,
                LocaleConverter.class);
    }

    @Test
    void testFindSubTypesFileClassPredicate() {
        String toFind = ".DurationConverter";
        List<Class<? extends IConverter>> types = ClassFinder.findSubTypes(
                new File("target/classes"),
                IConverter.class,
                s -> s.endsWith(toFind));
        assertThat(types).hasSize(1);
        assertThat(types.get(0)).isEqualTo(DurationConverter.class);

        assertThat(ClassFinder.findSubTypes(
                (File) null, IConverter.class)).isEmpty();
        assertThat(ClassFinder.findSubTypes(
                new File("target/classes"), null)).isEmpty();
    }
}
