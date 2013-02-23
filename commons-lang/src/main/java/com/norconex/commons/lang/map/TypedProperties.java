package com.norconex.commons.lang.map;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Properties;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

/**
 * This class represents a persistent set of
 * properties just like its {@link Properties} parent class.  It adds
 * to the parent class the ability to obtain and set primitive values and
 * some commonly used objects.  Upon encountering a problem in parsing the
 * data to its target format, a {@link TypedPropertiesException} is thrown.
 * @author Pascal Essiembre (pascal.essiembre&#x40;norconex.com)
 */
@SuppressWarnings("nls")
public class TypedProperties extends Properties {

    /** Default delimiter when dealing with arrays. */
    public static final String DEFAULT_DELIMITER = "^^";
    
    /** For serialization. */
    private static final long serialVersionUID = -7215126924574341L;

    /** Logger. */
    private static final Logger LOG =
        LogManager.getLogger(TypedProperties.class);
    
    private String delimiter = DEFAULT_DELIMITER;
    
    /**
     * @see Properties#Properties()
     */
    public TypedProperties() {
        super();
    }
    /**
     * @see Properties#Properties(Properties)
     */
    public TypedProperties(Properties defaults) {
        super(defaults);
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
     * Gets the delimiter used to split multi-value properties.
     * @return delimiter
     */
    public String getDelimiter() {
        return delimiter;
    }
    /**
     * Sets the delimiter used to split multi-value properties.
     * @param delimiter the delimiter
     */
    public void setDelimiter(String delimiter) {
        this.delimiter = delimiter;
    }

    //--- String ---------------------------------------------------------------
    public String getString(String key) {
        return getProperty(key);
    }
    public String getString(String key, String defaultValue) {
        return getProperty(key, defaultValue);
    }
    /**
     * Sets a string.  Setting a string with a <code>null</code> value
     * will set a blank string.
     * @param key the key of the value to set
     * @param value the value to set
     */
    public void setString(String key, String value) {
    	if (value == null) {
            setProperty(key, "");
    	} else {
            setProperty(key, value);
    	}
    }
    public String[] getStrings(String key) {
        return StringUtils.split(getString(key), delimiter);
    }
    public void setStrings(String key, String[] values) {
        setString(key, StringUtils.join(values, delimiter));
    }
    
    //--- Integer --------------------------------------------------------------
    public int getInt(String key) {
        try {
            return Integer.parseInt(getProperty(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse integer value.", key, getProperty(key), e);
        }
    }
    public int getInt(String key, int defaultValue) {
        String value = getProperty(key, "" + defaultValue);
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse integer value.", key, value, e);
        }
    }
    public void setInt(String key, int value) {
        setProperty(key, Integer.toString(value));
    }
    public int[] getInts(String key) {
        String[] values = getStrings(key);
        String value = null;
        try {
            int[] ints = new int[values.length];
            for (int i = 0; i < ints.length; i++) {
                value = values[i];
                ints[i] = Integer.parseInt(value);
            }
            return ints;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse integer value.", key, value, e);
        }
    }
    public void setInts(String key, int[] values) {
        Object[] ints = ArrayUtils.toObject(values);
        setString(key, StringUtils.join(ints, delimiter));
    }
    
    //--- Double ---------------------------------------------------------------
    public double getDouble(String key) {
        try {
            return Double.parseDouble(getProperty(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse double value.", key, getProperty(key), e);
        }
    }
    public double getDouble(String key, double defaultValue) {
        String value = getProperty(key, "" + defaultValue);
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse double value.", key, value, e);
        }
    }
    public void setDouble(String key, double value) {
        setProperty(key, Double.toString(value));
    }
    public double[] getDoubles(String key) {
        String[] values = getStrings(key);
        String value = null;
        try {
            double[] array = new double[values.length];
            for (int i = 0; i < array.length; i++) {
                value = values[i];
                array[i] = Double.parseDouble(value);
            }
            return array;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse double value.", key, value, e);
        }
    }
    public void setDoubles(String key, double[] values) {
        Object[] array = ArrayUtils.toObject(values);
        setString(key, StringUtils.join(array, delimiter));
    }

    //--- Long -----------------------------------------------------------------
    public long getLong(String key) {
        try {
            return Long.parseLong(getProperty(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse long value.", key, getProperty(key), e);
        }
    }
    public long getLong(String key, long defaultValue) {
        String value = getProperty(key, "" + defaultValue);
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse long value.", key, value, e);
        }
    }
    public void setLong(String key, long value) {
        setProperty(key, Long.toString(value));
    }
    public long[] getLongs(String key) {
        String[] values = getStrings(key);
        String value = null;
        try {
            long[] array = new long[values.length];
            for (int i = 0; i < array.length; i++) {
                value = values[i];
                array[i] = Long.parseLong(value);
            }
            return array;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse long value.", key, value, e);
        }
    }
    public void setLongs(String key, long[] values) {
        Object[] array = ArrayUtils.toObject(values);
        setString(key, StringUtils.join(array, delimiter));
    }
    
    //--- Float ----------------------------------------------------------------
    public float getFloat(String key) {
        try {
            return Float.parseFloat(getProperty(key));
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse float value.", key, getProperty(key), e);
        }
    }
    public float getFloat(String key, float defaultValue) {
        String value = getProperty(key, "" + defaultValue);
        try {
            return Float.parseFloat(value);
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse floag value.", key, value, e);
        }
    }
    public void setFloat(String key, float value) {
        setProperty(key, Float.toString(value));
    }
    public float[] getFloats(String key) {
        String[] values = getStrings(key);
        String value = null;
        try {
            float[] array = new float[values.length];
            for (int i = 0; i < array.length; i++) {
                value = values[i];
                array[i] = Float.parseFloat(value);
            }
            return array;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse float value.", key, value, e);
        }
    }
    public void setFloats(String key, float[] values) {
        Object[] array = ArrayUtils.toObject(values);
        setString(key, StringUtils.join(array, delimiter));
    }
    
    //--- BigDecimal -----------------------------------------------------------
    public BigDecimal getBigDecimal(String key) {
        String value = getProperty(key);
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
            setProperty(key, "");
        } else {
            setProperty(key, value.toString());
        }
    }
    public BigDecimal[] getBigDecimals(String key) {
        String[] values = getStrings(key);
        String value = null;
        try {
            BigDecimal[] array = new BigDecimal[values.length];
            for (int i = 0; i < array.length; i++) {
                value = values[i];
                array[i] = new BigDecimal(value);
            }
            return array;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse BigDecimal value.", key, value, e);
        }
    }
    public void setBigDecimals(String key, BigDecimal[] values) {
        setString(key, StringUtils.join(values, delimiter));
    }
    
    //--- Date -----------------------------------------------------------------
    public Date getDate(String key) {
        String value = getProperty(key);
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
    public void setDate(String key, Date value) {
        if (value == null) {
            setProperty(key, "");
        } else {
            setLong(key, value.getTime());
        }
    }
    public Date[] getDates(String key) {
        String[] values = getStrings(key);
        String value = null;
        try {
            Date[] array = new Date[values.length];
            for (int i = 0; i < array.length; i++) {
                value = values[i];
                array[i] = new Date(Long.parseLong(value));
            }
            return array;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse Date value.", key, value, e);
        }
    }
    public void setDates(String key, Date[] values) {
        if (values == null) {
            setProperty(key, "");
        } else {
            String[] array = new String[values.length];
            for (int i = 0; i < array.length; i++) {
                array[i] = Long.toString(values[i].getTime());
            }
            setStrings(key, array);
        }
    }

    //--- Boolean --------------------------------------------------------------
    public boolean getBoolean(String key) {
        return Boolean.valueOf(getProperty(key)).booleanValue();
    }
    public boolean getBoolean(String key, boolean defaultValue) {
        return Boolean.valueOf(
                getProperty(key, "" + defaultValue)).booleanValue();
    }    
    public void setBoolean(String key, boolean value) {
        setProperty(key, Boolean.toString(value));
    }
    public boolean[] getBooleans(String key) {
        String[] values = getStrings(key);
        String value = null;
        try {
            boolean[] array = new boolean[values.length];
            for (int i = 0; i < array.length; i++) {
                value = values[i];
                array[i] = Boolean.parseBoolean(value);
            }
            return array;
        } catch (NumberFormatException e) {
            throw createTypedException(
                    "Could not parse boolean value.", key, value, e);
        }
    }
    public void setBooleans(String key, boolean[] values) {
        Object[] array = ArrayUtils.toObject(values);
        setString(key, StringUtils.join(array, delimiter));
    }
    
    //--- Locale ---------------------------------------------------------------
    public Locale getLocale(String key) {
        try {
            return LocaleUtils.toLocale(getProperty(key));
        } catch (IllegalArgumentException e) {
            throw createTypedException(
                    "Could not parse Locale value.", key, getProperty(key), e);
        }
    }
    public Locale getLocale(String key, Locale defaultValue) {
        try {
            return LocaleUtils.toLocale(getProperty(key));
        } catch (IllegalArgumentException e) {
            return defaultValue;
        }
    }
    public void setLocale(String key, Locale value) {
        if (value == null) {
            setProperty(key, "");
        } else {
            setProperty(key, value.toString());
        }
    }
    public Locale[] getLocales(String key) {
        String[] values = getStrings(key);
        String value = null;
        try {
            Locale[] array = new Locale[values.length];
            for (int i = 0; i < array.length; i++) {
                value = values[i];
                array[i] = LocaleUtils.toLocale(value);
            }
            return array;
        } catch (IllegalArgumentException e) {
            throw createTypedException(
                    "Could not parse locale value.", key, value, e);
        }
    }
    public void setLocales(String key, Locale[] values) {
        if (values == null) {
            setProperty(key, "");
        } else {
            String[] array = new String[values.length];
            for (int i = 0; i < array.length; i++) {
                array[i] = values[i].toString();
            }
            setStrings(key, array);
        }
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
    public void setFile(String key, File value) {
        if (value == null) {
            setProperty(key, "");
        } else {
            setProperty(key, value.getPath());
        }
    }
    public File[] getFiles(String key) {
        String[] values = getStrings(key);
        String value = null;
        File[] array = new File[values.length];
        for (int i = 0; i < array.length; i++) {
            value = values[i];
            array[i] = new File(value);
        }
        return array;
    }
    public void setFiles(String key, File[] values) {
        if (values == null) {
            setProperty(key, "");
        } else {
            String[] array = new String[values.length];
            for (int i = 0; i < array.length; i++) {
                array[i] = values[i].getPath();
            }
            setStrings(key, array);
        }
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
    public void setClass(String key, Class<?> value) {
        if (value == null) {
            setProperty(key, "");
        } else {
            setProperty(key, value.getName());
        }
    }
    public Class<?>[] getClasses(String key) {
        String[] values = getStrings(key);
        String value = null;
        Class<?>[] array = new Class<?>[values.length];
        for (int i = 0; i < array.length; i++) {
            value = values[i];
            array[i] = getClass(value);
        }
        return array;
    }
    public void setClasses(String key, Class<?>[] values) {
        if (values == null) {
            setProperty(key, "");
        } else {
            String[] array = new String[values.length];
            for (int i = 0; i < array.length; i++) {
                array[i] = values[i].getName();
            }
            setStrings(key, array);
        }
    }
    

    
    private TypedPropertiesException createTypedException(
            String msg, String key, String value, Exception cause) {
        String message = msg + " [key=" + key + "; value=" + value + "].";
        LOG.error(message, cause);
        return new TypedPropertiesException(message, cause);
    }
    /**
     * Converts value to a String using its "toString" method before storing
     * it.  If null, the value is converted to an empty string.  Arrays and
     * collections are joined by the specified delimiter.
     * @see java.util.Hashtable#put(java.lang.Object, java.lang.Object)
     */
    public synchronized Object put(Object key, Object value) {
        if (value == null) {
            return super.put(key, "");
        }
        String strValue;
        if (value.getClass().isArray()) {
            strValue = StringUtils.join((Object[]) value, delimiter);
        } else if (value instanceof Collection<?>) {
            strValue = StringUtils.join((Collection<?>) value, delimiter);
        } else {
            strValue = value.toString();
        }
        return super.put(key, strValue);
    }
}
