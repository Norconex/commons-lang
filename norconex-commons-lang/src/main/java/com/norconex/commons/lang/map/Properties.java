/* Copyright 2010-2016 Norconex Inc.
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.CharEncoding;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * <p>This class is a enhanced version of {@link java.util.Properties}
 * that enforces the use of String keys and values internally, but offers many
 * convenience methods for storing and retrieving multiple values of different
 * types (e.g. Integer, Locale, File, etc). You can also see it as a 
 * string-based multi-value map with helpful methods. While it does not extend 
 * {@link java.util.Properties}, it offers similar load and store
 * methods and can be used as a replacement for it in many cases.</p>
 * 
 * <p>As of <b>1.4</b>, this class no longer extends {@code TreeMap}.
 * It now extends {@link ObservableMap} which means you can listen
 * for map changes.</p>
 * 
 * <p>To insert values, there are <i>set</i> methods and <i>add</i> methods.
 * The <i>set</i> methods will replace any value(s) already present under the 
 * given key.  It is essentially the same behavior as 
 * {@link Map#put(Object, Object)}.  The <i>add</i> method will add the 
 * new value(s) to the list of already existing ones (if any).
 * </p>
 * 
 * <p>Upon encountering a problem in parsing the
 * data to its target format, a {@link PropertiesException} is thrown.</p>
 * @author Pascal Essiembre
 */
