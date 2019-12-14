package demo.app;

import org.testng.Assert;
import vest.doctor.Scheduled;

import javax.inject.Singleton;
import java.util.concurrent.atomic.AtomicInteger;

@Singleton
public class TCScheduled {

    public AtomicInteger every10Milliseconds = new AtomicInteger(0);
    public AtomicInteger every50Milliseconds = new AtomicInteger(0);

    @Scheduled(period = 10)
    public void every10Milliseconds() {
        every10Milliseconds.incrementAndGet();
    }

    @Scheduled(period = 50, type = Scheduled.Type.FIXED_DELAY)
    public void event50Milliseconds(CoffeeMaker coffeeMaker) {
        Assert.assertNotNull(coffeeMaker);
        every50Milliseconds.incrementAndGet();
    }
}
