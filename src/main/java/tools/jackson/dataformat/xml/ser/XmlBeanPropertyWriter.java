package tools.jackson.dataformat.xml.ser;

import java.util.Collection;

import javax.xml.namespace.QName;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.PropertyName;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.BeanPropertyWriter;

/* NOTE: This file is a modified copy of the class of the same fully-qualified
 * name from Jackson library. It was modified so null values are still going
 * through the flow as opposed to return right away, so default values
 * can be resolved (i.e., inclusion: NON_DEFAULT).
 */
public class XmlBeanPropertyWriter extends BeanPropertyWriter {

    private static final long serialVersionUID = 1L;

    protected final QName _wrapperQName;
    protected final QName _wrappedQName;

    public XmlBeanPropertyWriter(
            BeanPropertyWriter wrapped,
            PropertyName wrapperName,
            PropertyName wrappedName) {
        this(wrapped, wrapperName, wrappedName, null);
    }

    public XmlBeanPropertyWriter(
            BeanPropertyWriter wrapped,
            PropertyName wrapperName,
            PropertyName wrappedName,
            ValueSerializer<Object> serializer) {
        super(wrapped);
        _wrapperQName = _qname(wrapperName);
        _wrappedQName = _qname(wrappedName);

        if (serializer != null) {
            assignSerializer(serializer);
        }
    }

    private QName _qname(PropertyName name) {
        var namespace = name.getNamespace();
        if (namespace == null) {
            namespace = "";
        }
        return new QName(namespace, name.getSimpleName());
    }

    @Override
    public void serializeAsProperty(
            Object bean,
            JsonGenerator jgen,
            SerializationContext prov) throws Exception {
        var value = get(bean);
        final var xmlGen = (jgen instanceof ToXmlGenerator)
                ? (ToXmlGenerator) jgen
                : null;

        var serializer = _serializer;
        if (serializer == null && value != null) {
            Class<?> cls = value.getClass();
            var map = _dynamicSerializers;
            serializer = map.serializerFor(cls);
            if (serializer == null) {
                serializer = _findAndAddDynamic(map, cls, prov);
            }
        }

        if (_suppressableValue != null) {
            if (MARKER_FOR_EMPTY == _suppressableValue) {
                if (value != null && serializer.isEmpty(prov, value)) {
                    return;
                }
            } else if (_suppressableValue.equals(value)) {
                return;
            }
        }

        if ((value == bean)
                && _handleSelfReference(bean, jgen, prov, serializer)) {
            return;
        }

        if (xmlGen != null) {
            xmlGen.startWrappedValue(_wrapperQName, _wrappedQName);
        }
        if (value instanceof Collection<?> coll && coll.isEmpty()) {
            xmlGen.writeRaw("");
        } else if (value != null) {
            jgen.writeName(_name);
            if (_typeSerializer == null) {
                serializer.serialize(value, jgen, prov);
            } else {
                serializer.serializeWithType(value, jgen, prov,
                        _typeSerializer);
            }
        }
        if (xmlGen != null) {
            xmlGen.finishWrappedValue(_wrapperQName, _wrappedQName);
        }
    }
}