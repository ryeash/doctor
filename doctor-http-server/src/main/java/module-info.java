module doctor.http.server {
    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.codec;
    requires io.netty.handler;
    requires io.netty.transport;
    requires io.netty.common;
    requires org.slf4j;

    requires doctor.core;

    exports vest.doctor.http.server;
    exports vest.doctor.http.server.impl;
}