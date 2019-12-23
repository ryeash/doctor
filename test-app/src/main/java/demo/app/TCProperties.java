package demo.app;

import org.testng.Assert;
import vest.doctor.Property;
import vest.doctor.Prototype;

import javax.inject.Inject;
import java.math.BigInteger;
import java.util.Arrays;
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
                        @Property("none-existent-property") Optional<Integer> otherThing) {
        Assert.assertEquals(stringProp, "value");
        Assert.assertEquals(c, 'v');
        Assert.assertTrue(bool);
        Assert.assertTrue(primBool);
        Assert.assertEquals(optionalString.get(), "value");
        Assert.assertEquals((int) otherThing.orElse(-1), -1);
    }

    @Inject
    public void injectProperties(@Property("list") List<String> stringList,
                                 @Property("list") List<Integer> numberList,
                                 @Property("set") Set<String> set) {
        Assert.assertEquals(stringList, Arrays.asList("42", "12", "97"));
        Assert.assertEquals(numberList, Arrays.asList(42, 12, 97));
        Assert.assertEquals(set, Arrays.asList("one", "two", "three"));
    }

    @Inject
    public void mixedProvidersAndProperties(CoffeeMaker coffeeMaker,
                                            @Property("number") BigInteger bigint) {
        Assert.assertNotNull(coffeeMaker);
        Assert.assertEquals(bigint, new BigInteger("42"));
    }
}
