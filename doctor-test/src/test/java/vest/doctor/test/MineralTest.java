package vest.doctor.test;

import jakarta.inject.Inject;
import org.testng.annotations.Test;

@TestConfiguration(
        modules = {"minerals"},
        configurationBuilder = TestConfigurationFacadeBuilder.class)
public class MineralTest extends AbstractDoctorTest {

    @Inject
    private Granite granite;

    @Inject
    private Diamond diamond;

    private boolean injectedMethod = false;

    @Inject
    private void injectedMethod() {
        injectedMethod = true;
    }

    @Test
    public void granite() {
        assertEquals(granite.hardness(), 7);
    }

    @Test
    public void diamond() {
        assertEquals(diamond.hardness(), 10);
    }

    @Test
    public void checkMethodInjection() {
        assertTrue(injectedMethod);
    }
}
