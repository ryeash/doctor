package demo.app;

import javax.inject.Inject;

public class TCSkipInjection {
    public boolean injected = false;

    @Inject
    public void postConstruct() {
        injected = true;
    }
}
