package vest.doctor.util;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Track executed 'ticks' and run code every Nth tick.
 */
public final class Heartbeat {
    private final int runEveryN;
    private final AtomicInteger count = new AtomicInteger(0);

    /**
     * Create a new heart beat.
     *
     * @param runEveryN how often (every N ticks) to run the heartbeat task
     */
    public Heartbeat(int runEveryN) {
        this.runEveryN = runEveryN;
    }

    /**
     * Increment the 'tick', executing the given task if it has reached the Nth execution.
     *
     * @param heartbeatTask the task to execute every N ticks, the task is passed the total number of ticks
     *                      that have passed
     */
    public void tick(Consumer<Integer> heartbeatTask) {
        int c = count.incrementAndGet();
        if (c % runEveryN == 0) {
            heartbeatTask.accept(c);
        }
    }
}
