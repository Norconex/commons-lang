package com.norconex.commons.lang.config;

import java.io.IOException;
import java.io.Reader;
import java.io.Serializable;
import java.io.Writer;

/**
 * Provides indications that a class is configurable via XML.  Classes
 * implementing this should be carefull to document XML configuration options
 * propertly (e.g. in Javadoc).
 * @author Pascal Essiembre
 */
public interface IXMLConfigurable extends Serializable {

    void loadFromXML(Reader in) throws IOException;
    void saveToXML(Writer out) throws IOException;
}
