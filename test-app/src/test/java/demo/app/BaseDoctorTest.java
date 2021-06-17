package demo.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.BeforeSuite;
import vest.doctor.runtime.DefaultConfigurationFacade;
import vest.doctor.runtime.Doctor;
import vest.doctor.runtime.MapConfigurationSource;

import java.util.Map;

public abstract class BaseDoctorTest extends Assert {

    protected final Logger log = LoggerFactory.getLogger(getClass());
    public static Doctor doctor;

    @BeforeSuite(alwaysRun = true)
    public void start() {
        if (doctor == null) {
            System.setProperty("qualifierInterpolation", "interpolated");
            System.setProperty("doctor.app.properties", "test-override.props,test.props");

            doctor = Doctor.load(DefaultConfigurationFacade.defaultConfigurationFacade()
                    .addSource(new MapConfigurationSource(Map.of(
                            "doctor.netty.http.bind", "localhost:61233")))
                    .addSource(new TCConfigReload()));
        }
    }
}
