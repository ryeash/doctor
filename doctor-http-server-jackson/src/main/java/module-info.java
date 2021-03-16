module doctor.http.server.jackson {
    requires doctor.http.server;
    requires com.fasterxml.jackson.databind;
    requires io.netty.codec.http;
    requires org.slf4j;
    requires io.netty.buffer;

    exports vest.doctor.http.jackson;
}