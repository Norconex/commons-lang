/* Copyright 2010-2017 Norconex Inc.
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
package com.norconex.commons.lang.map;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.PropertyUtils;
import org.apache.commons.beanutils.PropertyUtilsBean;
import org.apache.commons.beanutils.SuppressPropertiesBeanIntrospector;
import org.apache.commons.io.input.ReaderInputStream;
import org.apache.commons.io.output.WriterOutputStream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.norconex.commons.lang.bean.ExtendedBeanUtilsBean;

/**
 * <p>This class is a enhanced version of {@link java.util.Properties}
 * that enforces the use of String keys and values internally, but offers many
 * convenience methods for storing and retrieving multiple values of different
 * types (e.g. Integer, Locale, File, etc). It also supports properties with
 * multiple values.  You can also see this class as a
 * string-based multi-value map with helpful methods. While it does not extend
 * {@link java.util.Properties}, it offers similar load and store
 * methods and can be used as a replacement for it in many cases.</p>
 * <p>This class extends {@link ObservableMap} which means you can listen
 * for property changes.</p>
 *
 * <p>To insert values, there are <i>set</i> methods and <i>add</i> methods.
 * The <i>set</i> methods will replace any value(s) already present under the
 * given key.  It is essentially the same behavior as
 * {@link Map#put(Object, Object)}.  The <i>add</i> method will add the
 * new value(s) to the list of already existing ones (if any).
 * </p>
 *
 * <p><b>Since 1.14.0</b>, the storing of entries with multiple values
 * will create one file entry per value. To preserve old behavior and
 * force multiple values to be on the same line, use the store/load
 * method accepting a joining delimiter. That version also
 * introduced storing and loading as JSON.
 * </p>
 *
 * <p>Upon encountering a problem in parsing a value to
 * its desired type, a {@link PropertiesException} is thrown.</p>
 * @author Pascal Essiembre
 */
