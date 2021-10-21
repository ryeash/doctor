module doctor.netty.bundle {
    requires static java.compiler;

    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.codec;
    requires io.netty.handler;
    requires io.netty.transport;
    requires io.netty.common;
    requires org.slf4j;

    requires doctor.core;

    exports vest.doctor.netty.common;
}