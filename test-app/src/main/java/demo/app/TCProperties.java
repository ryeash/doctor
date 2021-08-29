package demo.app;

import demo.app.dao.DBProps;
import jakarta.inject.Inject;
import org.testng.Assert;
import vest.doctor.Property;
import vest.doctor.Prototype;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Prototype
public class TCProperties {

    @Inject
    public TCProperties(@Property("string") String stringProp,
                        @Property("string") char c,
                        @Property("boolean") Boolean bool,
                        @Property("boolean") boolean primBool,
                        @Property("string") Optional<String> optionalString,
                        @Property("non-existent-property") Optional<Integer> otherThing,
                        @Property("string") TCStaticStringConverter converted) {
        Assert.assertEquals(stringProp, "value");
        Assert.assertEquals(c, 'v');
        Assert.assertTrue(bool);
        Assert.assertTrue(primBool);
        Assert.assertEquals(optionalString.get(), "value");
        Assert.assertEquals((int) otherThing.orElse(-1), -1);
        Assert.assertEquals(converted.getValue(), "value");
    }

    @Inject
    public void injectProperties(@Property("list") List<String> stringList,
                                 @Property("list") Collection<Integer> numberList,
                                 @Property("set") Set<String> set) {
        Assert.assertEquals(stringList, List.of("42", "12", "97"));
        Assert.assertEquals(numberList, List.of(42, 12, 97));
        Assert.assertEquals(set, List.of("one", "two", "three"));
    }

    @Inject
    public void mixedProvidersAndProperties(CoffeeMaker coffeeMaker,
                                            @Property("number") BigInteger bigint) {
        Assert.assertNotNull(coffeeMaker);
        Assert.assertEquals(bigint, new BigInteger("42"));
    }

    @Inject
    public void propertiesInterfaceImplementation(TCPropertiesIntfc props) {
        Assert.assertEquals(props.stringProp(), "value");
        Assert.assertEquals(props.alias(), "value");
        Assert.assertEquals(props.stringPropOpt().orElse(null), "value");
        Assert.assertFalse(props.otherThing().isPresent());
        Assert.assertEquals(props.number().intValue(), 42);
        Assert.assertEquals(props.stringList(), List.of("42", "12", "97"));
        Assert.assertEquals(props.numberList(), List.of(42, 12, 97));
    }

    @Inject
    public void propertiesWithPrefix(DBProps dbProps) {
        Assert.assertEquals(dbProps.username(), "unused");
        Assert.assertEquals(dbProps.password(), "nothing");
    }
}
