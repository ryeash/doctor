package demo.app.unit;

import org.testng.annotations.Test;
import vest.doctor.Cron;

import java.time.Instant;
import java.time.ZoneId;
import java.util.Date;

import static org.testng.Assert.assertEquals;

public class CronTest {

    @Test
    public void cronTest() {
        Cron cron = new Cron("15 * * * * *");
        Instant next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getSecond(), 15);

        cron = new Cron("* 21 * * * *");
        next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getMinute(), 21);

        cron = new Cron("* * 23 * * *");
        next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getHour(), 23);

        cron = new Cron("* * * 29 * *");
        next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getDayOfMonth(), 29);

        cron = new Cron("* * * * 2 *");
        next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getMonth().getValue(), 2);

        cron = new Cron("* * * * * 7");
        next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getDayOfWeek().getValue(), 7);
//        System.out.println(new Date());
//        System.out.println(new Date(cron.nextFireTime()));
    }

    @Test
    public void ranges() {
        Cron cron = new Cron("57-59 3,5 * * * *");
        nextN(cron, 5);

        nextN(new Cron("0 0,30 8-10 * * *"), 9);
        nextN(new Cron("0 0 0 2-5 JAN,JUN MON-FRI"), 9);

    }

    private void nextN(Cron cron, int n) {
        System.out.println("-----------------------");
        long l = cron.nextFireTime();
        System.out.println(new Date(l));
        for (int i = 0; i < n - 1; i++) {
            l = cron.nextFireTime(l);
            System.out.println(new Date(l));
        }
    }
}
