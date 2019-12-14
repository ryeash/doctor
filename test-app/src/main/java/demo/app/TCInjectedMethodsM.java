package demo.app;

import vest.doctor.ThreadLocal;

import javax.inject.Inject;

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
