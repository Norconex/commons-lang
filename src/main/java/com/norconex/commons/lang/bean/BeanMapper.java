/* Copyright 2023 Norconex Inc.
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

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.xml.stream.XMLOutputFactory;

import org.apache.commons.lang3.ArrayUtils;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.norconex.commons.lang.ClassFinder;
import com.norconex.commons.lang.convert.GenericJsonModule;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import lombok.Builder;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Singular;
import lombok.extern.slf4j.Slf4j;

/**
 * <p>
 * Simplified mapping of objects to/from XML, JSON, and Yaml.
 * </p>
 * <h3>Polymorphism</h3>
 * <p>
 * Classes annotated with {@link JsonTypeInfo} and {@link JsonSubTypes}
 * are handled by this mapper. For cases where no annotations are used,
 * you can specify one or more classes that can have subclasses with
 * {@link BeanMapperBuilder#polymorphicType(Class, Predicate)}. When doing
 * so, the classpath will be scanned for matching implementations and
 * automatically register them as subtypes.
 * To speed up the process and avoid possible classloader issues, it is
 * best that you provide a predicate to help quickly filter discovered subtypes.
 * </p>
 * <p>
 * While this mapper supports a wide variety of use cases, it is recommended
 * to use a more elaborate serialization tool for more complex needs.
 * </p>
 * @since 3.0.0
 */
@Builder
@Slf4j
public class BeanMapper { //NOSONAR
    @RequiredArgsConstructor
    public enum Format {
        XML(
                XmlMapper::builder,
                b -> {
                    var m = (XmlMapper) b.build();
                    m.getFactory()
                        .getXMLOutputFactory()
                        .setProperty(
                                XMLOutputFactory.IS_REPAIRING_NAMESPACES, true);
                    return m;
                }),
        JSON(
                JsonMapper::builder,
                MapperBuilder::build),
        YAML(
                () -> YAMLMapper.builder()
                    .disable(YAMLGenerator.Feature.USE_NATIVE_TYPE_ID),
                MapperBuilder::build),
        ;
        final Supplier<MapperBuilder<?, ?>> builder;
        final Function<MapperBuilder<?, ?>, ObjectMapper> mapper;
    }

    /**
     * A build mapper initialized with default settings.
     */
    public static final BeanMapper DEFAULT = BeanMapper.builder().build();

    /** Whether to silently ignored unknown mapped properties when reading. */
    private boolean ignoreUnknownProperties;
    /** Whether to treat empty strings as <code>null</code> when reading. */
    private boolean treatEmptyAsNull;
    /** Whether to skip validation when reading. */
    private boolean skipValidation;
    /** Whether to indent elements when writing. */
    private boolean indent;
    /**
     * Registry of classes which may have subclasses, with optional predicate
     * to filter said subclasses by their fully qualified name. Not
     * relevant for beans without polymorphic types, or annotated with
     * a combination of {@link JsonTypeInfo} and
     * {@link JsonSubTypes}.
     */
    @Singular
    private Map<Class<?>, Predicate<String>> polymorphicTypes;

    // We are caching them on first use
    private final Map<Format, ObjectMapper> cache = new ConcurrentHashMap<>(3);

    /**
     * Write the given object as XML, JSON, or Yaml.
     * @param object the object to write
     * @param writer the target to write to
     * @param format the target format to write
     * @throws BeanException if writing failed
     */
    public void write(
            @NonNull Object object,
            @NonNull Writer writer,
            @NonNull Format format) {
        try {
            getMapper(format).writeValue(writer, object);
        } catch (IOException e) {
            throw new BeanException("Could not write object to %s: %s"
                    .formatted(format, object), e);
        }
    }

    /**
     * Reads an XML, JSON, or Yaml source and map it into an existing object.
     * @param <T> the type of the object to populate
     * @param object the object to populate
     * @param reader the source content to read
     * @param format the source format
     * @return populated object (same instance)
     * @throws BeanException if reading failed
     * @throws ConstraintViolationException on bean validation error
     */
    public <T> T read(
            @NonNull T object,
            @NonNull Reader reader,
            @NonNull Format format) {
        return doRead(mapper -> {
            try {
                return mapper.readerForUpdating(object).readValue(reader);
            } catch (IOException e) {
                throw new BeanException("Could not read %s for object: %s"
                        .formatted(format, object), e);
            }
        }, format);
    }

