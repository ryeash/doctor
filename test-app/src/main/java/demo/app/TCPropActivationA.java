package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.PropertyActivation;

@Singleton
@PropertyActivation(name = "enable.a", value = "true")
public class TCPropActivationA {
}
