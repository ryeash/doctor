module doctor.netty.bundle {
    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires io.netty.codec;
    requires io.netty.handler;
    requires io.netty.transport;
    requires io.netty.common;
    requires org.slf4j;

    requires doctor.core;
    requires doctor.runtime;

    exports vest.doctor.netty.common;
}
