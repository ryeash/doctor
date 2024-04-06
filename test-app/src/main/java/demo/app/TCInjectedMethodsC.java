package demo.app;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

@Singleton
public class TCInjectedMethodsC {

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
