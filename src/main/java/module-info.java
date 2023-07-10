
@SuppressWarnings({ "module" })
module com.norconex.commons.lang {
    exports com.norconex.commons.lang.map;
    exports com.norconex.commons.lang.jar;
    exports com.norconex.commons.lang.security;
    exports com.norconex.commons.lang.bean;
    exports com.norconex.commons.lang.xml;
    exports com.norconex.commons.lang.encrypt;
    exports com.norconex.commons.lang.file;
    exports com.norconex.commons.lang;
    exports com.norconex.commons.lang.io;
    exports com.norconex.commons.lang.text;
    exports com.norconex.commons.lang.img;
    exports com.norconex.commons.lang.xml.flow;
    exports com.norconex.commons.lang.convert;
    exports com.norconex.commons.lang.function;
    exports com.norconex.commons.lang.javadoc;
    exports com.norconex.commons.lang.exec;
    exports com.norconex.commons.lang.pipeline;
    exports com.norconex.commons.lang.event;
    exports com.norconex.commons.lang.url;
    exports com.norconex.commons.lang.unit;
    exports com.norconex.commons.lang.version;
    exports com.norconex.commons.lang.collection;
    exports com.norconex.commons.lang.time;
    exports com.norconex.commons.lang.config;
    exports com.norconex.commons.lang.xml.flow.impl;
    exports com.norconex.commons.lang.net;

    // Mark as "transitive" the ones that are publicly exposed to
    // this module consumers.
    requires com.fasterxml.jackson.annotation;
    requires icu4j;
    requires imgscalr.lib;
    requires jakarta.xml.bind;
    requires java.compiler;
    requires transitive java.desktop;
    requires transitive java.logging;
    requires transitive java.xml;
    requires jdk.compiler;
    requires transitive jdk.javadoc;
    requires json;
    requires lombok;
    requires org.apache.commons.collections4;
    requires org.apache.commons.io;
    requires org.apache.commons.lang3;
    requires org.apache.commons.text;
    requires transitive org.slf4j;
    requires transitive velocity.engine.core;
}