import vest.doctor.http.server.RouteOrchestration;

module doctor.http.server {
    requires doctor.core;
    requires java.compiler;
    requires jakarta.inject;

    uses RouteOrchestration;

    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.codec;
    requires io.netty.handler;
    requires io.netty.transport;
    requires io.netty.common;
    requires org.slf4j;

    exports vest.doctor.http.server;
    exports vest.doctor.http.server.impl;
    exports vest.doctor.http.server.processing;
}
