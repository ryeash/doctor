package app.ext;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import vest.doctor.Factory;

@Singleton
public class ExternalAppConfig {

    @Factory
    @Named("spring")
    public Widget springWidget() {
        return new ConfigurableWidget("spring");
    }
}
