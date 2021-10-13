package vest.doctor.test;

import org.testng.annotations.AfterClass;

@TestConfiguration(
        modules = {"animals"},
        propertyFiles = {"classpath:test-config.props"})
public class DuplicateConfigTest extends AbstractDoctorTest {

    @AfterClass(alwaysRun = true)
    public void verifyCaching() {
        assertEquals(DoctorInstanceManager.singleton().size(), 2);
    }
}
