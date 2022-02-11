module doctor.reactor.http {
    requires static java.compiler;
    requires doctor.core;
    requires doctor.runtime;
    requires jakarta.inject;

    requires io.netty.common;
    requires io.netty.codec;
    requires io.netty.codec.http;
    requires io.netty.buffer;
    requires io.netty.handler;
    requires io.netty.transport;

    requires reactor.netty.http;
    requires reactor.netty.core;
    requires org.reactivestreams;
    requires reactor.core;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;

    exports vest.doctor.reactor.http;
    exports vest.doctor.reactor.http.impl;
    exports vest.doctor.reactor.http.jackson;
}