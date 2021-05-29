package com.norconex.commons.lang.xml.flow;

import java.util.function.Consumer;

import com.norconex.commons.lang.map.Properties;
import com.norconex.commons.lang.xml.IXMLConfigurable;
import com.norconex.commons.lang.xml.XML;

public class MockLowercaseConsumer
        implements Consumer<Properties>, IXMLConfigurable {
    private String field;
    @Override
    public void accept(Properties p) {
        p.set(field, p.getString(field, "").toLowerCase());
    }
    @Override
    public void loadFromXML(XML xml) {
        field = xml.getString("field");
    }
    @Override
    public void saveToXML(XML xml) {
        xml.addElement("field", field);
    }
}