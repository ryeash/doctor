module doctor.processor {
    uses vest.doctor.ProcessorConfiguration;

    requires static java.compiler;

    requires transitive doctor.core;

    requires jakarta.inject;
}