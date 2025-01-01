module doctor.apache.http {
    requires org.apache.httpcomponents.core5.httpcore5;
    requires org.slf4j;

    exports vest.doctor.apache.server;
    exports vest.doctor.apache.server.impl;
}