package demo.app;

import vest.doctor.Prototype;

import javax.inject.Named;

@Prototype
@Named("name-${qualifierInterpolation}")
public class TCQualifierInterpolation {
}
