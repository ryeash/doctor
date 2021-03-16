module doctor.http.server.jackson {
    requires com.fasterxml.jackson.databind;
    requires doctor.http.server;
    requires io.netty.buffer;
    requires io.netty.codec.http;
    requires jakarta.inject;
    requires org.slf4j;

    exports vest.doctor.http.jackson;
}