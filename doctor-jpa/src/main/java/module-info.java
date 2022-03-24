module doctor.jpa {
    requires static java.compiler;

    requires doctor.core;
    requires jakarta.inject;
    requires jakarta.persistence;

    exports vest.doctor.jpa;
}