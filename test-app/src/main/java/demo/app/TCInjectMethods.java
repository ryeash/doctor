package demo.app;

import org.testng.Assert;

import javax.inject.Inject;
import javax.inject.Singleton;
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
