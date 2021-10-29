module doctor.http.server {
    requires static java.compiler;

    requires transitive doctor.core;

    requires jakarta.inject;
    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.codec;
    requires io.netty.handler;
    requires io.netty.transport;
    requires io.netty.common;
    requires org.slf4j;
    requires doctor.runtime;
    requires doctor.netty.bundle;

    exports vest.doctor.http.server;
    exports vest.doctor.http.server.impl;
    exports vest.doctor.http.server.rest;
}