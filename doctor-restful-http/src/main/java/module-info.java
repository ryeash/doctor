import vest.doctor.restful.http.RouteOrchestration;

module doctor.restful.http {
    uses RouteOrchestration;

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

    requires com.fasterxml.jackson.core;
    requires com.fasterxml.jackson.databind;
    requires org.slf4j;
    requires doctor.http.server;

    exports vest.doctor.restful.http;
    exports vest.doctor.restful.http.impl;
    exports vest.doctor.restful.http.jackson;
}