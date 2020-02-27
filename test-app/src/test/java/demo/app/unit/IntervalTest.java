package demo.app.unit;

import org.testng.Assert;
import org.testng.annotations.Test;
import vest.doctor.Interval;

import java.util.concurrent.TimeUnit;

public class IntervalTest extends Assert {

    @Test
    public void validate() {
        assertInterval("10", 10, TimeUnit.MILLISECONDS);
        assertInterval("10ms", 10, TimeUnit.MILLISECONDS);
        assertInterval("10 ms", 10, TimeUnit.MILLISECONDS);
        assertInterval("10       ms", 10, TimeUnit.MILLISECONDS);

        assertInterval("1us", 1, TimeUnit.MICROSECONDS);
        assertInterval("1s", 1, TimeUnit.SECONDS);
        assertInterval("1m", 1, TimeUnit.MINUTES);
        assertInterval("1d", 1, TimeUnit.DAYS);
        assertInterval("1h", 1, TimeUnit.HOURS);

        assertThrows(IllegalArgumentException.class, () -> new Interval(""));
        assertThrows(IllegalArgumentException.class, () -> new Interval("ms"));
        assertThrows(IllegalArgumentException.class, () -> new Interval("12cycles"));
    }

    private void assertInterval(String intervalString, long expectedMagnitude, TimeUnit expectedTimeUnit) {
        Interval i = new Interval(intervalString);
        assertEquals(i.getMagnitude(), expectedMagnitude);
        assertEquals(i.getUnit(), expectedTimeUnit);
    }
}
