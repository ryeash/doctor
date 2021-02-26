package demo.app;

import jakarta.inject.Inject;
import vest.doctor.ThreadLocal;

@ThreadLocal
public class TCInjectedMethodsM {

    public CoffeeMaker coffeeMaker;
    public boolean injectedEmpty = false;

    @Inject
    public void injectedDependency(CoffeeMaker defaultCoffeeMaker) {
        this.coffeeMaker = defaultCoffeeMaker;
    }

    @Inject
    public void injectNothing() {
        injectedEmpty = !injectedEmpty;
    }
}
