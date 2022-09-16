package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.Activation;

@Singleton
@Activation(IsActivationPropertyPresent.class)
public class TCActivation {
}
