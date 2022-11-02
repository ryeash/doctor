package demo.app;

import jakarta.inject.Named;
import vest.doctor.Configuration;
import vest.doctor.Eager;
import vest.doctor.Factory;
import vest.doctor.Modules;

@Configuration
@Modules("test")
public class TestAppConfigModuleInheritance {

    @Factory
    @Named("class-level-module")
    @Eager
    public CoffeeMaker testCoffeeMakerQualified() {
        return () -> "module-qualified";
    }
}
