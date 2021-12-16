package vest.doctor.test;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import vest.doctor.Factory;
import vest.doctor.Modules;
import vest.doctor.Primary;
import vest.doctor.Prototype;

@Singleton
@Modules("animals")
public class AnimalConfig {
    @Factory
    @Prototype
    @Named("dog")
    @Primary
    public Dog dog() {
        return new Dog();
    }

    @Factory
    @Prototype
    @AnimalColor(color = AnimalColor.Color.BLUE, name = "hank")
    public Dog blueDog() {
        return new Dog("blue-bark");
    }
}