@SuppressWarnings("nls")
public class Properties extends ObservableMap<String, List<String>>
        implements Serializable {

    //TODO remove support for case sensitivity and provide a utility
    //class that does it instead on any string-key maps? 
    // OR, store it in a case sensitive way instead of keeping
    // multiple keys of different cases around. Could provide
    // options to put everyting lower, upper, or rely on first key
    // entered (to know which case-version to use).
    
    private static final long serialVersionUID = -7215126924574341L;
    private static final Logger LOG = LogManager.getLogger(Properties.class);

    /**
     * Default delimiter when storing/loading multi-values to/from 
     * <code>.properties</code> files.
     */
    public static final String DEFAULT_MULTIVALUE_DELIMITER = "^|~";
    
    private final boolean caseInsensitiveKeys;
    private String multiValueDelimiter = DEFAULT_MULTIVALUE_DELIMITER;
    
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
     * {@link #load(Map)} method.
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
     * {@link #load(Map)} method.
     * @param map the Map to decorate 
     * @param caseInsensitiveKeys when <code>true</code> methods taking a 
     *        key argument will consider the key being passed without 
     *        consideration for character case.
     */
    public Properties(
            Map<String, List<String>> map, boolean caseInsensitiveKeys) {
        super(map);
        this.caseInsensitiveKeys = caseInsensitiveKeys;
    }

    /**
     * Gets whether keys are case sensitive or not.
     * @return <code>true</code> if case sensitive
     * @since 1.4
     * @deprecated Since 1.8.0, use {@link #isCaseInsensitiveKeys()}
     */
    @Deprecated
    public boolean isCaseSensitiveKeys() {
        return !caseInsensitiveKeys;
    }
    /**
     * Gets whether keys are case sensitive or not.
     * @return <code>true</code> if case insensitive
     * @since 1.8
     */
    public boolean isCaseInsensitiveKeys() {
        return caseInsensitiveKeys;
    }

    /**
     * Gets multiple value string delimiter.
     * @return multiple value string delimiter
     * @since 1.4
     */
    public String getMultiValueDelimiter() {
        return multiValueDelimiter;
    }
    /**
     * Sets multiple value string delimiter.
     * @param multiValueDelimiter multiple value string delimiter
     * @since 1.4
     */
    public void setMultiValueDelimiter(String multiValueDelimiter) {
        this.multiValueDelimiter = multiValueDelimiter;
    }

    //--- Store ----------------------------------------------------------------
    /**
     * Writes this property list (key and element pairs) in this
     * <code>Properties</code> table to the output stream as UTF-8 in a format 
     * suitable for loading into a <code>Properties</code> table using the
     * {@link #load(InputStream) load} method.
     * Otherwise, the same considerations as
     * {@link #store(OutputStream, String)} apply.
     * @param   comments   a description of the property list.
     * @return the properties as string
     * @throws IOException problem storing to string
     */
    public String storeToString(String comments) throws IOException {
        StringWriter writer = new StringWriter();
        store(writer, comments);
        String str = writer.toString();
        writer.close();
        return str;
    }
    /**
     * Writes this {@link Map} (key and element pairs) to the output character
     * stream in a format suitable for using the 
     * {@link #load(Reader)} method. 
     * If a key only has one value, then this method behavior is the
     * exact same as the {@link Properties#store(Writer, String)} method.
     * Keys with multi-values are joined into a single string, using
     * the default delimiter:
     * {@link Properties#DEFAULT_MULTIVALUE_DELIMITER}
     * @param   writer     an output character stream writer.
     * @param   comments   a description of the property list.
     * @throws IOException i/o problem
     * @see Properties#store(Writer, String)
     */
    public void store(Writer writer, String comments) throws IOException {
        store(writer, comments, DEFAULT_MULTIVALUE_DELIMITER);
    }
    /**
     * Writes this {@link Map} (key and element pairs) to the output character
     * stream in a format suitable for using the 
     * {@link #load(Reader, String)} method. 
     * If a key only has one value, then this method behavior is the
     * exact same as the {@link Properties#store(Writer, String)} method.
     * Keys with multi-values are joined into a single string, using
     * the delimiter provided.
     * @param   writer     an output character stream writer.
     * @param   comments   a description of the property list.
     * @param delimiter string to used as a separator when joining 
     *        multiple values for the same key.
     * @throws IOException i/o problem
     * @see Properties#store(Writer, String)
     */
    public void store(Writer writer, String comments, String delimiter)
            throws IOException {
        java.util.Properties p = new java.util.Properties();
        for (String key : keySet()) {
            List<String> values = getStrings(key);
            p.put(key, StringUtils.join(values, delimiter));
        }
        p.store(writer, comments);
        p = null;
    }
    /**
     * Writes this {@link Map} (key and element pairs) to the output character
     * stream in a format suitable for using the 
     * {@link #load(InputStream)} method. 
     * If a key only has one value, then this method behavior is the
     * exact same as the {@link Properties#store(OutputStream, String)} method.
     * Keys with multi-values are joined into a single string, using
     * the default delimiter:
     * {@link Properties#DEFAULT_MULTIVALUE_DELIMITER}
     * @param   out      an output stream.
     * @param   comments   a description of the property list.
     * @throws IOException i/o problem
     * @see Properties#store(OutputStream, String)
     */
    public void store(OutputStream out, String comments) 
            throws IOException {
        store(out, comments, DEFAULT_MULTIVALUE_DELIMITER);
    }
    /**
     * Writes this {@link Map} (key and element pairs) to the output character
     * stream as UTF-8 in a format suitable for using the 
     * {@link #load(InputStream, String)} method. 
     * If a key only has one value, then this method behavior is the
     * exact same as the {@link Properties#store(OutputStream, String)} method.
     * Keys with multi-values are joined into a single string, using
     * the delimiter provided.
     * @param   out      an output stream.
     * @param   comments   a description of the property list.
     * @param delimiter delimiter string to used as a separator when joining 
     *        multiple values for the same key.
     * @throws IOException i/o problem
     * @see Properties#store(OutputStream, String)
     */
    public void store(OutputStream out, String comments, String delimiter) 
            throws IOException {
        store(new OutputStreamWriter(
                out, CharEncoding.UTF_8), comments, delimiter);
    }
    /**
     * Emits an XML document representing all of the properties contained
     * in this {@link Map}, using the specified encoding.
     * If a key only has one value, then this method behavior is the
     * exact same as the 
     * {@link Properties#storeToXML(OutputStream, String, String)} method,
     * where the character encoding is "UTF-8".
     * Keys with multi-values are joined into a single string, using
     * the default delimiter:
     * {@link Properties#DEFAULT_MULTIVALUE_DELIMITER}
     * @param os the output stream on which to emit the XML document.
     * @param comment a description of the property list, or <code>null</code>
     *        if no comment is desired.
     * @throws IOException i/o problem
     * @see Properties#storeToXML(OutputStream, String, String)
     */
    public synchronized void storeToXML(
            OutputStream os, String comment)
            throws IOException {
        storeToXML(os, comment, CharEncoding.UTF_8);        
    }
    /**
     * Emits an XML document representing all of the properties contained
     * in this {@link Map}, using the specified encoding.
     * If a key only has one value, then this method behavior is the
     * exact same as the 
     * {@link Properties#storeToXML(OutputStream, String, String)} method.
     * Keys with multi-values are joined into a single string, using
     * the default delimiter:
     * {@link Properties#DEFAULT_MULTIVALUE_DELIMITER}
     * @param os the output stream on which to emit the XML document.
     * @param comment a description of the property list, or <code>null</code>
     *        if no comment is desired.
     * @param encoding character encoding
     * @throws IOException i/o problem
     * @see Properties#storeToXML(OutputStream, String, String)
     */
    public synchronized void storeToXML(OutputStream os, String comment, 
            String encoding) throws IOException {
        storeToXML(os, comment, encoding, 
                DEFAULT_MULTIVALUE_DELIMITER);
    }
    /**
     * Emits an XML document representing all of the properties contained
     * in this {@link Map}, using the specified encoding.
     * If a key only has one value, then this method behavior is the
     * exact same as the 
     * {@link Properties#storeToXML(OutputStream, String, String)} method.
     * Keys with multi-values are joined into a single string, using
     * the delimiter provided.
     * @param os the output stream on which to emit the XML document.
     * @param comment a description of the property list, or <code>null</code>
     *        if no comment is desired.
     * @param encoding character encoding
     * @param delimiter delimiter string to used as a separator when joining 
     *        multiple values for the same key.
     * @throws IOException i/o problem
     * @see Properties#storeToXML(OutputStream, String, String)
     */
    public synchronized void storeToXML(OutputStream os, String comment, 
            String encoding, String delimiter) throws IOException {
        java.util.Properties p = new java.util.Properties();
        for (String key : keySet()) {
            List<String> values = getStrings(key);
            p.put(key, StringUtils.join(values, delimiter));
        }
        p.storeToXML(os, comment, encoding);
        p = null;
    }
    
    //--- Load -----------------------------------------------------------------
    /**
     * Reads a property list (key and element pairs) from the input
     * character stream in a simple line-oriented format.
     * If a key was stored with multiple values using a delimiter, this,
     * method will split these values appropriately assuming the delimiter is
     * {@link Properties#DEFAULT_MULTIVALUE_DELIMITER}
     * If the key value was stored as a
     * single value, then this method behavior is the
     * exact same as the 
     * {@link Properties#load(Reader)} method.
     * @param   reader   the input character stream.
     * @throws IOException i/o problem
     * @see Properties#load(Reader)
     */
    public synchronized void load(Reader reader)
            throws IOException {
        load(reader, DEFAULT_MULTIVALUE_DELIMITER);
    }
    /**
     * Reads a property list (key and element pairs) from the input
     * character stream in a simple line-oriented format.
     * If a key was stored with multiple values using a delimiter, 
     * this method will split these values appropriately provided the 
     * supplied delimiter is the same. If the key value was stored as a
     * single value, then this method behavior is the
     * exact same as the 
     * {@link Properties#load(Reader)} method.
     * @param   reader   the input character stream.
     * @param delimiter delimiter string to used to parse a multi value
     *        key.
     * @throws IOException i/o problem
     * @see Properties#load(Reader)
     */
    public synchronized void load(Reader reader, String delimiter)
            throws IOException {
        java.util.Properties p = new java.util.Properties();
        p.load(reader);
        for (String key : p.stringPropertyNames()) {
            List<String> values = new ArrayList<>();
            String value = p.getProperty(key);
            if (value != null) {
                values.addAll(Arrays.asList(
                        StringUtils.splitByWholeSeparator(value, delimiter)));
            }
            put(key, values);
        }
        p = null;
    }

    /**
     * <p>Reads all key/value pairs in the given map, and 
     * add them to this <code>Map</code>.  Keys are converted to strings
     * using their toString() method, with exception
     * of values being arrays or collections.  In such case, the entry
     * is considered a multi-value one and each value will be converted
     * to individual strings. <code>null</code> keys are ignored.
     * <code>null</code> values are converted to an empty string.</p> 
     * <p>Changes to this instance
     * won't be reflected in the given <code>Map</code>.  If you want otherwise,
     * use invoke the constructor with a <code>Map</code> argument.</p>
     * 
     * @param map the map containing values to load
     */
    public synchronized void load(Map<?, ?> map) {
        if (map != null) {
            for (Entry<?, ?> entry : map.entrySet()) {
                Object keyObj = entry.getKey();
                if (keyObj == null) {
                    continue;
                }
                String key = Objects.toString(keyObj, null);
                Object valObj = entry.getValue();
                if (valObj == null) {
                    valObj = StringUtils.EMPTY;
                }
                Iterable<?> it = null;
                if (valObj.getClass().isArray()) {
                    it = Arrays.asList((Object[]) valObj);
                } else if (valObj instanceof Iterable) {
                    it = (Iterable<?>) valObj;
                }
                if (it == null) {
                    addString(key, Objects.toString(valObj, null));
                } else {
                    for (Object val : it) {
                        addString(key, Objects.toString(val, null));
                    }
                }
            }
        }
    }

    
    /**
     * Reads a property list (key and element pairs) from the input
     * character stream (UTF-8) in a simple line-oriented format.
     * If a key was stored with multiple values using a delimiter, this,
     * method will split these values appropriately assuming the delimiter is
     * {@link Properties#DEFAULT_MULTIVALUE_DELIMITER}
     * If the key value was stored as a
     * single value, then this method behavior is the
     * exact same as the 
     * {@link Properties#load(InputStream)} method.
     * @param   inStream   the input stream.
     * @throws IOException i/o problem
     * @see Properties#load(InputStream)
     */
    public synchronized void load(InputStream inStream)
            throws IOException {
        load(new InputStreamReader(inStream, CharEncoding.UTF_8),
                DEFAULT_MULTIVALUE_DELIMITER);
    }
    /**
     * Reads a property list (key and element pairs) from the input
     * character stream (UTF8) in a simple line-oriented format.
     * If a key was stored with multiple values using a delimiter, 
     * this method will split these values appropriately provided the 
     * supplied delimiter is the same. If the key value was stored as a
     * single value, then this method behavior is the
     * exact same as the 
     * {@link Properties#load(InputStream)} method.
     * @param   inStream   the input stream.
     * @param delimiter delimiter string to used to parse a multi value
     *        key.
     * @throws IOException i/o problem
     * @see Properties#load(InputStream)
     */
    public synchronized void load(InputStream inStream, String delimiter)
            throws IOException {
        load(new InputStreamReader(inStream, CharEncoding.UTF_8), delimiter);
    }
    /**
     * Loads all of the properties represented by the XML document on the
     * specified input stream into this instance.
     * If a key was stored with multiple values using a delimiter, 
     * method will split these values appropriately assuming the delimiter is
     * {@link Properties#DEFAULT_MULTIVALUE_DELIMITER}
     * If the key value was stored as a
     * single value, then this method behavior is the
     * exact same as the 
     * {@link Properties#loadFromXML(InputStream)} method.
     * @param in in the input stream from which to read the XML document.
     * @throws IOException i/o problem
     */
    public synchronized void loadFromXML(InputStream in)
            throws IOException {
        loadFromXML(in, DEFAULT_MULTIVALUE_DELIMITER);
    }
    /**
     * Loads all of the properties represented by the XML document on the
     * specified input stream into this instance.
     * If a key was stored with multiple values using a delimiter, 
     * this method will split these values appropriately provided the 
     * supplied delimiter is the same. If the key value was stored as a
     * single value, then this method behavior is the
     * exact same as the 
     * {@link Properties#loadFromXML(InputStream)} method.
     * @param in in the input stream from which to read the XML document.
     * @param delimiter delimiter string to used to parse a multi value
     *        key.
     * @throws IOException i/o problem
     */
    public synchronized void loadFromXML(InputStream in, String delimiter)
            throws IOException {
        java.util.Properties p = new java.util.Properties();
        p.loadFromXML(in);
        List<String> values = new ArrayList<>();
        for (String key : p.stringPropertyNames()) {
            String value = p.getProperty(key);
            if (value != null) {
                values.addAll(Arrays.asList(
                        StringUtils.splitByWholeSeparator(value, delimiter)));
            }
            put(key, values);
        }
        p = null;
    }
    /**
     * Reads a property list (key and element pairs) from the UTF-8 input
     * string.  Otherwise, the same considerations as
     * {@link #load(InputStream)} apply.
     * @param str the string to load
     * @throws IOException problem loading string
     */
    public void loadFromString(String str) throws IOException {
        InputStream is = new ByteArrayInputStream(
                str.getBytes(CharEncoding.UTF_8));
        load(is);
        is.close();
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
        String s = getString(key);
        if (s == null) {
            return defaultValue;
        }
        return s;
    }
    /**
     * Gets values as a list of strings.
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
        BigDecimal value = getBigDecimal(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
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
        Date value = getDate(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
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
        File value = getFile(key);
        if (value == null) {
            return defaultValue;
        }
    	return value;
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
        Class<?> value = getClass(key);
        if (value == null) {
            return defaultValue;
        }
    	return value;
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
        for (String value : nullSafeValues) {
            if (value == null) {
                nullSafeValues.add(StringUtils.EMPTY);
            } else {
                nullSafeValues.add(value);
            }
        }
        return super.put(caseResolvedKey(key), values);
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
            
        }
        return strArray;
    }
}
