/* Copyright 2010-2013 Norconex Inc.
 * 
 * This file is part of Norconex Commons Lang.
 * 
 * Norconex Commons Lang is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Norconex Commons Lang is distributed in the hope that it will be useful, 
 * but WITHOUT ANY WARRANTY; without even the implied warranty of 
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with Norconex Commons Lang. If not, see <http://www.gnu.org/licenses/>.
 */
package com.norconex.commons.lang.map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.InvalidPropertiesFormatException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * <p>This class is a enhanced version of {@link java.util.Properties}
 * that enforces the use of String keys and values, but offers many
 * convenience methods for storing and retrieving multiple values of different
 * types (e.g. Integer, Locale, File, etc).  While it does not extend 
 * {@link java.util.Properties}, it offers similar load and store
 * method and can be used as a replacement for it in many cases 
 * (e.g. works great with configuration files).</p>
 * 
 * <p>It can also be used as a 
 * string-based multi-value map with helpful methods.  This works great
 * in a few scenarios, like easily accessing or manipulating URL query string
 * values.  It extends 
 * {@link TreeMap} so that keys are always sorted, either by the 
 * <code>String</code> natural order, or by supplying a comparator.
 * </p>
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
 * @author <a href="mailto:pascal.essiembre@norconex.com">Pascal Essiembre</a>
 */
@SuppressWarnings("nls")
public class Properties extends TreeMap<String, List<String>> {

    private static final long serialVersionUID = -7215126924574341L;

    /**
     * Default delimiter when storing/loading multi-values to/from 
     * <code>.properties</code> files.
     */
    public static final String DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER = "^^^";
    
    /** Logger. */
    private static final Logger LOG =
        LogManager.getLogger(Properties.class);
    
    private boolean caseSensitiveKeys = false;
    
    /**
     * Create a new instance with case-sensitive keys.
     * @see TreeMap#TreeMap()
     */
    public Properties() {
        super();
    }

    /**
     * Creates a new instance.
     * @param caseSensitiveKeys getter methods taking a key as well as
     *        {{@link #remove(Object)} will 
     *        return all combined values or remove all keys where keys are 
     *        equal, ignoring case, to the key supplied. 
     * @see TreeMap#TreeMap()
     */
    public Properties(boolean caseSensitiveKeys) {
        this(null, false);
    }
    
    /**
     * <p>Creates a new <code>Properties</code> initializing it with values
     * from the given <code>Map</code>.  {@link ObjectUtils#toString(Object)} 
     * is used to convert to convert keys and values to strings, with exception
     * of values being arrays or collections.  In such case, the entry
     * is considered a mult-value one and each value will be converted
     * to individual strings. <code>null</code> keys are ignored.</p>
     * <code>null</code> values are converted to an empty string. 
     * <p>Changes to this instance
     * won't be reflected in the given <code>Map</code>. Keys are
     * case-sensitive.</p>
     * 
     * @param defaults the default values
     */
    public Properties(Map<?, ?> defaults) {
        this(defaults, false);
    }
    
