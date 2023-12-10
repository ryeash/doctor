package vest.doctor.conf;

import org.testng.annotations.Test;
import vest.doctor.rx.QueuePublisher;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;

import static org.testng.Assert.assertTrue;

public class FlowTest {

    @Test
    public void foo() throws InterruptedException {
        QueuePublisher<String> p = new QueuePublisher<>(Executors.newCachedThreadPool());

        CountDownLatch latch = new CountDownLatch(1);
        p.subscribe(new Flow.Subscriber<>() {
            @Override
            public void onSubscribe(Flow.Subscription subscription) {
                subscription.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(String item) {
                System.out.println(Thread.currentThread().getName() + " got and item: " + item);
            }

            @Override
            public void onError(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onComplete() {
                latch.countDown();
            }
        });

        p.onNext("toot");
        p.onNext("teet");
        p.onComplete();
        assertTrue(latch.await(1, TimeUnit.SECONDS));
    }
}
