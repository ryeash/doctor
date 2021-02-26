package demo.app;

import jakarta.inject.Named;
import vest.doctor.Prototype;

@Prototype
@Named("name-${qualifierInterpolation}")
public class TCQualifierInterpolation {
}
