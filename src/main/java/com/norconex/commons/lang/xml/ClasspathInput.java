/* Copyright 2017-2022 Norconex Inc.
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
package com.norconex.commons.lang.xml;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

import org.w3c.dom.ls.LSInput;

import lombok.extern.slf4j.Slf4j;

/**
 * Load XML Schema resources input from Classpath.
 * @author Pascal Essiembre
 * @since 1.13.0
 */
@Slf4j
public class ClasspathInput implements LSInput {

    private String publicId;
    private String systemId;
    private BufferedInputStream inputStream;

    public ClasspathInput(String publicId, String sysId, InputStream input) {
        this.publicId = publicId;
        systemId = sysId;
        inputStream = new BufferedInputStream(input);
    }

    @Override
    public String getPublicId() {
        return publicId;
    }

    @Override
    public void setPublicId(String publicId) {
        this.publicId = publicId;
    }

    @Override
    public String getBaseURI() {
        return null;
    }

    @Override
    public InputStream getByteStream() {
        return null;
    }

    @Override
    public boolean getCertifiedText() {
        return false;
    }

    @Override
    public Reader getCharacterStream() {
        return null;
    }

    @Override
    public String getEncoding() {
        return null;
    }

    @Override
    public String getStringData() {
        synchronized (inputStream) {
            try {
                byte[] input = new byte[inputStream.available()];
                inputStream.read(input); //NOSONAR
                return new String(input);
            } catch (IOException e) {
                LOG.error("Could not get string data.", e);
                return null;
            }
        }
    }

    @Override
    public void setBaseURI(String baseURI) {
        //NOOP
    }

    @Override
    public void setByteStream(InputStream byteStream) {
        //NOOP
    }

    @Override
    public void setCertifiedText(boolean certifiedText) {
        //NOOP
    }

    @Override
    public void setCharacterStream(Reader characterStream) {
        //NOOP
    }

    @Override
    public void setEncoding(String encoding) {
        //NOOP
    }

    @Override
    public void setStringData(String stringData) {
        //NOOP
    }

    @Override
    public String getSystemId() {
        return systemId;
    }

    @Override
    public void setSystemId(String systemId) {
        this.systemId = systemId;
    }

    public BufferedInputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(BufferedInputStream inputStream) {
        this.inputStream = inputStream;
    }
}