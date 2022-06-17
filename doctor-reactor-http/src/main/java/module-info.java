module doctor.reactor.http {
    uses vest.doctor.reactor.http.RouteOrchestration;

    requires static java.compiler;
    requires doctor.core;
    requires doctor.runtime;
    requires doctor.reactive;
    requires jakarta.inject;

    requires io.netty.common;
    requires io.netty.codec;
    requires io.netty.codec.http;
    requires io.netty.buffer;
    requires io.netty.handler;
    requires io.netty.transport;

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires doctor.http.server;

    exports vest.doctor.reactor.http;
    exports vest.doctor.reactor.http.impl;
    exports vest.doctor.reactor.http.jackson;
}