package demo.app;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

@Singleton
public class TCParamterizedInject {
    private final ParameterizedThing<String> injectedList;

    @Inject
    public TCParamterizedInject(@Named("string") ParameterizedThing<String> injectedList) {
        this.injectedList = injectedList;
    }

    public ParameterizedThing<String> getInjectedList() {
        return injectedList;
    }
}
