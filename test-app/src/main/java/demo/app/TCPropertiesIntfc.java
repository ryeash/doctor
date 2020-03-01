package demo.app;

import vest.doctor.Properties;
import vest.doctor.Property;

import javax.inject.Singleton;
import java.util.List;
import java.util.Optional;

@Singleton
@Properties
public interface TCPropertiesIntfc extends TCPropertiesIntfcParent {

    @Property("number")
    Integer number();

    @Property("non-existent-property$$$$")
    Optional<Integer> otherThing();

    @Property("list")
    List<String> stringList();

    @Property("list")
    List<Integer> numberList();
}
