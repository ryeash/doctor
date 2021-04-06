package demo.app;

import jakarta.inject.Named;
import vest.doctor.ThreadLocal;

@ThreadLocal
@Named("pourOver")
public class PourOver implements CoffeeMaker {

    @Override
    public String brew() {
        return "pouring over";
    }
}
