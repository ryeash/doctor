package demo.app;

import vest.doctor.ThreadLocal;

import javax.inject.Named;

@ThreadLocal
@Named("pourOver")
public class PourOver implements CoffeeMaker {

    @Override
    public String brew() {
        return "pouring over";
    }
}
