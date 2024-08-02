package com.fasterxml.jackson.dataformat.xml.ser;

import java.util.Collection;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.BeanPropertyWriter;

/* NOTE: This file is a modified copy of the class of the same fully-qualified
 * name from Jackson library. It was modified so null values are still going
 * through the flow as opposed to return right away, so default values
 * can be resolved (i.e., inclusion: NON_DEFAULT).
 *
 * Should be deleted if a better approach is found.
 */

/**
 * Property writer sub-class used for handling element wrapping needed for
 * serializing collection (array, Collection; possibly Map) types.
 */
public class XmlBeanPropertyWriter
    extends BeanPropertyWriter
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Config settings
    /**********************************************************
     */

    /**
     * Element name used as wrapper for collection.
     */
    protected final QName _wrapperQName;

    /**
     * Element name used for items in the collection
     */
    protected final QName _wrappedQName;

    /*
    /**********************************************************
    /* Life-cycle: construction, configuration
    /**********************************************************
     */

    public XmlBeanPropertyWriter(BeanPropertyWriter wrapped,
            PropertyName wrapperName, PropertyName wrappedName) {
        this(wrapped, wrapperName, wrappedName, null);
    }

    public XmlBeanPropertyWriter(BeanPropertyWriter wrapped,
            PropertyName wrapperName, PropertyName wrappedName,
            JsonSerializer<Object> serializer)
    {
        super(wrapped);
        _wrapperQName = _qname(wrapperName);
        _wrappedQName = _qname(wrappedName);

        if (serializer != null) {
            assignSerializer(serializer);
        }
    }

    private QName _qname(PropertyName n)
    {
        var ns = n.getNamespace();
        if (ns == null) {
            ns = "";
        }
        return new QName(ns, n.getSimpleName());
    }

    /*
    /**********************************************************
    /* Overridden methods
    /**********************************************************
     */

    /**
     * Overridden version so that we can wrap output within wrapper element if
     * and as necessary.
     */
    @Override
    public void serializeAsField(Object bean, JsonGenerator jgen, SerializerProvider prov)
        throws Exception
    {
        var value = get(bean);
        final var xmlGen = (jgen instanceof ToXmlGenerator) ? (ToXmlGenerator) jgen : null;

        // then find serializer to use
        var ser = _serializer;
        if (ser == null
                /*added*/ && value != null
                ) {
            Class<?> cls = value.getClass();
            var map = _dynamicSerializers;
            ser = map.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(map, cls, prov);
            }
        }
        // and then see if we must suppress certain values (default, empty)
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (/*added*/ value != null
                        && ser.isEmpty(prov, value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }
        // For non-nulls: simple check for direct cycles
        // NOTE: method signature here change 2.3->2.4
        if ((value == bean) && _handleSelfReference(bean, jgen, prov, ser)) {
            return;
        }

        // Ok then; addition we want to do is to add wrapper element, and that's what happens here
        // 19-Aug-2013, tatu: ... except for those nasty 'convertValue()' calls...
        if (xmlGen != null) {
            xmlGen.startWrappedValue(_wrapperQName, _wrappedQName);
        }
        if (value instanceof Collection coll && coll.isEmpty()) {
            xmlGen.writeRaw("");
        } else if (value != null) {
            jgen.writeFieldName(_name);
            if (_typeSerializer == null) {
                ser.serialize(value, jgen, prov);
            } else {
                ser.serializeWithType(value, jgen, prov, _typeSerializer);
            }
        }
        if (xmlGen != null) {
            xmlGen.finishWrappedValue(_wrapperQName, _wrappedQName);
        }
    }
}
