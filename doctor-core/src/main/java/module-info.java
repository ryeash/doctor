module doctor.core {
    requires static java.compiler;

    requires jakarta.inject;
    requires java.sql;
    requires org.slf4j;

    uses vest.doctor.ApplicationLoader;

    exports vest.doctor;
    exports vest.doctor.aop;
    exports vest.doctor.codegen;
    exports vest.doctor.conf;
    exports vest.doctor.event;
    exports vest.doctor.jdbc;
    exports vest.doctor.processing;
    exports vest.doctor.reactive;
    exports vest.doctor.runtime;
    exports vest.doctor.scheduled;

}
