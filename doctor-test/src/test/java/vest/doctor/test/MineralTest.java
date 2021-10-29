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

    @Test
    public void granite() {
        assertEquals(granite.hardness(), 7);
    }

    @Test
    public void diamond() {
        assertEquals(diamond.hardness(), 10);
    }
}
