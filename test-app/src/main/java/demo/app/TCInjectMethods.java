package demo.app;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.testng.Assert;

import java.io.InputStream;

@Singleton
public class TCInjectMethods {

    public boolean injected = false;

    @Inject
    public void injected(InputStream io) {
        Assert.assertNotNull(io);
        injected = true;
    }
}
