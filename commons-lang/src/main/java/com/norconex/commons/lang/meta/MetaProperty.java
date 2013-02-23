package com.norconex.commons.lang.meta;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A metadata property, which can contain one or more values.
 * @author Pascal Essiembre
 */
public class MetaProperty implements Serializable {
	private static final long serialVersionUID = -3517783810300646335L;
	private String name;
	private final List<String> values = new ArrayList<String>();
	public MetaProperty() {
        super();
    }
    public MetaProperty(String name) {
        super();
        this.name = name;
    }
    public MetaProperty(String name, String... values) {
        super();
        this.name = name;
        this.values.addAll(Arrays.asList(values));
    }

    /**
	 * Gets the property name.
	 * @return the property name
	 */
	public String getName() {
		return name;
	}
	/**
	 * Sets the property name.
	 * @param name the property name
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * Gets the property values.
	 * @return the property values
	 */
	public List<String> getValues() {
		return values;
	}
	/**
	 * Gets a property value.  If the property has multiple values, 
	 * the first one is returned only.  Returns null of there are no values.
	 * @return the property values
	 */
	public String getValue() {
	    if (values.isEmpty()) {
	        return null;
	    }
		return values.get(0);
	}
	/**
	 * Sets the values for this property, clearing any existing values
	 * first.
	 * @param values the new values
	 */
	public void setValues(List<String> values) {
		this.values.clear();
		this.values.addAll(values);
	}
	/**
	 * Sets the value for this property, clearing any existing values
	 * first.
	 * @param value the value to set
	 */
	public void setValue(String value) {
		this.values.clear();
		this.values.add(value);
	}
	/**
	 * Adds new values to this property.  If a value already exists 
	 * for this property, it will be replaced by the new value, appended
	 * at the end of the value list.
	 * @param values values to add
	 */
	public void addValues(List<String> values) {
	    this.values.removeAll(values);
		this.values.addAll(values);
	}
    /**
     * Add a new value to this property.  If the value already exists 
     * for this property, it will be replaced by the new value, appended
     * at the end of the value list.
     * @param value value to add
     */
	public void addValue(String value) {
	    this.values.remove(value);
		this.values.add(value);
	}
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result
				+ ((values == null) ? 0 : values.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetaProperty other = (MetaProperty) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (values == null) {
			if (other.values != null)
				return false;
		} else if (!values.equals(other.values))
			return false;
		return true;
	}
    @Override
    public String toString() {
        return "MetaProperty [name=" + name + ", values=" + values + "]";
    }
}