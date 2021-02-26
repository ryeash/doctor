package demo.app;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import vest.doctor.Async;

@Singleton
public class TCInjectedMethodsC {

    public CoffeeMaker coffeeMaker;
    public boolean injectedEmpty = false;
    public String injectAsync = null;

    @Inject
    public void injectedDependency(CoffeeMaker defaultCoffeeMaker) {
        this.coffeeMaker = defaultCoffeeMaker;
    }

    @Inject
    public void injectNothing() {
        injectedEmpty = !injectedEmpty;
    }

    @Inject
    @Async
    public void injectAsync() {
        this.injectAsync = Thread.currentThread().getName();
    }

    @Inject
    @Async
    public void anotherAsyncInject() {
        System.out.println("another asynchronous initializer");
    }
}
