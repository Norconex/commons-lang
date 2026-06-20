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

import static java.util.Optional.ofNullable;
import static tools.jackson.dataformat.xml.XmlReadFeature.EMPTY_ELEMENT_AS_NULL;

import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import javax.xml.stream.XMLOutputFactory;

import org.apache.commons.collections4.MultiMapUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonSetter.Value;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.Nulls;
import com.norconex.commons.lang.ClassFinder;
import com.norconex.commons.lang.ClassUtil;
import com.norconex.commons.lang.bean.jackson.EmptyWithClosingTagXmlFactory;
import com.norconex.commons.lang.bean.jackson.JsonXmlCollectionModule;
import com.norconex.commons.lang.bean.jackson.JsonXmlMapModule;
import com.norconex.commons.lang.bean.jackson.JsonXmlPropertiesModule;
import com.norconex.commons.lang.bean.spi.PolymorphicTypeLoader;
import com.norconex.commons.lang.bean.spi.PolymorphicTypeProvider;
import com.norconex.commons.lang.config.Configurable;
import com.norconex.commons.lang.convert.GenericJsonModule;
import com.norconex.commons.lang.flow.FlowMapperConfig;
import com.norconex.commons.lang.flow.module.FlowModule;

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
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLReadFeature;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * <p>
 * Simplified mapping of objects to/from XML, JSON, and Yaml.
 * </p>
 * <h2>Polymorphism</h2>
 * <p>
 * Polymorphism is supported in a few different ways.
 * </p>
 * <h2>Via Annotations</h2>
 * <p>
 * First, classes annotated with {@link JsonTypeInfo} and {@link JsonSubTypes}
 * are properly handled by this mapper.
 * </p>
 * <h2>Via registration</h2>
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
 * <h2>Class mapping</h2>
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
 * <h2>Configurable objects</h2>
 * <p>
 * Objects implementing {@link Configurable} indicates that a
 * separate class dedicated to configuration is used for bean-style mapping.
 * By default, this mapper will ignore all properties of a configurable class
 * except for its <code>getConfiguration()</code> method which will be
 * treated as if annotated with <code>{@literal @}valid</code>. That
 * configuration class will be populated without the need for "configuration"
 * wrapper elements (automatically adds <code>{@literal @}JsonUnwrapped</code>
 * This behavior can be turned off with
 * {@link BeanMapperBuilder#configurableDetectionDisabled(boolean)}
 * </p>
 * @since 3.0.0
 */
@Builder
@Slf4j
public class BeanMapper { //NOSONAR
    @RequiredArgsConstructor
    public enum Format {
        XML(
                () -> XmlMapper.builder(new EmptyWithClosingTagXmlFactory())
                        .configureForJackson2(),
                b -> {
                    var m = (XmlMapper) b.build();
                    m.tokenStreamFactory()
                            .getXMLOutputFactory()
                            .setProperty(
                                    XMLOutputFactory.IS_REPAIRING_NAMESPACES,
                                    true);
                    return m;
                }),
        JSON(
                JsonMapper::builderWithJackson2Defaults,
                MapperBuilder::build),
        YAML(
                () -> YAMLMapper.builder()
                        .disable(YAMLWriteFeature.USE_NATIVE_TYPE_ID),
                MapperBuilder::build),
                ;

        final Supplier<MapperBuilder<?, ?>> builder;
        final Function<MapperBuilder<?, ?>, ObjectMapper> mapper;

        /**
         * Resolve the format based on the file extension for a given path.
         * Supported extensions are ".json", ".yaml", ".yml", and ".xml"
         * (all case-insensitive).
         * @param path the path to evaluate
         * @return the detected format or <code>null</code> if path is
         *     <code>null</code> or does not have a supported extension.
         */
        public static Format fromPath(Path path) {
            return fromPath(path, null);
        }

        /**
         * Resolve the format based on the file extension for a given path.
         * Supported extensions are ".json", ".yaml", ".yml", and ".xml"
         * (all case-insensitive).
         * @param path the path to evaluate
         * @param defaultFormat format returned if the path is <code>null</code>
         *     or does not have a supported extension
         * @return the detected or default format
         */
        public static Format fromPath(Path path, Format defaultFormat) {
            if (path == null) {
                return defaultFormat;
            }
            Format format;
            var asStr = path.toString().toLowerCase();
            if (asStr.endsWith(".json")) {
                format = Format.JSON;
            } else if (asStr.endsWith(".yaml") || asStr.endsWith(".yml")) {
                format = Format.YAML;
            } else if (asStr.endsWith(".xml")) {
                format = Format.XML;
            } else {
                format = defaultFormat;
            }
            return format;
        }

        /**
         * Resolve the format based on supplied content.
         * @param content the content to evaluate
         * @return the detected format or <code>null</code> if the content is
         *     blank or the format could not be detected
         */
        public static Format fromContent(String content) {
            return fromContent(content, null);
        }

        /**
         * Resolve the format based on supplied content.
         * @param content the content to evaluate
         * @param defaultFormat format returned if the content is blank
         *     or the format could not be detected
         * @return the detected or default format
         */
        public static Format fromContent(String content, Format defaultFormat) {
            if (StringUtils.isBlank(content)) {
                return defaultFormat;
            }
            var cfg = content.stripLeading();
            if (cfg.startsWith("{")) {
                return Format.JSON;
            }
            if (cfg.startsWith("<")) {
                return Format.XML;
            }
            return Format.YAML;
        }
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
    private final AtomicBoolean polyTypesResolved = new AtomicBoolean();

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
        } catch (JacksonException e) {
            rethrowValidationCause(e);
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
        } catch (JacksonException e) {
            rethrowValidationCause(e);
            throw new BeanException(
                    "Could not read %s source: %s"
                            .formatted(format,
                                    legacyJacksonMessage(e.getMessage())),
                    e);
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
        } catch (JacksonException e) {
            rethrowValidationCause(e);
            // Log the error with full context for easier debugging
            LOG.error("Failed to read {} source for type: {}. Error: {}",
                    format, type.getName(), e.getMessage());
            LOG.debug("Error stacktrace:\n", e);
            throw new BeanException(
                    "Could not read %s source for type %s: %s"
                            .formatted(format, type.getName(),
                                    legacyJacksonMessage(e.getMessage())),
                    e);
        }
    }

    private static void rethrowValidationCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException cve) {
                throw cve;
            }
            current = current.getCause();
        }
    }

    private static String legacyJacksonMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.replace("Unrecognized property", "Unrecognized field");
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
            builder.enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS);
            builder.enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS);
            builder.enable(MapperFeature.USE_GETTERS_AS_SETTERS);
            builder.defaultMergeable(false);
            builder.disable(StreamReadFeature.AUTO_CLOSE_SOURCE);
            if (LOG.isDebugEnabled()) {
                builder.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION);
            }
            if (builder instanceof JsonMapper.Builder jsonBuilder) {
                jsonBuilder.enable(JsonReadFeature.ALLOW_JAVA_COMMENTS);
                jsonBuilder.enable(JsonReadFeature.ALLOW_YAML_COMMENTS);
                jsonBuilder
                        .enable(JsonReadFeature.ALLOW_UNQUOTED_PROPERTY_NAMES);
                jsonBuilder.enable(JsonReadFeature.ALLOW_TRAILING_COMMA);
            }
            if (builder instanceof YAMLMapper.Builder yamlBuilder) {
                yamlBuilder.enable(YAMLReadFeature.EMPTY_STRING_AS_NULL);
                yamlBuilder.withCoercionConfig(LogicalType.POJO, cfg -> cfg
                        .setCoercion(CoercionInputShape.String,
                                CoercionAction.AsNull));
            }
            if (builder instanceof XmlMapper.Builder xmlBuilder) {
                xmlBuilder.enable(EMPTY_ELEMENT_AS_NULL);
            }
            // handlers:
            builder.addHandler(new BeanMapperPropertyHandler(this));

            // read:
            builder.configure(
                    DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES,
                    !ignoreUnknownProperties);
            builder.configure(
                    DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                    treatEmptyAsNull);
            builder.configure(
                    DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES,
                    false);
            builder.disable(
                    DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY);
            builder.changeDefaultNullHandling(v -> Value
                    .empty()
                    .withValueNulls(Nulls.SET)
                    .withContentNulls(Nulls.SET));
            builder.withCoercionConfig(LogicalType.POJO, cfg -> cfg
                    .setAcceptBlankAsEmpty(true)
                    .setCoercion(CoercionInputShape.EmptyString,
                            CoercionAction.AsEmpty));

            // write:
            builder.configure(SerializationFeature.INDENT_OUTPUT, indent);
            builder.disable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

            // modules:
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

            // register polymorphic types and mix-ins before building.
            registerPolymorphicTypes(builder);
            if (format == Format.XML) {
                builder.addModule(new JsonXmlCollectionModule());
                builder.addModule(new JsonXmlMapModule());
                builder.addModule(new JsonXmlPropertiesModule());
            }
            builder.addMixIn(Object.class, NonDefaultInclusionMixIn.class);

            //--- Mapper ---

            return format.mapper.apply(builder);
        });
    }

    // A Configurable's only serializable state is its (unwrapped) configuration.
    // Suppress auto-detected getters on the implementing class so derived
    // convenience accessors (e.g. getUserAgent(), getOnMatch(), getHttpClient())
    // are not emitted as duplicate/extra properties alongside the unwrapped
    // config (which otherwise produces repeated elements that fail to read back).
    @JsonAutoDetect(
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE
    )
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
        if (polyTypesResolved.get()) {
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

        polyTypesResolved.set(true);
    }

    private void registerPolymorphicTypes(MapperBuilder<?, ?> builder) {
        resolvePolymorphicTypes();

        resolvedPolymorphicTypes.asMap().forEach((type, subTypes) -> {
            registerPolymorphicType(builder, type, subTypes);
        });
    }

    private void registerPolymorphicType(
            MapperBuilder<?, ?> builder,
            Class<?> type,
            Collection<? extends Class<?>> subTypes) {

        resolvedPolymorphicTypes.putAll(type, subTypes);

        //MAYBE: check and report those already registered?
        builder.addMixIn(type, PolymorphicMixIn.class);
        builder.registerSubtypes(subTypes.toArray(Class<?>[]::new));
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
                ValueDeserializer<?> deserializer,
                Object beanOrClass,
                String propertyName) {

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
                String failureMsg) {

            var defaultType = beanMapper.defaultPolymorphicTypes.get(
                    baseType.getRawClass());

            // No default exists, let super handle it.
            if (defaultType == null) {
                return super.handleMissingTypeId(
                        ctxt, baseType, idResolver, failureMsg);
            }

            // Default exists, return it.
            var type = ctxt.constructType(defaultType);
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
                String failureMsg) {
            if (beanMapper.canonicalNameSupportDisabled) {
                return null;
            }

            // if subTypeId is the fully qualified name of a valid sub type,
            // resolve it.
            JavaType type;
            try {
                type = TypeFactory.createDefaultInstance()
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
