/* Copyright 2022 Norconex Inc.
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

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.apache.commons.lang3.StringUtils;

/**
 * XPath-related utility methods.
 * @since 3.0.0
 */
public final class XpathUtil {

    private XpathUtil() {
    }

    /**
     * Create an XPath attribute reference or an empty string if the
     * attribute name is blank.
     * @param attributeName the attribute name
     * @return the attribute name, prefixed with {@literal @}, or an
     *     empty string (never null)
     */
    public static String attr(String attributeName) {
        if (StringUtils.isNotBlank(attributeName)) {
            return "@" + attributeName;
        }
        return StringUtils.EMPTY;
    }

    /**
     * Gets a new XPath instance from the default object model.
     * @return new XPath instance
     */
    public static XPath newXPath() {
        // Consider caching w/ ThreadLocal if performance becomes a concern
        return XPathFactory.newInstance().newXPath();
    }

    /**
     * Gets a new compiled {@link XPathExpression} from the given string.
     * @param expression the XPath string
     * @return compiled XPath expression
     */
    public static XPathExpression newXPathExpression(String expression) {
        try {
            return newXPath().compile(expression);
        } catch (XPathExpressionException e) {
            throw new XmlException("Could not create XPath expression.", e);
        }
    }
}
