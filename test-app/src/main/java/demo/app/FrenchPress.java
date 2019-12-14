package demo.app;

import vest.doctor.Prototype;

@Prototype
public class FrenchPress implements CoffeeMaker {
    @Override
    public String brew() {
        return "french pressing";
    }
}
