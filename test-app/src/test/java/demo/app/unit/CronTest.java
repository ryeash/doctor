package demo.app.unit;

import org.testng.annotations.Test;
import vest.doctor.scheduled.Cron;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.Month;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

public class CronTest {

    @Test
    public void cronTest() {
        Cron cron = new Cron("15 * * * * *", ZoneId.systemDefault().toString());
        Instant next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getSecond(), 15);

        cron = new Cron("* 21 * * * *", ZoneId.systemDefault().toString());
        next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getMinute(), 21);

        cron = new Cron("* * 23 * * *", ZoneId.systemDefault().toString());
        next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getHour(), 23);

        cron = new Cron("* * * 29 * *", ZoneId.systemDefault().toString());
        next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getDayOfMonth(), 29);

        cron = new Cron("* * * * 2 *", ZoneId.systemDefault().toString());
        next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getMonth().getValue(), 2);

        cron = new Cron("* * * * * 7", ZoneId.systemDefault().toString());
        next = Instant.ofEpochMilli(cron.nextFireTime());
        assertEquals(next.atZone(ZoneId.systemDefault()).getDayOfWeek().getValue(), 7);
    }

    @Test
    public void checkScheduleSequence() {
        assertLoop("57-59 3,5 * * * *", date -> {
            assertRange(date.getSecond(), 57, 59);
            assertTrue(date.getMinute() == 3 || date.getMinute() == 5);
        });

        assertLoop("0 0,30 8-10 * * *", date -> {
            assertEquals(date.getSecond(), 0);
            assertTrue(date.getMinute() == 0 || date.getMinute() == 30);
            assertRange(date.getHour(), 8, 10);
        });

        assertLoop("0 0 0 2-5 JAN,JUN MON-FRI", date -> {
            assertEquals(date.getSecond(), 0);
            assertEquals(date.getMinute(), 0);
            assertEquals(date.getHour(), 0);
            assertRange(date.getDayOfMonth(), 2, 5);
            assertTrue(date.getMonth() == Month.JANUARY || date.getMonth() == Month.JUNE);
            assertRange(date.getDayOfWeek().getValue(), DayOfWeek.MONDAY.getValue(), DayOfWeek.FRIDAY.getValue());
        });

        assertLoop("@weekly", date -> {
            assertEquals(date.getSecond(), 0);
            assertEquals(date.getMinute(), 0);
            assertEquals(date.getHour(), 0);
            assertEquals(date.getDayOfWeek(), DayOfWeek.SUNDAY);
        });

        assertLoop("0 0 0 1 JAN TUE", date -> {
            assertEquals(date.getSecond(), 0);
            assertEquals(date.getMinute(), 0);
            assertEquals(date.getHour(), 0);
            assertEquals(date.getDayOfMonth(), 1);
            assertEquals(date.getMonth(), Month.JANUARY);
            assertEquals(date.getDayOfWeek(), DayOfWeek.TUESDAY);
        });
    }

    private void assertLoop(String cronExpression, Consumer<ZonedDateTime> assertions) {
        Cron c = new Cron(cronExpression, ZoneId.systemDefault().toString());
        long l = c.nextFireTime();
        for (int i = 0; i < 50; i++) {
            assertions.accept(Instant.ofEpochMilli(l).atZone(ZoneId.systemDefault()));
            l = c.nextFireTime(l);
        }
    }

    private static void assertRange(int value, int start, int end) {
        assertTrue(value >= start && value <= end);
    }

    @Test
    public void errorConditions() {
        assertThrows(IllegalArgumentException.class, () -> {
            new Cron("100 * * * * *", ZoneId.systemDefault().toString());
        });
    }
}
