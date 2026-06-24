package tools.jackson.dataformat.xml.ser;

import java.util.Collection;

import javax.xml.namespace.QName;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.ser.*;
import tools.jackson.databind.ser.impl.PropertySerializerMap;

/* NOTE: This file is a modified copy of the class of the same fully-qualified
 * name from the Jackson library, kept in sync with the Jackson version this
 * project depends on. The ONLY functional change vs the upstream class is in
 * serializeAsProperty(): null values are NOT short-circuited but still flow
 * through the wrapper so that default-value resolution (JsonInclude.NON_DEFAULT)
 * works, and empty collections are emitted as an empty wrapper. Everything else
 * (including the @since 3.2 dynamic-wrapping support and the #27 wrapped-name
 * fix) is a verbatim copy.
 */
/**
 * Property writer sub-class used for handling element wrapping needed for serializing
 * collection (array, Collection; not Map) types.
 */
public class XmlBeanPropertyWriter
    extends BeanPropertyWriter
{
    /*
    /**********************************************************************
    /* Config settings
    /**********************************************************************
     */

    /**
     * Element name used as wrapper for collection.
     */
    protected final QName _wrapperQName;

    /**
     * Element name used for items in the collection
     */
    protected final QName _wrappedQName;

    /**
     * Whether wrapping should be checked dynamically based on the runtime value type.
     * When {@code true}, wrapping is only applied if the runtime value is actually
     * a Collection, Iterable, or array. Used for properties declared as {@code Object}
     * that may or may not contain a collection at runtime.
     *
     * @since 3.2
     */
    protected final boolean _dynamicWrapping;

    /*
    /**********************************************************************
    /* Life-cycle: construction, configuration
    /**********************************************************************
     */

    /**
     * @deprecated Since 3.2
     */
    @Deprecated
    public XmlBeanPropertyWriter(BeanPropertyWriter wrapped,
            PropertyName wrapperName, PropertyName wrappedName) {
        this(wrapped, wrapperName, wrappedName, null, false);
    }

    /**
     * @deprecated Since 3.2
     */
    @Deprecated
    public XmlBeanPropertyWriter(BeanPropertyWriter wrapped,
            PropertyName wrapperName, PropertyName wrappedName,
            ValueSerializer<Object> serializer)
    {
        this(wrapped, wrapperName, wrappedName, serializer, false);
    }

    /**
     * @since 3.2
     */
    public XmlBeanPropertyWriter(BeanPropertyWriter wrapped,
            PropertyName wrapperName, PropertyName wrappedName,
            boolean dynamicWrapping)
    {
        this(wrapped, wrapperName, wrappedName, null, dynamicWrapping);
    }

    private XmlBeanPropertyWriter(BeanPropertyWriter wrapped,
            PropertyName wrapperName, PropertyName wrappedName,
            ValueSerializer<Object> serializer, boolean dynamicWrapping)
    {
        super(wrapped);
        _wrapperQName = _qname(wrapperName);
        _wrappedQName = _qname(wrappedName);
        _dynamicWrapping = dynamicWrapping;

        if (serializer != null) {
            assignSerializer(serializer);
        }
    }

    private QName _qname(PropertyName n)
    {
        String ns = n.getNamespace();
        if (ns == null) {
            ns = "";
        }
        return new QName(ns, n.getSimpleName());
    }

    /*
    /**********************************************************************
    /* Overridden methods
    /**********************************************************************
     */

    /**
     * Overridden version so that we can wrap output within wrapper element if
     * and as necessary.
     */
    @Override
    public void serializeAsProperty(Object bean, JsonGenerator g, SerializationContext ctxt)
        throws Exception
    {
        Object value = get(bean);

        // [dataformat-xml#8]: For dynamic wrapping (Object-typed properties),
        // check runtime type and delegate to standard handling if not a collection
        if (_dynamicWrapping && (value == null || !_isIndexedValue(value))) {
            super.serializeAsProperty(bean, g, ctxt);
            return;
        }

        // NORCONEX: Unlike upstream, null values are NOT short-circuited here. They
        // still flow through the wrapper below (without writing a value) so that
        // default-value resolution (e.g. JsonInclude.Include.NON_DEFAULT) works;
        // empty collections are emitted as an empty wrapper.

        // then find serializer to use (only needed for non-null values)
        ValueSerializer<Object> ser = _serializer;
        if (ser == null && value != null) {
            Class<?> cls = value.getClass();
            PropertySerializerMap map = _dynamicSerializers;
            ser = map.serializerFor(cls);
            if (ser == null) {
                ser = _findAndAddDynamic(map, cls, ctxt);
            }
        }
        // and then see if we must suppress certain values (default, empty)
        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (value != null && ser.isEmpty(ctxt, value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }
        // For non-nulls: simple check for direct cycles
        if (value == bean) {
            if (_handleSelfReference(bean, g, ctxt, ser)) {
                return;
            }
        }

        final ToXmlGenerator xmlGen = (g instanceof ToXmlGenerator) ? (ToXmlGenerator) g : null;
        // Ok then; addition we want to do is to add wrapper element, and that's what happens here
        if (xmlGen != null) {
            xmlGen.startWrappedValue(_wrapperQName, _wrappedQName);
        }
        if (value instanceof Collection<?> coll && coll.isEmpty()) {
            xmlGen.writeRaw("");
        } else if (value != null) {
            // [dataformat-xml#27]: Use wrapped name (inner element name), not property
            // name which may be the wrapper name after introspector conflict resolution
            g.writeName(_wrappedQName.getLocalPart());
            if (_typeSerializer == null) {
                ser.serialize(value, g, ctxt);
            } else {
                ser.serializeWithType(value, g, ctxt, _typeSerializer);
            }
        }
        if (xmlGen != null) {
            xmlGen.finishWrappedValue(_wrapperQName, _wrappedQName);
        }
    }

    /**
     * Check if the runtime value is a Collection, array, or Iterable
     * (i.e. something that should get wrapper element handling).
     */
    private static boolean _isIndexedValue(Object value) {
        return (value instanceof java.util.Collection<?>)
                || (value instanceof Iterable<?>)
                || value.getClass().isArray();
    }
}
