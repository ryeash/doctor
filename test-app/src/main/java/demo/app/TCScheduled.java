package demo.app;

import org.testng.Assert;
import vest.doctor.Prototype;
import vest.doctor.scheduled.Scheduled;

import java.util.concurrent.atomic.AtomicInteger;

@Prototype
public class TCScheduled {

    public AtomicInteger every10Milliseconds = new AtomicInteger(0);
    public AtomicInteger every50Milliseconds = new AtomicInteger(0);
    public AtomicInteger cronEverySecond = new AtomicInteger(0);

    @Scheduled(interval = "${configurableInterval}")
    public void every10Milliseconds() {
        every10Milliseconds.incrementAndGet();
    }

    @Scheduled(interval = "50", type = Scheduled.Type.FIXED_DELAY)
    public void every50Milliseconds(CoffeeMaker coffeeMaker) {
        Assert.assertNotNull(coffeeMaker);
        every50Milliseconds.incrementAndGet();
    }

    @Scheduled(cron = "* * * * * *")
    public Integer cronEverySecond() {
        return cronEverySecond.incrementAndGet();
    }
}
