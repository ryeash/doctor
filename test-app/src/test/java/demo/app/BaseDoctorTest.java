package demo.app;

import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import vest.doctor.DefaultConfigurationFacade;
import vest.doctor.Doctor;
import vest.doctor.MapConfigurationSource;

public abstract class BaseDoctorTest extends Assert {

    public static Doctor doctor;

    @BeforeSuite(alwaysRun = true)
    public void start() {
        if (doctor == null) {
            System.setProperty("qualifierInterpolation", "interpolated");
            System.setProperty("doctor.app.properties", "test-override.props,test.props");

            doctor = Doctor.load(DefaultConfigurationFacade.defaultConfigurationFacade()
                    .addSource(new MapConfigurationSource(
                            "doctor.netty.bind", "localhost:8081"))
                    .addSource(new TCConfigReload()));
        }
    }
}
