/* Copyright 2018 Norconex Inc.
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

import java.util.ArrayList;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A null-safe wrapper around {@link NodeList} that is also
 * an {@link ArrayList}.
 *
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class NodeArrayList extends ArrayList<Node> implements NodeList {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a node ArrayList from the given NodeList.
     * A <code>null</code> NodeList results in an empty NodeArrayList.
     * @param nodeList a node list
     */
    public NodeArrayList(NodeList nodeList) {
        super();
        if (nodeList != null) {
            for (int i = 0; i < nodeList.getLength(); i++) {
                add(nodeList.item(i));
            }
        }
    }
    /**
     * Creates a node ArrayList from the given Node children.
     * A <code>null</code> node or no children results in an empty
     * NodeArrayList.
     * @param node node to get the children list
     */
    public NodeArrayList(Node node) {
        super();
        if (node != null) {
            NodeList nodeList = node.getChildNodes();
            if (nodeList != null) {
                for (int i = 0; i < nodeList.getLength(); i++) {
                    add(nodeList.item(i));
                }
            }
        }
    }

    @Override
    public Node item(int index) {
        return get(index);
    }

    @Override
    public int getLength() {
        return size();
    }
}
