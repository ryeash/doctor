module doctor.core {
    requires static java.compiler;

    requires jakarta.inject;
    requires java.sql;

    exports vest.doctor;
    exports vest.doctor.codegen;
    exports vest.doctor.aop;
    exports vest.doctor.event;
    exports vest.doctor.scheduled;
    exports vest.doctor.processing;
    exports vest.doctor.conf;
    exports vest.doctor.jdbc;
    exports vest.doctor.reactive;
}
