package demo.app;

import org.testng.annotations.Test;
import vest.doctor.test.AbstractDoctorTest;
import vest.doctor.test.TestConfiguration;

@TestConfiguration(
        modules = "test",
        propertyFiles = {"test-override.props", "test.props"})
public class TestModuleTest extends AbstractDoctorTest {

    @Test
    public void modules() {
        assertEquals(providerRegistry().getInstance(CoffeeMaker.class, "modules-test").brew(), "test");
        assertEquals(providerRegistry().getInstance(CoffeeMaker.class, "class-level-module").brew(), "module-qualified");
    }
}
