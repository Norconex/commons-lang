/* Copyright 2023-2024 Norconex Inc.
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

import static com.fasterxml.jackson.dataformat.xml.deser.FromXmlParser.Feature.EMPTY_ELEMENT_AS_NULL;
import static java.util.Optional.ofNullable;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.xml.stream.XMLOutputFactory;

import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSetter.Value;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.Nulls;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonParser.Feature;
import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.cfg.MapperBuilder;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.norconex.commons.lang.ClassFinder;
import com.norconex.commons.lang.ClassUtil;
import com.norconex.commons.lang.bean.jackson.EmptyWithClosingTagXmlFactory;
import com.norconex.commons.lang.bean.jackson.JsonXmlCollectionModule;
import com.norconex.commons.lang.bean.jackson.JsonXmlPropertiesDeserializer;
import com.norconex.commons.lang.bean.spi.PolymorphicTypeLoader;
import com.norconex.commons.lang.bean.spi.PolymorphicTypeProvider;
import com.norconex.commons.lang.config.Configurable;
import com.norconex.commons.lang.convert.GenericJsonModule;
import com.norconex.commons.lang.flow.FlowMapperConfig;
import com.norconex.commons.lang.flow.module.FlowModule;
import com.norconex.commons.lang.map.Properties;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validation;
import lombok.Builder;
import lombok.Builder.Default;
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
 * Polymorphism is supported in a few different ways.
 * </p>
 * <h3>Via Annotations</h3>
 * <p>
 * First, classes annotated with {@link JsonTypeInfo} and {@link JsonSubTypes}
 * are properly handled by this mapper.
 * </p>
 * <h3>Via registration</h3>
 * <p>
 * For cases where no annotations are used, you can register one or more
 * classes (typically interfaces) that can have subclasses with
 * {@link BeanMapperBuilder#polymorphicType(Class, Predicate)}.
 * When doing so, the classpath will be scanned for matching implementations
 * and automatically register them as subtypes using their simple class name.
 * To speed up the discovery process and avoid possible classloader issues,
 * it is best that you provide a predicate (over fully qualified class names)
 * to help quickly filter discovered subtypes.
 * It relies on the "class" property to find the class reference and
 * (de)serialize sub-types.
 * </p>
 *
 * <h3>Class mapping</h3>
 * <p>
 * When auto-registering subtypes, class short names are used (class name
 * without package name). You can optionally provide in the source the full
 * canonical name of a class to have that class recognized at deserialization
 * time, regardless whether it was registered or not. This can be useful
 * when dynamically adding classes that could not be previously registered for
 * some reason.
 * </p>
 * <p>
 * While this mapper supports a wide variety of use cases, it is recommended
 * to use a more elaborate serialization tool for more complex needs.
 * </p>
 * <h3>Configurable objects</h3>
 * <p>
 * Objects implementing {@link Configurable} indicates that a
 * separate class dedicated to configuration is used for bean-style mapping.
 * By default, this mapper will ignore all properties of a configurable class
 * except for its <code>getConfiguration()</code> method which will be
 * treated as if annotated with <code>{@literal @}valid</code>. That
 * configuration class will be populated without the need for "configuration"
 * wrapper elements (automatically adds <code>{@literal @}JsonUnwrapped</code>
 * This behavior can be turned off with {@link #configurableDetectionDisabled}
 * </p>
 * @since 3.0.0
 */
