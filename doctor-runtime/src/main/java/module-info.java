module doctor.runtime {
    uses vest.doctor.ApplicationLoader;

    requires transitive doctor.core;

    requires jakarta.inject;
    requires org.slf4j;

    exports vest.doctor.runtime;
}