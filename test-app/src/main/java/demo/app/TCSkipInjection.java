package demo.app;

import jakarta.inject.Inject;

public class TCSkipInjection {
    public boolean injected = false;

    @Inject
    public void postConstruct() {
        injected = true;
    }
}
