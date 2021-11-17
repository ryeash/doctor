package demo.app;

import vest.doctor.test.AbstractDoctorTest;
import vest.doctor.test.TestConfiguration;

@TestConfiguration(
        propertyFiles = {"netty-test.props", "test-override.props", "test.props"},
        configurationBuilder = CustomConfigBuilder.class)
public abstract class AbstractTestAppTest extends AbstractDoctorTest {

    static {
        System.setProperty("qualifierInterpolation", "interpolated");
    }
}