public class Properties extends ObservableMap<String, List<String>>
        implements Serializable {

    public static final String DEFAULT_JAVA_PROPERTIES_DELIMITER = "\\u241E";

    //TODO rename plural methods to getXxxList()?

    //TODO have getXXXRegex() methods to return all keys or values matching
    // a regular expression

    //TODO remove support for case sensitivity and provide a utility
    //class that does it instead on any string-key maps?
    // OR, store it in a case sensitive way instead of keeping
    // multiple keys of different cases around. Could provide
    // options to put everyting lower, upper, or rely on first key
    // entered (to know which case-version to use).

    private static final long serialVersionUID = -7215126924574341L;
    private static final Logger LOG = LoggerFactory.getLogger(Properties.class);

    //TODO still support this?
    private final boolean caseInsensitiveKeys;

    private final PropertyUtilsBean propertyUtilsBean;
    private final BeanUtilsBean beanUtilsBean;

    /**
     * Create a new instance with case-sensitive keys.
     * Internally wraps a {@link HashMap} to store keys and values.
     */
    public Properties() {
        this(false);
    }

    /**
     * Creates a new instance. Internally wraps a {@link HashMap} to
     * store keys and values.
     * @param caseInsensitiveKeys when <code>true</code> methods taking a
     *        key argument will consider the key being passed without
     *        consideration for character case.
     */
    public Properties(boolean caseInsensitiveKeys) {
        this(null, caseInsensitiveKeys);
    }

    /**
     * Decorates {@code Map} as a {@code Properties}.
     * As of version <b>1.4</b> the {@code Map} argument is decorated so that
     * modifications to this instance will also modify the supplied {@code Map}.
     * To use a {@code Map} to initialize values only, use the
     * {@link #loadFromMap(Map)} method.
     * @param map the Map to decorate
     */
    public Properties(Map<String, List<String>> map) {
        this(map, false);
    }
    /**
     * Decorates a {@code Map} argument as a {@code Properties}.
     * As of version <b>1.4</b> the {@code Map} argument is decorated so that
     * modifications to this instance will also modify the supplied {@code Map}.
     * To use a {@code Map} to initialize values only, use the
     * {@link #loadFromMap(Map)} method.
     * @param map the Map to decorate
     * @param caseInsensitiveKeys when <code>true</code> methods taking a
     *        key argument will consider the key being passed without
     *        consideration for character case.
     */
    public Properties(
            Map<String, List<String>> map, boolean caseInsensitiveKeys) {
        super(map);
        this.caseInsensitiveKeys = caseInsensitiveKeys;

        //--- PropertyUtilsBean ---
        // Make sure objects needing more than "toString" are handled in
        // loadFromMap(Map<?, ?> map) {
        this.propertyUtilsBean = new PropertyUtilsBean();
        this.propertyUtilsBean.addBeanIntrospector(
                SuppressPropertiesBeanIntrospector.SUPPRESS_CLASS);
        this.beanUtilsBean = new ExtendedBeanUtilsBean();
    }

    /**
     * Gets whether keys are case sensitive or not.
     * @return <code>true</code> if case insensitive
     * @since 1.8
     */
    public boolean isCaseInsensitiveKeys() {
        return caseInsensitiveKeys;
    }

    //--- Store ----------------------------------------------------------------
    /**
     * Returns this {@link Map} in a format
     * compatible with {@link java.util.Properties#store(Writer, String)}.
     * Multi-value properties
     * are merged, joined by the symbol for record separator (U+241E).
     * @return the properties as string
     * @since 2.0.0
     */
    @Override
    public String toString() {
        try {
            StringWriter writer = new StringWriter();
            storeToProperties(writer);
            String str = writer.toString();
            writer.close();
            return str;
        } catch (IOException e) {
            throw new PropertiesException("Could not convert to string.", e);
        }
    }

    /**
     * Stores this {@link Map} in a format
     * compatible with {@link java.util.Properties#store(Writer, String)}.
     * Multi-value properties
     * are merged, joined by the symbol for record separator (U+241E).
     * @param   writer     an output character stream writer.
     * @throws IOException i/o problem
     * @since 2.0.0
     */
    public void storeToProperties(Writer writer) throws IOException {
        storeToProperties(writer, null);
    }
    /**
     * Stores this {@link Map} in a format
     * compatible with {@link java.util.Properties#store(Writer, String)}.
     * Multi-value properties
     * are merged, joined by the supplied delimiter character.
     * If the delimiter is <code>null</code>, the
     * symbol for record separator (U+241E) is used.
     * @param   writer     an output character stream writer.
     * @param delimiter string to used as a separator when joining
     *        multiple values for the same key.
     * @throws IOException i/o problem
     * @since 2.0.0
     */
    public void storeToProperties(Writer writer, String delimiter)
            throws IOException {
        storeToJavaUtilProperties(writer, delimiter, false);
    }
    /**
     * Stores this {@link Map} in a format
     * compatible with {@link java.util.Properties#store(OutputStream, String)}.
     * Multi-value properties
     * are merged, joined by the symbol for record separator (U+241E).
     * @param   out      an output stream.
     * @throws IOException i/o problem
     * @since 2.0.0
     */
    public void storeToProperties(OutputStream out) throws IOException {
        storeToProperties(out, null);
    }
    /**
     * Stores this {@link Map} in a format
     * compatible with {@link java.util.Properties#store(OutputStream, String)}.
     * Multi-value properties
     * are merged, joined by the supplied delimiter character.
     * If the delimiter is <code>null</code>, the
     * symbol for record separator (U+241E) is used.
     * @param   out      an output stream.
     * @param delimiter delimiter string to used as a separator when joining
     *        multiple values for the same key.
     * @throws IOException i/o problem
     * @since 2.0.0
     */
    public void storeToProperties(OutputStream out, String delimiter) throws IOException {
        storeToJavaUtilProperties(out, delimiter, false);
    }

    /**
     * Stores this {@link Map} in a UTF-8 format compatible with
     * {@link java.util.Properties#storeToXML(OutputStream, String)}.
     * Multi-value properties
     * are merged, joined by the symbol for record separator (U+241E).
     * @param os the output stream on which to store the XML document.
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public void storeToXML(OutputStream os) throws IOException {
        storeToXML(os, null);
    }

    /**
     * Stores this {@link Map} in a UTF-8 format compatible with
     * {@link java.util.Properties#storeToXML(OutputStream, String)}.
     * Multi-value properties
     * are merged, joined by the supplied delimiter character.
     * If the delimiter is <code>null</code>, the
     * symbol for record separator (U+241E) is used.
     * @param os the output stream on which to store the XML document.
     * @param delimiter delimiter string to used as a separator when joining
     *        multiple values for the same key.
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public void storeToXML(
            OutputStream os, String delimiter) throws IOException {
        storeToJavaUtilProperties(os, delimiter, true);
    }

    /**
     * Stores this {@link Map} in a UTF-8 format compatible with
     * {@link java.util.Properties#storeToXML(OutputStream, String)}.
     * Multi-value properties
     * are merged, joined by the symbol for record separator (U+241E).
     * @param writer writer on which to store the XML document.
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public void storeToXML(Writer writer) throws IOException {
        storeToXML(writer, null);
    }

    /**
     * Stores this {@link Map} in a UTF-8 format compatible with
     * {@link java.util.Properties#storeToXML(OutputStream, String)}.
     * Multi-value properties
     * are merged, joined by the supplied delimiter character.
     * If the delimiter is <code>null</code>, the
     * symbol for record separator (U+241E) is used.
     * @param writer the writer on which to store the XML document.
     * @param delimiter delimiter string to used as a separator when joining
     *        multiple values for the same key.
     * @throws IOException i/o problem
     */
    public void storeToXML(Writer writer, String delimiter)
            throws IOException {
        storeToJavaUtilProperties(writer, delimiter, true);
    }


    private synchronized void storeToJavaUtilProperties(
            Object output, String delimiter, boolean isXML) throws IOException {
        java.util.Properties p = new java.util.Properties();
        String sep = StringUtils.defaultIfEmpty(
                delimiter, DEFAULT_JAVA_PROPERTIES_DELIMITER);
        for (Entry<String, List<String>> entry : entrySet()) {
            p.setProperty(
                    entry.getKey(),
                    StringUtils.join(entry.getValue(), sep));
        }
        if (output instanceof Writer) {
            if (isXML) {
                p.storeToXML(new WriterOutputStream((Writer) output,
                        StandardCharsets.UTF_8), "");
            } else {
                p.store((Writer) output, "");
            }
        } else {
            if (isXML) {
                p.storeToXML((OutputStream) output, "");
            } else {
                p.store((OutputStream) output, "");
            }
        }
    }

    /**
     * Writes this {@link Map} as JSON to the output stream as UTF-8 in a format
     * suitable for using the {@link #loadFromJSON(InputStream)} method.
     * @param os the output stream on which to store the properties.
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public void storeToJSON(OutputStream os) throws IOException {
        storeToJSON(new OutputStreamWriter(os, StandardCharsets.UTF_8));
    }
    /**
     * Writes this {@link Map} as JSON to the writer in a format
     * suitable for using the
     * {@link #loadFromJSON(Reader)} method.
     * @param writer the writer on which to store the XML document.
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public void storeToJSON(Writer writer) throws IOException {
        writer.write('{');
        boolean keyFirst = true;
        for (Entry<String, List<String>> entry : entrySet()) {
            if (!keyFirst) {
                writer.write(',');
            }
            writer.write('"');
            writer.write(StringEscapeUtils.escapeJson(entry.getKey()));
            writer.write("\":[");
            boolean valueFirst = true;
            for (String value : entry.getValue()) {
                if (value == null) {
                    continue;
                }
                if (!valueFirst) {
                    writer.write(',');
                }
                writer.write('"');
                writer.write(StringEscapeUtils.escapeJson(value));
                writer.write('"');
                valueFirst = false;
            }
            writer.write("]");
            keyFirst = false;
        }
        writer.write('}');
        writer.flush();
    }

    /**
     * Copy all properties in this map to the given bean, mapping keys
     * to setter methods of the same name. Existing bean values for matching
     * accessors are overwritten. Other values are left intact.
     * <code>null</code> beans are ignored.
     * @param bean the object to store properties into
     * @since 2.0.0
     */
    public void storeToBean(Object bean) {
        if (bean == null) {
            return;
        }
        try {
            for (Entry<String, List<String>> it : entrySet()) {
                String property = it.getKey();
                List<String> values = it.getValue();
                if (property == null || values.isEmpty()
                        || !PropertyUtils.isWriteable(bean, property)) {
                    LOG.debug("Property is not writable (no setter?): {}",
                            property);
                    continue;
                }

                Class<?> type = PropertyUtils.getPropertyType(bean, property);
                Object value = values;

                if (!type.isArray() && !Iterable.class.isAssignableFrom(type)) {
                    value = values.get(0);
                }

                beanUtilsBean.copyProperty(bean, property, value);
            }
        } catch (IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new PropertiesException(
                    "Could not store Properties into given bean.", e);
        }
    }

    //--- Load -----------------------------------------------------------------

    /**
     * Reads a property list (key and element pairs) from the input
     * string.  Otherwise, the same considerations as
     * {@link #loadFromProperties(InputStream)} apply.
     * @param str the string to load
     * @throws IOException problem loading string
     */
    public void fromString(String str) throws IOException {
        Reader r = new StringReader(str);
        loadFromProperties(r);
        r.close();
    }

    /**
     * <p>Reads all key/value pairs in the given map, and
     * add them to this <code>Map</code>.  Keys and values are converted to
     * strings using their toString() method, with exception
     * of values being arrays or collections.  In such case, the entry
     * is considered a multi-value one and each value will be converted
     * to individual strings. <code>null</code> keys are ignored.
     * <code>null</code> values are converted to an empty string.</p>
     * <p>Changes to this instance
     * won't be reflected in the given <code>Map</code>.  If you want otherwise,
     * use invoke the constructor with a <code>Map</code> argument.</p>
     * @param map the map containing values to load
     * @since 2.0.0
     */
    public synchronized void loadFromMap(Map<?, ?> map) {
        if (map != null) {
            for (Entry<?, ?> entry : map.entrySet()) {
                Object keyObj = entry.getKey();
                if (keyObj == null) {
                    continue;
                }
                String key = toString(keyObj);
                Object valObj = entry.getValue();
                if (valObj == null) {
                    valObj = StringUtils.EMPTY;
                }
                Iterable<?> it = null;
                if (valObj.getClass().isArray()) {
                    if(valObj.getClass().getComponentType().isPrimitive()) {
                        List<Object> objs = new ArrayList<>();
                        for (int i = 0; i < Array.getLength(valObj); i++) {
                            objs.add(Array.get(valObj, i));
                        }
                        it = objs;
                    } else {
                        it = Arrays.asList((Object[]) valObj);
                    }
                } else if (valObj instanceof Iterable) {
                    it = (Iterable<?>) valObj;
                }
                if (it == null) {
                    addString(key, toString(valObj));
                } else {
                    for (Object val : it) {
                        addString(key, toString(val));
                    }
                }
            }
        }
    }

    /**
     * Loads this {@link Map} from an input of a format
     * compatible with {@link java.util.Properties#load(Reader)}.
     * Multi-value properties
     * are split using the symbol for record separator (U+241E).
     * @param   reader   the input character stream.
     * @throws IOException i/o problem
     * @since 2.0.0
     */
    public void loadFromProperties(Reader reader) throws IOException {
        loadFromProperties(reader, null);
    }
    /**
     * Loads this {@link Map} from an input of a format
     * compatible with {@link java.util.Properties#load(Reader)}.
     * Multi-value properties
     * are split using the supplied delimiter string.
     * If the delimiter is <code>null</code>, the symbol for record separator
     * (U+241E) is used.
     * @param   reader   the input character stream.
     * @param delimiter delimiter string to used to parse a multi-value key.
     * @throws IOException i/o problem
     * @since 2.0.0
     */
    public void loadFromProperties(Reader reader, String delimiter)
            throws IOException {
        loadFromJavaUtilProperties(reader, delimiter, false);
    }

    /**
     * Loads this {@link Map} from an input of a format
     * compatible with {@link java.util.Properties#load(InputStream)}.
     * Multi-value properties
     * are split using the symbol for record separator (U+241E).
     * @param   inStream   the input stream.
     * @throws IOException i/o problem
     * @since 2.0.0
     */
    public synchronized void loadFromProperties(
            InputStream inStream) throws IOException {
        loadFromProperties(inStream, null);
    }
    /**
     * Loads this {@link Map} from an input of a format
     * compatible with {@link java.util.Properties#load(InputStream)}.
     * Multi-value properties
     * are split using the supplied delimiter string.
     * If the delimiter is <code>null</code>, the symbol for record separator
     * (U+241E) is used.
     * @param   inStream   the input stream.
     * @param delimiter delimiter string to used to parse a multi value
     *        key.
     * @throws IOException i/o problem
     * @since 2.0.0
     */
    public synchronized void loadFromProperties(
            InputStream inStream, String delimiter) throws IOException {
        loadFromJavaUtilProperties(inStream, delimiter, false);
    }

    /**
     * Loads this {@link Map} from an input of a format
     * compatible with {@link java.util.Properties#loadFromXML(InputStream)}.
     * Multi-value properties
     * are split using the symbol for record separator (U+241E).
     * @param in in the input stream from which to read the XML document.
     * @throws IOException i/o problem
     */
    public void loadFromXML(InputStream in) throws IOException {
        loadFromXML(in, null);
    }
    /**
     * Loads this {@link Map} from an input of a format
     * compatible with {@link java.util.Properties#load(InputStream)}.
     * Multi-value properties
     * are split using the supplied delimiter string.
     * If the delimiter is <code>null</code>, the symbol for record separator
     * (U+241E) is used.
     * @param in in the input stream from which to read the XML document.
     * @param delimiter delimiter string to used to parse a multi-value key.
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public void loadFromXML(InputStream in, String delimiter)
            throws IOException {
        loadFromJavaUtilProperties(in, delimiter, true);
    }

    /**
     * Loads this {@link Map} from an input of a format
     * compatible with {@link java.util.Properties#loadFromXML(InputStream)}.
     * Multi-value properties
     * are split using the symbol for record separator (U+241E).
     * @param reader the reader from which to read the XML document.
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public void loadFromXML(Reader reader) throws IOException {
        loadFromXML(reader, null);
    }
    /**
    /**
     * Loads this {@link Map} from an input of a format
     * compatible with {@link java.util.Properties#load(InputStream)}.
     * Multi-value properties
     * are split using the supplied delimiter string.
     * If the delimiter is <code>null</code>, the symbol for record separator
     * (U+241E) is used.
     * @param reader reader from which to read the XML document.
     * @param delimiter delimiter string to used to parse a multi-value key.
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public void loadFromXML(Reader reader, String delimiter)
            throws IOException {
        loadFromJavaUtilProperties(reader, delimiter, true);
    }

    // input is Reader or InputStream
    private synchronized void loadFromJavaUtilProperties(
            Object input, String delimiter, boolean isXML) throws IOException {

        java.util.Properties p = new java.util.Properties();
        String sep = StringUtils.defaultIfEmpty(
                delimiter, DEFAULT_JAVA_PROPERTIES_DELIMITER);

        if (input instanceof Reader) {
            if (isXML) {
                p.loadFromXML(new ReaderInputStream(
                        (Reader) input, StandardCharsets.UTF_8));
            } else {
                p.load((Reader) input);
            }
        } else {
            if (isXML) {
                p.loadFromXML((InputStream) input);
            } else {
                p.load((InputStream) input);
            }
        }

        for (Entry<Object, Object> entry : p.entrySet()) {
            if (entry.getKey() != null) {
                addString(entry.getKey().toString(),
                        StringUtils.splitByWholeSeparator(
                                Objects.toString(entry.getValue(), null), sep));
            }
        }
    }

    /**
     * Loads all of the properties from the JSON document input stream
     * (UTF-8) into this instance.
     * @param in the input stream from which to read the JSON document.
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public void loadFromJSON(InputStream in) throws IOException {
        if (in == null) {
            return;
        }
        loadFromJSON(new InputStreamReader(in, "UTF-8"));
    }
    /**
     * Loads all of the properties from the JSON document reader
     * into this instance.
     * @param reader the reader from which to read the JSON document.
     * @throws IOException i/o problem
     * @since 1.14.0
     */
    public void loadFromJSON(Reader reader) throws IOException {
        if (reader == null) {
            return;
        }
        JSONObject json = new JSONObject(new JSONTokener(reader));
        Iterator<String> it = json.keys();
        while (it.hasNext()) {
            String key = it.next();
            JSONArray array = json.getJSONArray(key);
            for (int i = 0; i < array.length(); i++) {
                String val = array.getString(i);
                addString(key, val);
            }
        }
    }

    /**
     * Converts all the bean properties into entries in this instance.
     * <code>null</code> beans are ignored.
     * @param bean the object to load properties from
     * @since 2.0.0
     */
    public void loadFromBean(Object bean) {
        if (bean == null) {
            return;
        }
        try {
            loadFromMap(propertyUtilsBean.describe(bean));
        } catch (IllegalAccessException | InvocationTargetException
                | NoSuchMethodException e) {
            throw new PropertiesException(
                    "Could not load Properties from given bean.", e);
        }
    }

    //--- String ---------------------------------------------------------------
    /**
     * Gets value as string.
     * @param key property key
     * @return the value
     */
    public final String getString(String key) {
        List<String> list = get(key);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
    /**
     * Gets value as string.
     * @param key property key
     * @param defaultValue default value to return when original value is null.
     * @return the value
     */
    public final String getString(String key, String defaultValue) {
        return ObjectUtils.defaultIfNull(getString(key), defaultValue);
    }
    /**
     * Gets values as a list of strings. This method is null-safe. No matches
     * returns an empty list.
     * @param key property key
     * @return the values
     */
    public final List<String> getStrings(String key) {
        List<String> values = get(key);
        if (values == null) {
            return new ArrayList<>();
        }
        return new ArrayList<>(values);
    }
    /**
     * Sets one or multiple string values replacing existing ones.
     * Setting a single <code>null</code> value or an empty string array is
     * the same as calling {@link #remove(Object)} with the same key.
     * When setting multiple values, <code>null</code> values are converted
     * to blank strings.
     * @param key the key of the value to set
     * @param values the values to set
     */
    public final void setString(String key, String... values) {
        if (ArrayUtils.isEmpty(values)) {
            remove(key);
        }
        put(key, new ArrayList<>(Arrays.asList(values)));
    }
    /**
     * Adds one or multiple string values.
     * Adding a single <code>null</code> value has no effect.
     * When adding multiple values, <code>null</code> values are converted
     * to blank strings.
     * @param key the key of the value to set
     * @param values the values to set
     */
    public final void addString(String key, String... values) {
        if (ArrayUtils.isEmpty(values)) {
            return;
        }
        List<String> list = get(key);
        if (list == null) {
            list = new ArrayList<>();
        }
        list.addAll(Arrays.asList(values));
        put(key, list);
    }

    //--- Integer --------------------------------------------------------------
    /**
     * Gets value as an integer.
     * @param key property key
     * @return the value
     */
    public final int getInt(String key) {
        try {
            return Integer.parseInt(getString(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse integer value.", key, getString(key), e);
        }
    }
    /**
     * Gets value as an integer.
     * @param key property key
     * @param defaultValue default value to return when original value is null.
     * @return the value
     */
    public final int getInt(String key, int defaultValue) {
        String value = getString(key, Integer.toString(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse integer value.", key, value, e);
        }
    }
    /**
     * Gets values as a list of integers.
     * @param key property key
     * @return the values
     */
    public final List<Integer> getInts(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Integer> ints = new ArrayList<>(values.size());
            for (String value : values) {
                errVal = value;
                ints.add(Integer.parseInt(value));
            }
            return ints;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse integer value.", key, errVal, e);
        }
    }
    /**
     * Sets one or multiple integer values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void setInt(String key, int... values) {
        setString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    /**
     * Adds one or multiple integer values values.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void addInt(String key, int... values) {
        addString(key, toStringArray(ArrayUtils.toObject(values)));
    }

    //--- Double ---------------------------------------------------------------
    /**
     * Gets value as a double.
     * @param key property key
     * @return the value
     */
    public final double getDouble(String key) {
        try {
            return Double.parseDouble(getString(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse double value.", key, getString(key), e);
        }
    }
    /**
     * Gets value as a double.
     * @param key property key
     * @param defaultValue default value to return when original value is null.
     * @return the value
     */
    public final double getDouble(String key, double defaultValue) {
        String value = getString(key, Double.toString(defaultValue));
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse double value.", key, value, e);
        }
    }
    /**
     * Gets values as a list of doubles.
     * @param key property key
     * @return the values
     */
    public final List<Double> getDoubles(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Double> list = new ArrayList<>(values.size());
            for (String value : values) {
                errVal = value;
                list.add(Double.parseDouble(value));
            }
            return list;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse double value.", key, errVal, e);
        }
    }
    /**
     * Sets one or multiple double values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void setDouble(String key, double... values) {
        setString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    /**
     * Adds one or multiple double values.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void addDouble(String key, double... values) {
        addString(key, toStringArray(ArrayUtils.toObject(values)));
    }

    //--- Long -----------------------------------------------------------------
    /**
     * Gets value as a long.
     * @param key property key
     * @return the value
     */
    public final long getLong(String key) {
        try {
            return Long.parseLong(getString(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse long value.", key, getString(key), e);
        }
    }
    /**
     * Gets value as a long.
     * @param key property key
     * @param defaultValue default value to return when original value is null.
     * @return the value
     */
    public final long getLong(String key, long defaultValue) {
        String value = getString(key, Long.toString(defaultValue));
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse long value.", key, value, e);
        }
    }
    /**
     * Gets values as a list of longs.
     * @param key property key
     * @return the values
     */
    public final List<Long> getLongs(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Long> list = new ArrayList<>(values.size());
            for (String value : values) {
                errVal = value;
                list.add(Long.parseLong(value));
            }
            return list;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse long value.", key, errVal, e);
        }
    }
    /**
     * Sets one or multiple long values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void setLong(String key, long... values) {
        setString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    /**
     * Add one or multiple long values.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void addLong(String key, long... values) {
        addString(key, toStringArray(ArrayUtils.toObject(values)));
    }

    //--- Float ----------------------------------------------------------------
    /**
     * Gets value as a float.
     * @param key property key
     * @return the value
     */
    public final float getFloat(String key) {
        try {
            return Float.parseFloat(getString(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse float value.", key, getString(key), e);
        }
    }
    /**
     * Gets value as a float.
     * @param key property key
     * @param defaultValue default value to return when original value is null.
     * @return the value
     */
    public final float getFloat(String key, float defaultValue) {
        String value = getString(key, Float.toString(defaultValue));
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse float value.", key, value, e);
        }
    }
    /**
     * Gets values as a list of floats.
     * @param key property key
     * @return the values
     */
    public final List<Float> getFloats(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Float> list = new ArrayList<>(values.size());
            for (String value : values) {
                errVal = value;
                list.add(Float.parseFloat(value));
            }
            return list;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse float value.", key, errVal, e);
        }
    }
    /**
     * Sets one or multiple float values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void setFloat(String key, float... values) {
        setString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    /**
     * Adds one or multiple long values.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void addFloat(String key, float... values) {
        addString(key, toStringArray(ArrayUtils.toObject(values)));
    }

    //--- BigDecimal -----------------------------------------------------------
    /**
     * Gets value as a BigDecimal.
     * @param key property key
     * @return the value
     */
    public final BigDecimal getBigDecimal(String key) {
        String value = getString(key);
        if (value == null || value.trim().length() == 0) {
            return null;
        }
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse BigDecimal value.", key, value, e);
        }
    }
    /**
     * Gets value as a BigDecimal.
     * @param key property key
     * @param defaultValue default value to return when original value is null.
     * @return the value
     */
    public final BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        return ObjectUtils.defaultIfNull(getBigDecimal(key), defaultValue);
    }
    /**
     * Gets values as a list of BigDecimals.
     * @param key property key
     * @return the values
     */
    public final List<BigDecimal> getBigDecimals(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<BigDecimal> list = new ArrayList<>(values.size());
            for (String value : values) {
                errVal = value;
                list.add(new BigDecimal(value));
            }
            return list;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse BigDecimal value.", key, errVal, e);
        }
    }
    /**
     * Sets one or multiple BigDecimal values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void setBigDecimal(String key, BigDecimal... values) {
        setString(key, toStringArray(values));
    }
    /**
     * Add one or multiple BigDecimal values.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void addBigDecimal(String key, BigDecimal... values) {
        addString(key, toStringArray(values));
    }

    //--- LocalDateTime --------------------------------------------------------
    /**
     * Gets value as a local date-time. The date must be a valid date-time
     * as defined by {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
     * @param key property key
     * @return the value
     * @since 2.0.0
     */
    public final LocalDateTime getLocalDateTime(String key) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return LocalDateTime.parse(value);
        } catch (DateTimeParseException e) {
            throw createTypedException(
                    "Could not parse LocalDateTime value.", key, value, e);
        }
    }
    /**
     * Gets value as a local date-time. The date must be a valid date-time
     * as defined by {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
     * @param key property key
     * @param defaultValue default value to return when original value is null.
     * @return the value
     * @since 2.0.0
     */
    public final LocalDateTime getLocalDateTime(
            String key, LocalDateTime defaultValue) {
        return ObjectUtils.defaultIfNull(getLocalDateTime(key), defaultValue);
    }
    /**
     * Gets values as a list of local date-times. Each date must be a valid
     * date-time as defined by {@link DateTimeFormatter#ISO_LOCAL_DATE_TIME}.
     * @param key property key
     * @return the values
     * @since 2.0.0
     */
    public final List<LocalDateTime> getLocalDateTimes(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<LocalDateTime> list = new ArrayList<>(values.size());
            for (String value : values) {
                errVal = value;
                list.add(LocalDateTime.parse(value));
            }
            return list;
        } catch (DateTimeParseException e) {
            throw createTypedException(
                    "Could not parse LocalDateTime value.", key, errVal, e);
        }
    }
    /**
     * Sets one or multiple local date-time values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     * @since 2.0.0
     */
    public final void setLocalDateTime(String key, LocalDateTime... values) {
        setString(key, localDateTimesToStringArray(values));
    }
    /**
     * Add one or multiple local date-time values.
     * @param key the key of the values to set
     * @param values the values to set
     * @since 2.0.0
     */
    public final void addLocalDateTime(String key, LocalDateTime... values) {
        addString(key, localDateTimesToStringArray(values));
    }
    private String[] localDateTimesToStringArray(LocalDateTime... values) {
        if (values == null) {
            return null;
        }
        String[] array = new String[values.length];
        for (int i = 0; i < array.length; i++) {
            LocalDateTime value = values[i];
            if (value == null) {
                array[i] = null;
            } else {
                array[i] = value.toString();
            }
        }
        return array;
    }

    //--- Date -----------------------------------------------------------------
    /**
     * Gets value as a date.
     * @param key property key
     * @return the value
     */
    public final Date getDate(String key) {
        String value = getString(key);
        if (StringUtils.isBlank(value)) {
            return null;
        }
        try {
            return new Date(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse Date value.", key, value, e);
        }
    }
    /**
     * Gets value as a date.
     * @param key property key
     * @param defaultValue default value to return when original value is null.
     * @return the value
     */
    public final Date getDate(String key, Date defaultValue) {
        return ObjectUtils.defaultIfNull(getDate(key), defaultValue);
    }
    /**
     * Gets values as a list of dates.
     * @param key property key
     * @return the values
     */
    public final List<Date> getDates(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Date> list = new ArrayList<>(values.size());
            for (String value : values) {
                errVal = value;
                list.add(new Date(Long.parseLong(value)));
            }
            return list;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse Date value.", key, errVal, e);
        }
    }
    /**
     * Sets one or multiple date values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void setDate(String key, Date... values) {
        setString(key, datesToStringArray(values));
    }
    /**
     * Add one or multiple date values.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void addDate(String key, Date... values) {
        addString(key, datesToStringArray(values));
    }
    private String[] datesToStringArray(Date... values) {
        if (values == null) {
            return null;
        }
        String[] array = new String[values.length];
        for (int i = 0; i < array.length; i++) {
            Date value = values[i];
            if (value == null) {
                array[i] = null;
            } else {
                array[i] = Long.toString(value.getTime());
            }
        }
        return array;
    }

    //--- Boolean --------------------------------------------------------------
    /**
     * Gets value as a boolean. The underlying string value matching
     * the key must exist and equal "true" (ignoring case) to return
     * <code>true</code>.  Any other value (including <code>null</code>)
     * will return <code>false</code>.
     * @param key property key
     * @return the value
     */
    public final boolean getBoolean(String key) {
        return Boolean.parseBoolean(getString(key));
    }
    /**
     * Gets value as a boolean. The underlying string value matching
     * the key must exist and equal "true" (ignoring case) to return
     * <code>true</code>.  Any other value (including <code>null</code>)
     * will return <code>false</code>.  If there are no entries for the given
     * key, the default value is returned instead.
     * @param key property key
     * @param defaultValue default value to return when original value is null.
     * @return the value
     */
    public final boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.parseBoolean(getString(key, Boolean.toString(defaultValue)));
    }
    /**
     * Gets values as a list of booleans.
     * @param key property key
     * @return the values
     */
    public final List<Boolean> getBooleans(String key) {
        List<String> values = getStrings(key);
        List<Boolean> list = new ArrayList<>(values.size());
        for (String value : values) {
            list.add(Boolean.parseBoolean(value));
        }
        return list;
    }
    /**
     * Sets one or multiple boolean values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void setBoolean(String key, boolean... values) {
        setString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    /**
     * Adds one or multiple boolean values.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void addBoolean(String key, boolean... values) {
        addString(key, toStringArray(ArrayUtils.toObject(values)));
    }

    //--- Locale ---------------------------------------------------------------
    /**
     * Gets value as a locale.
     * @param key property key
     * @return the value
     */
    public final Locale getLocale(String key) {
        try {
            return LocaleUtils.toLocale(getString(key));
        } catch (IllegalArgumentException e) {
            throw createTypedException(
                    "Could not parse Locale value.", key, getString(key), e);
        }
    }
    /**
     * Gets value as a locale.
     * @param key property key
     * @param defaultValue default value to return when original value is null.
     * @return the value
     */
    public final Locale getLocale(String key, Locale defaultValue) {
        try {
            return LocaleUtils.toLocale(getString(key));
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
    /**
     * Gets values as a list of locales.
     * @param key property key
     * @return the values
     */
    public final List<Locale> getLocales(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Locale> list = new ArrayList<>(values.size());
            for (String value : values) {
                errVal = value;
                list.add(LocaleUtils.toLocale(value));
            }
            return list;
        } catch (IllegalArgumentException  e) {
            throw createTypedException(
                    "Could not parse locale value.", key, errVal, e);
        }
    }
    /**
     * Sets one or multiple locale values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void setLocale(String key, Locale... values) {
        setString(key, toStringArray(values));
    }
    /**
     * Adds one or multiple locale values.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void addLocale(String key, Locale... values) {
        addString(key, toStringArray(values));
    }

    //--- File -----------------------------------------------------------------
    /**
     * Gets a file, assuming key value is a file system path.
     * @param key properties key
     * @return a File
     */
    public final File getFile(String key) {
        String filePath = getString(key);
        if (filePath == null) {
            return null;
        }
    	return new File(filePath);
    }
    /**
     * Gets a file, assuming key value is a file system path.
     * @param key properties key
     * @param defaultValue default file being returned if no file has been
     *        defined for the given key in the properties.
     * @return a File
     */
    public final File getFile(String key, File defaultValue) {
        return ObjectUtils.defaultIfNull(getFile(key), defaultValue);
    }
    /**
     * Gets values as a list of files.
     * @param key property key
     * @return the values
     */
    public final List<File> getFiles(String key) {
        List<String> values = getStrings(key);
        List<File> list = new ArrayList<>(values.size());
        for (String value : values) {
            list.add(new File(value));
        }
        return list;
    }
    /**
     * Sets one or multiple file values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void setFile(String key, File... values) {
        setString(key, filesToStringArray(values));
    }
    /**
     * Adds one or multiple file values.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void addFile(String key, File... values) {
        addString(key, filesToStringArray(values));
    }
    private String[] filesToStringArray(File... values) {
        if (values == null) {
            return null;
        }
        String[] array = new String[values.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = values[i].getPath();
        }
        return array;
    }

    //--- Class ----------------------------------------------------------------
    /**
     * Gets a class, assuming key value is a fully qualified class name
     * available in the classloader.
     * @param key properties key
     * @return initialized class
     */
    public final Class<?> getClass(String key) {
    	String value = getString(key);
    	try {
			return Class.forName(value);
		} catch (ClassNotFoundException e) {
            throw createTypedException(
                    "Could not parse class value.", key, value, e);
		}
    }
    /**
     * Gets a class, assuming key value is a fully qualified class name
     * available in the classloader.
     * @param key properties key
     * @param defaultValue default file being returned if no class has been
     *        defined for the given key in the properties.
     * @return initialized class
     */
    public final Class<?> getClass(String key, Class<?> defaultValue) {
        return ObjectUtils.defaultIfNull(getClass(key), defaultValue);
    }
    /**
     * Gets values as a list of initialized classes.
     * @param key property key
     * @return the values
     */
    public final List<Class<?>> getClasses(String key) {
        List<String> values = getStrings(key);
        List<Class<?>> list = new ArrayList<>(values.size());
        for (String value : values) {
            list.add(getClass(value));
        }
        return list;
    }
    /**
     * Sets one or multiple class values, replacing existing ones.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void setClass(String key, Class<?>... values) {
        setString(key, classesToStringArray(values));
    }
    /**
     * Adds one or multiple class values.
     * @param key the key of the values to set
     * @param values the values to set
     */
    public final void addClass(String key, Class<?>... values) {
        addString(key, classesToStringArray(values));
    }
    private String[] classesToStringArray(Class<?>... values) {
        if (values == null) {
            return null;
        }
        String[] array = new String[values.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = values[i].getName();
        }
        return array;
    }

    //--- Other ----------------------------------------------------------------
    @Override
    public final List<String> get(Object key) {
        return super.get(caseResolvedKey(key));
    }

    @Override
    public final List<String> remove(Object key) {
        return super.remove(caseResolvedKey(key));
    }

    /*
     * Puts the list of values to the given key. Supplying a <code>null</code>
     * list has the same effect as calling {@link #remove(Object)} with
     * the same key.
     */
    @Override
    public List<String> put(String key, List<String> values) {
        if (values == null) {
            return remove(key);
        }
        List<String> nullSafeValues = new ArrayList<>(values.size());
        for (String value : values) {
            if (value == null) {
                nullSafeValues.add(StringUtils.EMPTY);
            } else {
                nullSafeValues.add(value);
            }
        }
        return super.put(caseResolvedKey(key), nullSafeValues);
    }

    /*
     * When case insensitive, values of equal keys are merged into the first
     * key encountered.
     */
    @Override
    public void putAll(Map<? extends String, ? extends List<String>> m) {
        if (!caseInsensitiveKeys) {
            super.putAll(m);
            return;
        }

        if (m == null) {
            return;
        }
        for (Entry<? extends String, ? extends List<String>> entry :
            m.entrySet()) {
            String key = entry.getKey();
            List<String> values = entry.getValue();
            if (values != null) {
                addString(key, values.toArray(ArrayUtils.EMPTY_STRING_ARRAY));
            }
        }
    }

    //--- Privates -------------------------------------------------------------
    // If a key already exists under a different case, return it, else
    // return the one passed as argument
    private String caseResolvedKey(Object key) {
        String resolvedKey = Objects.toString(key, null);
        if (!isCaseInsensitiveKeys()) {
            return resolvedKey;
        }
        for (String existingKey : super.keySet()) {
            if (StringUtils.equalsIgnoreCase(existingKey, resolvedKey)) {
                resolvedKey = existingKey;
                break;
            }
        }
        return resolvedKey;
    }

    private PropertiesException createTypedException(
            String msg, String key, String value, Exception cause) {
        String message = msg + " [key=" + key + "; value=" + value + "].";
        LOG.error(message, cause);
        return new PropertiesException(message, cause);
    }
    private String[] toStringArray(Object[] array) {
        if (array == null) {
            return null;
        }
        String[] strArray = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            strArray[i] = Objects.toString(array[i], StringUtils.EMPTY);
            //TODO ------------------------------------- REALLY? ^^^^^
        }
        return strArray;
    }
    // TODO consider calling this from toStringArray(array) and have
    // Class, Date, etc, setters call that method. But check
    // implications of EMPTY vs null. (why have empty for elements in in array,
    // and null otherwise?) Should always be null or not kept in array.
    private String toString(Object obj) {
        if (obj == null) {
            return null;
        }
        if (obj instanceof Class) {
            return ((Class<?>) obj).getName();
        }
        if (obj instanceof Date) {
            return Long.toString(((Date) obj).getTime());
        }
        if (obj instanceof File) {
            return ((File) obj).getPath();
        }
        return obj.toString();
    }
}
