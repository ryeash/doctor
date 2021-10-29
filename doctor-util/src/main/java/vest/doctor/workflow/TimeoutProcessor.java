package vest.doctor.workflow;

import java.time.Duration;
import java.util.concurrent.Flow;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

final class TimeoutProcessor<T> extends AbstractProcessor<T, T> {

    private final ScheduledExecutorService executorService;
    private final long timeoutMillis;
    private volatile long lastUpdate = System.currentTimeMillis();
    private ScheduledFuture<?> scheduledFuture;

    public TimeoutProcessor(ScheduledExecutorService executorService, Duration timeout) {
        this.executorService = executorService;
        this.timeoutMillis = timeout.toMillis();
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
        this.scheduledFuture = executorService.scheduleAtFixedRate(this::checkTimeout, timeoutMillis, timeoutMillis, TimeUnit.MILLISECONDS);
    }

    @Override
    public void onNext(T item) {
        lastUpdate = System.currentTimeMillis();
        publishDownstream(item);
    }

    @Override
    public void onError(Throwable throwable) {
        super.onError(throwable);
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    @Override
    public void onComplete() {
        super.onComplete();
        if (scheduledFuture != null) {
            scheduledFuture.cancel(true);
        }
    }

    private void checkTimeout() {
        if (System.currentTimeMillis() > (lastUpdate + timeoutMillis)) {
            onError(new TimeoutException("timeout elapsed since last item publish"));
        }
    }
}