@Builder
@Slf4j
public class BeanMapper { //NOSONAR
    @RequiredArgsConstructor
    public enum Format {
        XML(
                () -> XmlMapper.builder(new EmptyWithClosingTagXmlFactory()),
                //XmlMapper::builder,
                b -> {
                    var m = (XmlMapper) b.build();
                    m.enable(EMPTY_ELEMENT_AS_NULL);
                    m.getFactory()
                            .getXMLOutputFactory()
                            .setProperty(
                                    XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                                    true);
                    m.registerModule(new SimpleModule().addDeserializer(
                            Properties.class,
                            new JsonXmlPropertiesDeserializer()));
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

    /**
     * Whether to disable resolving of fully qualified class names in source
     * when reading.
     */
    private boolean canonicalNameSupportDisabled;

    /**
     * Whether to disable detecting {@link Configurable} classes, which
     * when <code>false</code> (i.e., enabled), will automatically mark
     * the configuration class as <code>{@literal @}valid</code> and ignore
     * the configurable class properties when writing/reading.
     */
    private boolean configurableDetectionDisabled;

    /**
     * Whether to disable detecting polymorphic types and their subtypes
     * using Java Service Loader (SPI) mechanism.
     * @see PolymorphicTypeProvider
     */
    private boolean polymorphicServiceLoaderDisabled;

    /** Whether to skip validation when reading. */
    private boolean skipValidation;

    /** Whether to indent elements when writing. */
    private boolean indent;

    /**
     * The class loader to use to load classes. Defaults to this
     * class class loader.
     */
    private ClassLoader classLoader;

    /**
     * Consumes the internal Jackson {@link MapperBuilder} for custom
     * initialization.
     */
    private Consumer<MapperBuilder<?, ?>> mapperBuilderCustomizer;

    /**
     * Registry of classes which may have subclasses, with optional predicate
     * to filter said subclasses by their fully qualified name. Not
     * relevant for beans without polymorphic types, or annotated with
     * a combination of {@link JsonTypeInfo} and
     * {@link JsonSubTypes}.
     */
    @Singular
    private Map<Class<?>, Predicate<String>> polymorphicTypes;

    /**
     * Optionally register properties to be (de)serialialized to a class.
     * Only applicable when the property is not already bound to a class via
     * regular JSON mappings.
     */
    @Singular
    private Map<String, Class<?>> unboundPropertyMappings;

    /**
     * Optionally register default concrete implementations for polymorphic
     * type/interface. The default is used when no type id is defined.
     * (i.e., "class").
     */
    @Singular
    private Map<Class<?>, Class<?>> defaultPolymorphicTypes;

    /**
     * Allows for manually registering polymorphic types. The implementations
     * (map values) must extend the base class (map key) or it will fail.
     */
    @Singular
    private Map<Class<?>, List<Class<?>>> polymorphicTypeImpls;

    /**
     * Optionally setup support for using flow/conditions in your
     * your source.
     */
    @Default
    @NonNull
    private FlowMapperConfig flowMapperConfig = new FlowMapperConfig();

    /**
     * Optionally register source properties to be ignored when read.
     */
    @Singular
    private Set<String> ignoredProperties;

    // We are caching them on first use
    private final Map<Format, ObjectMapper> cache = new ConcurrentHashMap<>(3);

    // We keep a copy of resolved polymorphic type as they are otherwise
    // difficult to obtain from the ObjectMapper and we may need them in
    // different context (e.g., registration in OpenApi).
    private final MultiValuedMap<Class<?>, Class<?>> resolvedPolymorphicTypes =
            MultiMapUtils.newListValuedHashMap();
    private final MutableBoolean polyTypesResolved = new MutableBoolean();

    public MultiValuedMap<Class<?>, Class<?>> getPolymorphicTypes() {
        resolvePolymorphicTypes();
        return resolvedPolymorphicTypes;
    }

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
            toObjectMapper(format).writeValue(writer, object);
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
    @SuppressWarnings("unchecked")
    public <T> T read(
            @NonNull T object,
            @NonNull Reader reader,
            @NonNull Format format) {
        try {
            return (T) validate(toObjectMapper(format)
                    .readerForUpdating(object).readValue(reader));
        } catch (IOException e) {
            throw new BeanException(
                    "Could not read %s source.".formatted(format), e);
        }
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
    @SuppressWarnings("unchecked")
    public <T> T read(
            @NonNull Class<T> type,
            @NonNull Reader reader,
            @NonNull Format format) {
        try {
            return (T) validate(
                    toObjectMapper(format).readerFor(type).readValue(reader));
        } catch (IOException e) {
            LOG.error(e.getLocalizedMessage());
            throw new BeanException(
                    "Could not read %s source.".formatted(format), e);
        }
    }

    private Object validate(Object obj) {
        if (obj != null && !skipValidation) {
            var factory = Validation.buildDefaultValidatorFactory();
            var validator = factory.getValidator();
            Set<ConstraintViolation<Object>> violations =
                    validator.validate(obj);
            if (!violations.isEmpty()) {
                throw new ConstraintViolationException(
                        "Object validation failed for object of type: %s"
                                .formatted(obj.getClass().getName()),
                        violations);
            }
        }
        return obj;
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

    public static Format detectFormat(@NonNull String str) {
        var cfg = str.stripLeading();
        if (cfg.startsWith("{")) {
            return Format.JSON;
        }
        if (cfg.startsWith("<")) {
            return Format.XML;
        }
        return Format.YAML;
    }

    /**
     * Gets a Jackson {@link ObjectMapper} for the given format.
     * @param format format for which to get the mapper
     * @return Jackson ObjectMapper
     */
    public ObjectMapper toObjectMapper(Format format) {
        return cache.computeIfAbsent(format, fmt -> {

            //--- Builder ---

            var builder = format.builder.get();

            // general features:
            builder.disable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY);
            builder.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
            builder.enable(Feature.ALLOW_COMMENTS);
            builder.enable(Feature.ALLOW_YAML_COMMENTS);
            builder.configure(JsonReadFeature.ALLOW_UNQUOTED_FIELD_NAMES
                    .mappedFeature(), true);
            builder.configure(JsonReadFeature.ALLOW_TRAILING_COMMA
                    .mappedFeature(), true);
            if (LOG.isDebugEnabled()) {
                builder.enable(Feature.INCLUDE_SOURCE_IN_LOCATION);
            }

            // handlers:
            builder.addHandler(new BeanMapperPropertyHandler(this));

            // modules:
            builder.addModule(new ParameterNamesModule());
            builder.addModule(new Jdk8Module());
            builder.addModule(new JavaTimeModule());
            // Nx modules and mix-ins
            builder.addModule(new GenericJsonModule());
            if (!configurableDetectionDisabled) {
                builder.addMixIn(Configurable.class, ConfigurableMixIn.class);
            }
            builder.addModule(new FlowModule(flowMapperConfig));
            if (mapperBuilderCustomizer != null) {
                mapperBuilderCustomizer.accept(builder);
            }
            if (!ignoreUnknownProperties) {
                builder.addModule(
                        new FailOnUnknownConfigurablePropModule(this));
            }

            //--- Mapper ---

            var mapper = format.mapper.apply(builder);

            // read:
            mapper.configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    !ignoreUnknownProperties);
            mapper.configure(Feature.AUTO_CLOSE_SOURCE, false);
            mapper.configure(
                    DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                    treatEmptyAsNull);
            mapper.disable(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
            mapper.setDefaultSetterInfo(
                    Value
                            .empty()
                            .withValueNulls(Nulls.SET) // value
                            .withContentNulls(Nulls.SET)); // collection entry value

            // write:
            mapper.configure(SerializationFeature.INDENT_OUTPUT, indent);
            mapper.disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

            // register polymorphic types:
            registerPolymorphicTypes(mapper);

            // misc:
            if (format == Format.XML) {
                mapper.registerModule(new JsonXmlCollectionModule());
            }

            mapper.addMixIn(Object.class, NonDefaultInclusionMixIn.class);

            return mapper;
        });
    }

    //     @JsonIgnoreType  // <-- messes up nested object deserializing since update of Jackson
    abstract static class ConfigurableMixIn<T> {
        @JsonUnwrapped
        @Valid
        abstract T getConfiguration();

        @JsonUnwrapped
        void setConfiguration(T configuration) {
        }
    }

    //NOTE: we need to set NON_DEFAULT here since setting it globally
    // assumes "default" is the property type default, not the object
    // initialization of those properties.
    @JsonInclude(value = Include.NON_DEFAULT)
    abstract static class NonDefaultInclusionMixIn {
    }

    private synchronized void resolvePolymorphicTypes() {
        if (polyTypesResolved.isTrue()) {
            // already resolved for this instance, we skip.
            return;
        }

        //--- Directly specified ---
        polymorphicTypeImpls.forEach(resolvedPolymorphicTypes::putAll);

        //--- From service loader ---
        var cl = ofNullable(classLoader).orElseGet(
                () -> getClass().getClassLoader());
        if (!polymorphicServiceLoaderDisabled) {
            PolymorphicTypeLoader.polymorphicTypes(cl).asMap().forEach(
                    resolvedPolymorphicTypes::putAll);
        }

        //--- Configured ---
        polymorphicTypes
                .forEach((type, predicate) -> resolvedPolymorphicTypes.putAll(
                        type, ClassFinder.findSubTypes(type, predicate)));

        //--- Flow-specific ---
        Class<?> conditionType =
                flowMapperConfig.getPredicateType().getBaseType();
        if (conditionType != null) {
            resolvedPolymorphicTypes.putAll(
                    conditionType,
                    ClassFinder.findSubTypes(conditionType,
                            flowMapperConfig
                                    .getPredicateType()
                                    .getScanFilter()));
        }
        Class<?> consumerType =
                flowMapperConfig.getConsumerType().getBaseType();
        if (consumerType != null) {
            resolvedPolymorphicTypes.putAll(
                    consumerType,
                    ClassFinder.findSubTypes(consumerType,
                            flowMapperConfig
                                    .getConsumerType()
                                    .getScanFilter()));
        }

        polyTypesResolved.setTrue();
    }

    private void registerPolymorphicTypes(ObjectMapper mapper) {
        resolvePolymorphicTypes();

        resolvedPolymorphicTypes.asMap().forEach((type, subTypes) -> {
            registerPolymorphicType(mapper, type, subTypes);
        });
    }

    private void registerPolymorphicType(
            ObjectMapper mapper,
            Class<?> type,
            Collection<? extends Class<?>> subTypes) {

        resolvedPolymorphicTypes.putAll(type, subTypes);

        //MAYBE: check and report those already registered?
        mapper.addMixIn(type, PolymorphicMixIn.class);
        mapper.registerSubtypes(subTypes.toArray(new Class<?>[] {}));
        if (LOG.isDebugEnabled()) {
            LOG.debug("Registered polymorphic type and its sub-types:\n{}\n{}",
                    "  ▪ " + type.getName(),
                    StringUtils.join(subTypes.stream()
                            .map(st -> "    ▫ " + st.getName())
                            .toList(), "\n"));
        }
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
                LOG.error(" SAVED ► {}: {}", format, object);
                LOG.error("LOADED ◄ {}: {}", format, readObject);
                LOG.error("DIFF: \n{}\n", BeanUtil.diff(object, readObject));
            }
            throw new BeanException(
                    "Saved and loaded " + format + " are not the same.");
        }
    }

    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "class"
    )
    abstract static class PolymorphicMixIn {
    }

