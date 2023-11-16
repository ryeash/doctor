package demo.app;

import org.testng.annotations.BeforeMethod;
import vest.doctor.test.AbstractDoctorTest;
import vest.doctor.test.TestConfiguration;

import java.lang.reflect.Method;

@TestConfiguration(
        propertyFiles = {"netty-test.props", "test-override.props", "test.props"},
        configurationBuilder = CustomConfigBuilder.class)
public abstract class AbstractTestAppTest extends AbstractDoctorTest {

    static {
        System.setProperty("qualifierInterpolation", "interpolated");
    }

    @BeforeMethod
    public void beforeTestCase(Method m) {
        System.out.println("---------------------------------------------");
        System.out.println(m.getName());
        System.out.println("---------------------------------------------");
    }
}
