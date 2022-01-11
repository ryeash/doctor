module doctor.http.server {
    requires static java.compiler;

    requires transitive doctor.core;

    requires jakarta.inject;
    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.handler;
    requires io.netty.transport;
    requires io.netty.common;
    requires org.slf4j;
    requires doctor.netty.bundle;
    requires doctor.util;

    exports vest.doctor.http.server;
    exports vest.doctor.http.server.impl;
    exports vest.doctor.http.server.rest;
}