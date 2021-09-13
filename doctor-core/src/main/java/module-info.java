module doctor.core {
    requires static java.compiler;

    requires jakarta.inject;

    exports vest.doctor;
    exports vest.doctor.codegen;
    exports vest.doctor.aop;
    exports vest.doctor.event;
    exports vest.doctor.scheduled;
    exports vest.doctor.processing;
}