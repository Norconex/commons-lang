package com.norconex.commons.lang.config;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;
import org.junit.Assert;


/**
 * Convenience class for testing that a {@link IXMLConfigurable} instance
 * can be written, and read into an new instance that is 
 * {@link #equals(Object)}.
 * @author Pascal Essiembre
 */
public final class XMLConfigurableTester {

    private XMLConfigurableTester() {
        super();
    }

    public static void assertWriteRead(IXMLConfigurable xmlConfiurable)
            throws IOException, ConfigurationException {
        
        File tempFile = File.createTempFile("XMLConfigurableTester", ".xml");
        
        // Write
        FileWriter out = new FileWriter(tempFile);
        xmlConfiurable.saveToXML(out);
        out.close();
        
        // Read
        XMLConfiguration xml = new ConfigurationLoader().loadXML(tempFile);
        IXMLConfigurable readConfigurable = 
                (IXMLConfigurable) ConfigurationUtils.newInstance(xml);
        StringWriter w = new StringWriter();
        xml.save(w);
        StringReader r = new StringReader(w.toString());
        readConfigurable.loadFromXML(r);
        w.close();
        r.close();

        tempFile.delete();

        Assert.assertEquals("Saved and loaded XML are not the same.", 
                xmlConfiurable, readConfigurable);
    }

}