    /**
     * <p>Creates a new <code>Properties</code> initializing it with values
     * from the given <code>Map</code>.  {@link ObjectUtils#toString(Object)} 
     * is used to convert to convert keys and values to strings, with exception
     * of values being arrays or collections.  In such case, the entry
     * is considered a mult-value one and each value will be converted
     * to individual strings. <code>null</code> keys are ignored.</p>
     * <code>null</code> values are converted to an empty string. 
     * <p>Changes to this instance
     * won't be reflected in the given <code>Map</code>.</p>
     * 
     * @param defaults the default values
     * @param caseSensitiveKeys getter methods taking a key as well as
     *        {{@link #remove(Object)} will 
     *        return all combined values or remove all keys where keys are 
     *        equal, ignoring case, to the key supplied. 
     */
    public Properties(Map<?, ?> defaults, boolean caseSensitiveKeys) {
        super();
        this.caseSensitiveKeys = caseSensitiveKeys;
        if (defaults != null) {
            for (Object keyObj : defaults.keySet()) {
                if (keyObj == null) {
                    continue;
                }
                String key = ObjectUtils.toString(keyObj);
                Object valObj = defaults.get(keyObj);
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
                    addString(key, ObjectUtils.toString(valObj));
                } else {
                    for (Object val : it) {
                        addString(key, ObjectUtils.toString(val));
                    }
                }
            }
        }
    }

    //--- Store ----------------------------------------------------------------
    /**
     * Writes this property list (key and element pairs) in this
     * <code>Properties</code> table to the output stream in a format suitable
     * for loading into a <code>Properties</code> table using the
     * {@link #load(InputStream) load} method.
     * Otherwise, the same considerations as
     * {@link #store(OutputStream, String)} apply.
     * @param   comments   a description of the property list.
     * @return the properties as string
     * @throws IOException problem storing to string
     */
    public String storeToString(String comments) throws IOException {
        OutputStream os = new ByteArrayOutputStream();
        store(os, comments);
        String str = os.toString();
        os.close();
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
     * {@link Properties#DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER}
     * @param   writer     an output character stream writer.
     * @param   comments   a description of the property list.
     * @see Properties#store(Writer, String)
     */
    public void store(Writer writer, String comments) throws IOException {
        store(writer, comments, DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER);
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
     * {@link Properties#DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER}
     * @param   out      an output stream.
     * @param   comments   a description of the property list.
     * @see Properties#store(OutputStream, String)
     */
    public void store(OutputStream out, String comments) 
            throws IOException {
        store(out, comments, DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER);
    }
    /**
     * Writes this {@link Map} (key and element pairs) to the output character
     * stream in a format suitable for using the 
     * {@link #load(InputStream, String)} method. 
     * If a key only has one value, then this method behavior is the
     * exact same as the {@link Properties#store(OutputStream, String)} method.
     * Keys with multi-values are joined into a single string, using
     * the delimiter provided.
     * @param   out      an output stream.
     * @param   comments   a description of the property list.
     * @param delimiter delimiter string to used as a separator when joining 
     *        multiple values for the same key.
     * @see Properties#store(OutputStream, String)
     */
    public void store(OutputStream out, String comments, String delimiter) 
            throws IOException {
        store(new OutputStreamWriter(out, "8859_1"), comments, delimiter);
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
     * {@link Properties#DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER}
     * @param os the output stream on which to emit the XML document.
     * @param comment a description of the property list, or <code>null</code>
     *        if no comment is desired.
     * @see Properties#storeToXML(OutputStream, String, String)
     */
    public synchronized void storeToXML(
            OutputStream os, String comment)
            throws IOException {
        storeToXML(os, comment, "UTF-8");        
    }
    /**
     * Emits an XML document representing all of the properties contained
     * in this {@link Map}, using the specified encoding.
     * If a key only has one value, then this method behavior is the
     * exact same as the 
     * {@link Properties#storeToXML(OutputStream, String, String)} method.
     * Keys with multi-values are joined into a single string, using
     * the default delimiter:
     * {@link Properties#DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER}
     * @param os the output stream on which to emit the XML document.
     * @param comment a description of the property list, or <code>null</code>
     *        if no comment is desired.
     * @param encoding character encoding
     * @see Properties#storeToXML(OutputStream, String, String)
     */
    public synchronized void storeToXML(OutputStream os, String comment, 
            String encoding) throws IOException {
        storeToXML(os, comment, encoding, 
                DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER);
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
     * {@link Properties#DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER}
     * If the key value was stored as a
     * single value, then this method behavior is the
     * exact same as the 
     * {@link Properties#load(Reader)} method.
     * @param   reader   the input character stream.
     * @see Properties#load(Reader)
     */
    public synchronized void load(Reader reader)
            throws IOException {
        load(reader, DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER);
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
     * @see Properties#load(Reader)
     */
    public synchronized void load(Reader reader, String delimiter)
            throws IOException {
        java.util.Properties p = new java.util.Properties();
        p.load(reader);
        for (String key : p.stringPropertyNames()) {
            List<String> values = new ArrayList<String>();
            String value = p.getProperty(key);
            if (value != null) {
                values.addAll(Arrays.asList(
                        StringUtils.split(value, delimiter)));
            }
            put(key, values);
        }
        p = null;
    }
    /**
     * Reads a property list (key and element pairs) from the input
     * character stream in a simple line-oriented format.
     * If a key was stored with multiple values using a delimiter, this,
     * method will split these values appropriately assuming the delimiter is
     * {@link Properties#DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER}
     * If the key value was stored as a
     * single value, then this method behavior is the
     * exact same as the 
     * {@link Properties#load(InputStream)} method.
     * @param   inStream   the input stream.
     * @see Properties#load(InputStream)
     */
    public synchronized void load(InputStream inStream)
            throws IOException {
        load(new InputStreamReader(inStream, "8859_1"),
                DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER);
    }
    /**
     * Reads a property list (key and element pairs) from the input
     * character stream in a simple line-oriented format.
     * If a key was stored with multiple values using a delimiter, 
     * this method will split these values appropriately provided the 
     * supplied delimiter is the same. If the key value was stored as a
     * single value, then this method behavior is the
     * exact same as the 
     * {@link Properties#load(InputStream)} method.
     * @param   inStream   the input stream.
     * @param delimiter delimiter string to used to parse a multi value
     *        key.
     * @see Properties#load(InputStream)
     */
    public synchronized void load(InputStream inStream, String delimiter)
            throws IOException {
        load(new InputStreamReader(inStream, "8859_1"), delimiter);
    }
    /**
     * Loads all of the properties represented by the XML document on the
     * specified input stream into this instance.
     * If a key was stored with multiple values using a delimiter, 
     * method will split these values appropriately assuming the delimiter is
     * {@link Properties#DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER}
     * If the key value was stored as a
     * single value, then this method behavior is the
     * exact same as the 
     * {@link Properties#loadFromXML(InputStream)} method.
     * @param in in the input stream from which to read the XML document.
     */
    public synchronized void loadFromXML(InputStream in)
            throws IOException, InvalidPropertiesFormatException {
        loadFromXML(in, DEFAULT_PROPERTIES_MULTIVALUE_DELIMITER);
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
     */
    public synchronized void loadFromXML(InputStream in, String delimiter)
            throws IOException, InvalidPropertiesFormatException {
        java.util.Properties p = new java.util.Properties();
        p.loadFromXML(in);
        List<String> values = new ArrayList<String>();
        for (String key : p.stringPropertyNames()) {
            String value = p.getProperty(key);
            if (value != null) {
                values.addAll(Arrays.asList(
                        StringUtils.split(value, delimiter)));
            }
            put(key, values);
        }
        p = null;
    }
    /**
    * Reads a property list (key and element pairs) from the input
    * string.  Otherwise, the same considerations as
    * {@link #load(InputStream)} apply.
    * @param str the string to load
    * @throws IOException problem loading string
    */
    public void loadFromString(String str) throws IOException {
        InputStream is = new ByteArrayInputStream(str.getBytes());
        load(is);
        is.close();
    }

    //--- String ---------------------------------------------------------------
    public String getString(String key) {
        List<String> list = get(key);
        if (list != null && !list.isEmpty()) {
            return list.get(0);
        }
        return null;
    }
    public String getString(String key, String defaultValue) {
        String s = getString(key);
        if (s == null) {
            return defaultValue;
        }
        return s;
    }
    public List<String> getStrings(String key) {
        List<String> values = get(key);
        if (values == null) {
            return new ArrayList<String>();
        }
        return new ArrayList<String>(values);
    }
    /**
     * Sets one or multiple string values.  
     * Setting a string with a <code>null</code> value will set a blank string.
     * @param key the key of the value to set
     * @param values the values to set
     */
    public void setString(String key, String... values) {
        List<String> list = new ArrayList<String>(values.length);
        for (String value : values) {
            if (value == null) {
                list.add("");
            } else {
                list.add(value);
            }
        }
        put(key, list);
    }
    /**
     * Adds one or multiple string values.  
     * Setting a string with a <code>null</code> value will set a blank string.
     * @param key the key of the value to set
     * @param values the values to set
     */
    public void addString(String key, String... values) {
        List<String> list = get(key);
        if (list == null) {
            list = new ArrayList<String>(values.length);
        }
        for (String value : values) {
            if (value == null) {
                list.add("");
            } else {
                list.add(value);
            }
        }
        put(key, list);
    }

    //--- Integer --------------------------------------------------------------
    public int getInt(String key) {
        try {
            return Integer.parseInt(getString(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse integer value.", key, getString(key), e);
        }
    }
    public int getInt(String key, int defaultValue) {
        String value = getString(key, "" + defaultValue);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse integer value.", key, value, e);
        }
    }
    public List<Integer> getInts(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Integer> ints = new ArrayList<Integer>(values.size());
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
    public void setInt(String key, int... values) {
        setString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    public void addInt(String key, int... values) {
        addString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    
    //--- Double ---------------------------------------------------------------
    public double getDouble(String key) {
        try {
            return Double.parseDouble(getString(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse double value.", key, getString(key), e);
        }
    }
    public double getDouble(String key, double defaultValue) {
        String value = getString(key, "" + defaultValue);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse double value.", key, value, e);
        }
    }
    public List<Double> getDoubles(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Double> list = new ArrayList<Double>(values.size());
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
    public void setDouble(String key, double... values) {
        setString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    public void addDouble(String key, double... values) {
        addString(key, toStringArray(ArrayUtils.toObject(values)));
    }

    //--- Long -----------------------------------------------------------------
    public long getLong(String key) {
        try {
            return Long.parseLong(getString(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse long value.", key, getString(key), e);
        }
    }
    public long getLong(String key, long defaultValue) {
        String value = getString(key, "" + defaultValue);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse long value.", key, value, e);
        }
    }
    public List<Long> getLongs(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Long> list = new ArrayList<Long>(values.size());
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
    public void setLong(String key, long... values) {
        setString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    public void addLong(String key, long... values) {
        addString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    
    //--- Float ----------------------------------------------------------------
    public float getFloat(String key) {
        try {
            return Float.parseFloat(getString(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse float value.", key, getString(key), e);
        }
    }
    public float getFloat(String key, float defaultValue) {
        String value = getString(key, "" + defaultValue);
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse float value.", key, value, e);
        }
    }
    public List<Float> getFloats(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Float> list = new ArrayList<Float>(values.size());
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
    public void setFloat(String key, float... values) {
        setString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    public void addFloat(String key, float... values) {
        addString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    
    //--- BigDecimal -----------------------------------------------------------
    public BigDecimal getBigDecimal(String key) {
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
    public BigDecimal getBigDecimal(String key, BigDecimal defaultValue) {
        BigDecimal value = getBigDecimal(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }
    public void setBigDecimal(String key, BigDecimal value) {
        if (value == null) {
            setString(key, "");
        } else {
            setString(key, value.toString());
        }
    }
    public List<BigDecimal> getBigDecimals(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<BigDecimal> list = new ArrayList<BigDecimal>(values.size());
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
    public void setBigDecimal(String key, BigDecimal... values) {
        setString(key, toStringArray(values));
    }
    public void addBigDecimal(String key, BigDecimal... values) {
        addString(key, toStringArray(values));
    }
    
    //--- Date -----------------------------------------------------------------
    public Date getDate(String key) {
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
    public Date getDate(String key, Date defaultValue) {
        Date value = getDate(key);
        if (value == null) {
            return defaultValue;
        }
        return value;
    }    
    public List<Date> getDates(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Date> list = new ArrayList<Date>(values.size());
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
    public void setDate(String key, Date... values) {
        setString(key, datesToStringArray(values));
    }
    public void addDate(String key, Date... values) {
        addString(key, datesToStringArray(values));
    }
    private String[] datesToStringArray(Date... values) {
        if (values == null) {
            return null;
        }
        String[] array = new String[values.length];
        for (int i = 0; i < array.length; i++) {
            array[i] = Long.toString(values[i].getTime());
        }
        return array;
    }

    //--- Boolean --------------------------------------------------------------
    public boolean getBoolean(String key) {
        return Boolean.valueOf(getString(key)).booleanValue();
    }
    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.valueOf(
                getString(key, "" + defaultValue)).booleanValue();
    }    
    public void setBoolean(String key, boolean value) {
        setString(key, Boolean.toString(value));
    }
    public List<Boolean> getBooleans(String key) {
        List<String> values = getStrings(key);
        List<Boolean> list = new ArrayList<Boolean>(values.size());
        for (String value : values) {
            list.add(Boolean.parseBoolean(value));
        }
        return list;
    }
    public void setBoolean(String key, boolean... values) {
        setString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    public void addBoolean(String key, boolean... values) {
        addString(key, toStringArray(ArrayUtils.toObject(values)));
    }
    
    //--- Locale ---------------------------------------------------------------
    public Locale getLocale(String key) {
        try {
            return LocaleUtils.toLocale(getString(key));
        } catch (IllegalArgumentException e) {
            throw createTypedException(
                    "Could not parse Locale value.", key, getString(key), e);
        }
    }
    public Locale getLocale(String key, Locale defaultValue) {
        try {
            return LocaleUtils.toLocale(getString(key));
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
    public List<Locale> getLocales(String key) {
        List<String> values = getStrings(key);
        String errVal = null;
        try {
            List<Locale> list = new ArrayList<Locale>(values.size());
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
    public void setLocale(String key, Locale... values) {
        setString(key, toStringArray(values));
    }
    public void addLocale(String key, Locale... values) {
        addString(key, toStringArray(values));
    }
    
    //--- File -----------------------------------------------------------------
    /**
     * Gets a file, assuming key value is a file system path. 
     * @param key properties key
     */
    public File getFile(String key) {
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
     */
    public File getFile(String key, File defaultValue) {
        File value = getFile(key);
        if (value == null) {
            return defaultValue;
        }
    	return value;
    }
    public List<File> getFiles(String key) {
        List<String> values = getStrings(key);
        List<File> list = new ArrayList<File>(values.size());
        for (String value : values) {
            list.add(new File(value));
        }
        return list;
    }
    public void setFile(String key, File... values) {
        setString(key, filesToStringArray(values));
    }
    public void addFile(String key, File... values) {
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
     */
    public Class<?> getClass(String key) {
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
     */
    public Class<?> getClass(String key, Class<?> defaultValue) {
        Class<?> value = getClass(key);
        if (value == null) {
            return defaultValue;
        }
    	return value;
    }
    public List<Class<?>> getClasses(String key) {
        List<String> values = getStrings(key);
        List<Class<?>> list = new ArrayList<Class<?>>(values.size());
        for (String value : values) {
            list.add(getClass(value));
        }
        return list;
    }
    public void setClass(String key, Class<?>... values) {
        setString(key, classesToStringArray(values));
    }
    public void addClass(String key, Class<?>... values) {
        addString(key, classesToStringArray(values));
    }

    
    
    @Override
    public List<String> get(Object key) {
        if (!caseSensitiveKeys) {
            return super.get(key);
        }
        List<String> values = new ArrayList<String>();
        for (String pkey : keySet()) {
            if (StringUtils.equalsIgnoreCase(pkey, ObjectUtils.toString(key))) {
                values.addAll(super.get(pkey));
            }
        }
        return values;
    }
    @Override
    public List<String> remove(Object key) {
        if (!caseSensitiveKeys) {
            return super.remove(key);
        }
        List<String> values = new ArrayList<String>();
        for (Iterator<String> iterator = keySet().iterator(); 
                iterator.hasNext();) {
            String pkey = (String) iterator.next();
            if (StringUtils.equalsIgnoreCase(pkey, ObjectUtils.toString(key))) {
                iterator.remove();
            }
        }
        return values;
    }
    
    
    //--- Privates -------------------------------------------------------------
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
            strArray[i] = ObjectUtils.toString(array[i], "");
            
        }
        return strArray;
    }
}
