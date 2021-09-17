import vest.doctor.processing.ProcessorConfiguration;

module doctor.processor {
    uses ProcessorConfiguration;

    requires static java.compiler;

    requires transitive doctor.core;

    requires jakarta.inject;
}