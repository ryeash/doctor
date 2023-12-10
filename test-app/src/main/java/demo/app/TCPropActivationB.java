package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.PropertyActivation;

@Singleton
@PropertyActivation(name = "enable.b", value = "true")
@PropertyActivation(name = "list", value = "12")
public class TCPropActivationB {
}
