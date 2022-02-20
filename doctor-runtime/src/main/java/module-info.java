module doctor.runtime {
    uses vest.doctor.ApplicationLoader;

    requires transitive doctor.core;

    requires jakarta.inject;
    requires org.slf4j;
    requires com.fasterxml.jackson.dataformat.toml;
    requires com.fasterxml.jackson.core;

    exports vest.doctor.runtime;
}