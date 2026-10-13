/* Copyright 2026 Norconex Inc.
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
package com.norconex.commons.lang.bean.jackson;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import tools.jackson.databind.BeanDescription;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationConfig;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.BeanPropertyDefinition;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.BeanPropertyWriter;
import tools.jackson.databind.ser.PropertyBuilder;
import tools.jackson.databind.util.Annotations;

/**
 * A Jackson {@link PropertyBuilder} that writes a property explicitly set to
 * <code>null</code> when its default value is <em>not</em> null, so that the
 * <code>null</code> round-trips and "clears" the default when read back.
 * <p>
 * Why it is needed: BeanMapper writes only what differs from a bean's defaults
 * (<code>NON_DEFAULT</code>, comparing against a real default instance of the
 * bean). Since Jackson 3.2 ([databind#1757]) that comparison also suppresses a
 * <code>null</code> value, whatever the default is. For a configuration, that
 * silently turns a default component back on: a property such as
 * <code>sitemapResolver</code> that is non-null by default and set to
 * <code>null</code> to disable it would simply not be written, and reading the
 * result back would bring the default back.
 * </p>
 * <p>
 * What it does, exactly, and nothing more:
 * </p>
 * <ul>
 *   <li>A property left at its non-null default is <b>not</b> written (the
 *       reader uses the default).</li>
 *   <li>A property set to null whose default is non-null <b>is</b> written, as
 *       null (this class).</li>
 *   <li>A property set to null whose default is null is <b>not</b> written.</li>
 *   <li>Anything else is Jackson's own behavior: an explicit per-property
 *       inclusion (for example <code>@JsonInclude(NON_NULL)</code>) is still
 *       respected, and so is a bean with no real default to compare to.</li>
 * </ul>
 * <p>
 * It extends Jackson's class and is installed through the supported
 * {@link NullKeepingSerializerFactory}, instead of replacing Jackson's class
 * by providing another one under the same name. That older approach only worked
 * when this library came before Jackson on the class path, which an
 * application packaged as one jar does not guarantee.
 * </p>
 * <p>
 * Relies on {@link tools.jackson.databind.MapperFeature#USE_REAL_INCLUDE_NON_DEFAULT}
 * being enabled, as {@link com.norconex.commons.lang.bean.BeanMapper} does.
 * </p>
 */
public class NullKeepingPropertyBuilder extends PropertyBuilder {

    public NullKeepingPropertyBuilder(
            SerializationConfig config, BeanDescription beanDesc) {
        super(config, beanDesc);
    }

    // buildWriter() decides, for NON_DEFAULT, to suppress nulls and to suppress
    // the bean's real default value. It then hands both decisions here, which
    // is the one place we change: when the value to suppress is a real, non-null
    // default, nulls are kept.
    @Override
    protected BeanPropertyWriter _constructPropertyWriter(
            BeanPropertyDefinition propDef,
            AnnotatedMember member,
            Annotations contextAnnotations,
            JavaType declaredType,
            ValueSerializer<?> ser,
            TypeSerializer typeSer,
            JavaType serType,
            boolean suppressNulls,
            Object suppressableValue,
            Class<?>[] includeInViews,
            JsonInclude.Value inclV,
            Class<?> applyView) {
        return super._constructPropertyWriter(
                propDef, member, contextAnnotations, declaredType, ser,
                typeSer, serType,
                suppressNulls && !keepsNull(suppressableValue, inclV),
                suppressableValue, includeInViews, inclV, applyView);
    }

    /**
     * Whether a null is to be written because the suppressed value is a real,
     * non-null default of the bean (so that a null is a deliberate change
     * from it).
     */
    private boolean keepsNull(
            Object suppressableValue, JsonInclude.Value inclV) {
        return inclV.getValueInclusion() == Include.NON_DEFAULT
                && _useRealPropertyDefaults
                && suppressableValue != null
                && suppressableValue != BeanPropertyWriter.MARKER_FOR_EMPTY
                // The default bean the value came from; already created, and
                // cached, by the time we are asked.
                && getDefaultBean() != null;
    }
}
