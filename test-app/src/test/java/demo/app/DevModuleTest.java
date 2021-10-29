package demo.app;

import org.testng.annotations.Test;
import vest.doctor.test.AbstractDoctorTest;
import vest.doctor.test.TestConfiguration;

@TestConfiguration(
        modules = "dev",
        propertyFiles = {"test-override.props", "test.props"})
public class DevModuleTest extends AbstractDoctorTest {

    @Test
    public void modules() {
        CoffeeMaker cm = providerRegistry().getInstance(CoffeeMaker.class, "modules-test");
        assertEquals(cm.brew(), "dev");
    }
}
