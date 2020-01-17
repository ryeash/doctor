package demo.app;

import vest.doctor.Property;

import java.util.Optional;

public interface TCPropertiesIntfcParent {

    @Property("string")
    String stringProp();

    @Property("string")
    Optional<String> stringPropOpt();
}
