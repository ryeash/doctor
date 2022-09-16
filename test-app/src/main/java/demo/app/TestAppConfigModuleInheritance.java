package demo.app;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import vest.doctor.Eager;
import vest.doctor.Factory;
import vest.doctor.Modules;

@Singleton
@Modules("test")
public class TestAppConfigModuleInheritance {

    @Factory
    @Named("class-level-module")
    @Eager
    public CoffeeMaker testCoffeeMakerQualified() {
        return () -> "module-qualified";
    }
}