    @RequiredArgsConstructor
    static class BeanMapperPropertyHandler
            extends DeserializationProblemHandler {
        private final BeanMapper beanMapper;

        @Override
        public boolean handleUnknownProperty(
                DeserializationContext ctxt,
                JsonParser p,
                JsonDeserializer<?> deserializer,
                Object beanOrClass,
                String propertyName) throws IOException {

            if ("class".equals(propertyName)
                    || beanMapper.ignoredProperties.contains(propertyName)) {
                ctxt.readTree(p);
                return true;
            }
            Class<?> cls = beanMapper.unboundPropertyMappings.get(propertyName);
            if (cls != null) {
                p.assignCurrentValue(ClassUtil.newInstance(cls));
                return true;
            }
            return false;
        }

        @Override
        public JavaType handleMissingTypeId(
                DeserializationContext ctxt,
                JavaType baseType,
                TypeIdResolver idResolver,
                String failureMsg) throws IOException {

            var defaultType = beanMapper.defaultPolymorphicTypes.get(
                    baseType.getRawClass());

            // No default exists, let super handle it.
            if (defaultType == null) {
                return super.handleMissingTypeId(
                        ctxt, baseType, idResolver, failureMsg);
            }

            // Default exists, return it.
            var type = TypeFactory.defaultInstance()
                    .constructType(defaultType);
            if (type.isTypeOrSubTypeOf(baseType.getRawClass())) {
                return type;
            }
            throw new BeanException(
                    "Default polymorphic type %s not a subtype of %s."
                            .formatted(type.getTypeName(),
                                    baseType.getTypeName()));
        }

        @Override
        public JavaType handleUnknownTypeId(DeserializationContext ctxt,
                JavaType baseType, String subTypeId, TypeIdResolver idResolver,
                String failureMsg) throws IOException {
            if (beanMapper.canonicalNameSupportDisabled) {
                return null;
            }

            // if subTypeId is the fully qualified name of a valid sub type,
            // resolve it.
            JavaType type;
            try {
                type = TypeFactory.defaultInstance()
                        .constructFromCanonical(subTypeId);
                if (type.isTypeOrSubTypeOf(baseType.getRawClass())) {
                    return type;
                }
            } catch (IllegalArgumentException e) {
                // Swallow
            }
            // We can't handle it
            return null;
        }
    }
}
