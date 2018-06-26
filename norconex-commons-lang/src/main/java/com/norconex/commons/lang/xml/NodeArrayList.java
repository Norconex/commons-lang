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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Objects;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Wrapper around {@link NodeList} so it can be treated as an {@link ArrayList}.
 * @author Pascal Essiembre
 * @since 2.0.0
 */
public class NodeArrayList extends AbstractList<Node> implements NodeList {

    private final NodeList nodeList;
    
    /**
     * Creates a node ArrayList from the given NodeList. 
     * @param nodeList a node list
     */
    public NodeArrayList(NodeList nodeList) {
        super();
        this.nodeList = Objects.requireNonNull(
                nodeList, "NodeList cannot be null.");
    }
    /**
     * Creates a node ArrayList from the given Node children. 
     * @param node node to get the children list
     */
    public NodeArrayList(Node node) {
        super();
        this.nodeList = Objects.requireNonNull(
                node, "Node cannot be null.").getChildNodes();
    }
    
    @Override
    public Node item(int index) {
        return nodeList.item(index);
    }

    @Override
    public int getLength() {
        return nodeList.getLength();
    }

    @Override
    public Node get(int index) {
        return nodeList.item(index);
    }

    @Override
    public int size() {
        return nodeList.getLength();
    }
    
    @Override
    public boolean equals(final Object other) {
        return nodeList.equals(other);
    }
    @Override
    public int hashCode() {
        return nodeList.hashCode();
    }
}