    /**
     * Reads an XML, JSON, or Yaml source and map it into a new object
     * of the given type.
     * @param <T> the type of the object returned
     * @param type a class of the expected returned type
     * @param reader the source content to read
     * @param format  the source format
     * @return populated object
     * @throws BeanException if reading failed
     * @throws ConstraintViolationException on bean validation error
     */
    public <T> T read(
            @NonNull Class<T> type,
            @NonNull Reader reader,
            @NonNull Format format) {
        return doRead(mapper -> {
            try {
                return mapper.readValue(reader, type);
            } catch (IOException e) {
                throw new BeanException("Could not read %s for class: %s"
                        .formatted(format, type), e);
            }
        }, format);
    }

    /**
     * Throws a {@link BeanException} if the given object is not equal
     * to itself after writing it to specified formats and back. Not specifying
     * any format is equivalent to testing them all (XML, JSON, and Yaml).
     * @param obj the object
     * @param formats zero or more formats
     */
    public void assertWriteRead(@NonNull Object obj, Format... formats) {
        var resolvedFormats =
                (ArrayUtils.isEmpty(formats) ? Format.values() : formats);
        for (Format format : resolvedFormats) {
            assertWriteRead(obj, format);
        }
    }

    private ObjectMapper getMapper(Format format) {
        return cache.computeIfAbsent(format, fmt -> {

            var builder = format.builder.get();
            builder.enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);

            var mapper = format.mapper.apply(builder);

            // read:
            mapper.configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    !ignoreUnknownProperties);
            mapper.configure(Feature.AUTO_CLOSE_SOURCE, false);
            mapper.configure(
                    DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                    treatEmptyAsNull);

            // write:
            mapper.configure(SerializationFeature.INDENT_OUTPUT, indent);
            mapper.enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
            mapper.setSerializationInclusion(Include.NON_EMPTY);

            // modules:
            mapper.registerModule(new ParameterNamesModule());
            mapper.registerModule(new Jdk8Module());
            mapper.registerModule(new JavaTimeModule());
            mapper.registerModule(new GenericJsonModule());

            // register polymorphic types:
            registerPolymorphicTypes(mapper);

            return mapper;
        });
    }

    private void registerPolymorphicTypes(ObjectMapper mapper) {
        polymorphicTypes.forEach((type, predicate) -> {
            mapper.addMixIn(type, PolymorphicMixIn.class);
            mapper.registerSubtypes(ClassFinder.findSubTypes(
                    type, predicate).toArray(new Class<?>[] {}));
        });
    }

    private <T> T doRead(
            @NonNull Function<ObjectMapper, T> f,
            @NonNull Format format) {
        var obj = f.apply(getMapper(format));
        if (!skipValidation) {
            var factory = Validation.buildDefaultValidatorFactory();
            var validator = factory.getValidator();
            Set<ConstraintViolation<Object>> violations =
                    validator.validate(obj);
            if (!violations.isEmpty() ) {
                throw new ConstraintViolationException(
                        "Object validation failed when reading %s: "
                        .formatted(format),
                        violations);
            }
        }
        return obj;
    }

    private void assertWriteRead(Object object, Format format) {
        var out = new StringWriter();
        write(object, out, format);
        LOG.info("{} written for object {}:\n{}",
                format, object.getClass(), out.toString());
        var in = new StringReader(out.toString());
        Object readObject = read(object.getClass(), in, format);
        if (!object.equals(readObject)) {
            if (LOG.isErrorEnabled()) {
                LOG.error(" SAVED: {}", object);
                LOG.error("LOADED: {}", readObject);
                LOG.error("  DIFF: \n{}\n", BeanUtil.diff(object, readObject));
            }
            throw new BeanException(
                    "Saved and loaded " + format + " are not the same.");
        }
    }


    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class")
    abstract static class PolymorphicMixIn {}
}
