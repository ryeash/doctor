package vest.doctor.util;

import org.testng.annotations.Test;
import vest.doctor.pipeline.Pipeline;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Test(invocationCount = 5)
public class RxTest extends BaseUtilTest {

    static final List<String> strings = List.of("alpha", "bravo", "charlie", "delta", "foxtrot");

    public void basicObserve() {
        Pipeline.iterate(strings)
                .observe(expect(5, (it, string) -> assertEquals(string, strings.get(it))))
                .subscribeJoin();
    }

    public void basicMap() {
        Pipeline.iterate(strings)
                .map(String::length)
                .observe(expect(5, (it, length) -> assertEquals((int) length, strings.get(it).length())))
                .subscribeJoin();
    }

    public void basicFlatMap() {
        String test = "test";
        Pipeline.of(test)
                .flatMap(string -> string.chars().mapToObj(Character::toString).collect(Collectors.toList()))
                .observe(expect(4, (it, c) -> assertEquals(c, "" + test.charAt(it))))
                .subscribeJoin();
    }

    public void basicFlatStream() {
        String test = "test";
        Pipeline.of(test)
                .flatStream(string -> string.chars().mapToObj(Character::toString))
                .observe(expect(4, (it, c) -> assertEquals(c, "" + test.charAt(it))))
                .subscribeJoin();
    }

    public void basicFilter() {
        Pipeline.iterate(strings)
                .filter(s -> s.length() == 5)
                .observe(expect(3, (it, string) -> assertEquals(string.length(), 5)))
                .subscribeJoin();
    }

    public void basicCollect() {
        Pipeline.iterate(strings)
                .collect(Collectors.toList())
                .observe(expect(1, (it, list) -> assertEquals(list, strings)))
                .subscribeJoin();
    }

    public void collectFuture() {
        List<String> list = Pipeline.iterate(strings)
                .collect(Collectors.toList())
                .subscribeFuture()
                .join();
        assertEquals(list, strings);
    }

    public void adhocSource() {
        Pipeline<String, String> subscribe = Pipeline.adHoc(String.class)
                .observe(expect(3, (it, c) -> assertNotNull(c)))
                .subscribe();
        subscribe.publish("alpha")
                .publish("bravo")
                .publish("charlie")
                .onComplete();
        subscribe.join();
    }

    public void basicBackpressure() {
        AtomicInteger c = new AtomicInteger(0);
        Pipeline.iterate(strings)
                .observe((subscription, value) -> {
                    int andIncrement = c.getAndIncrement();
                    assertEquals(value, strings.get(andIncrement));
                    subscription.request(1);
                })
                .subscribe(1)
                .join();
        assertEquals(c.get(), 5);
    }

    public void basicBranch() {
        Pipeline.iterate(strings)
                .branch(p -> p
                        .observe(expect(5, (it, string) -> {
                            System.out.println("first observer: " + it + " " + string);
                        }))
                        .map(String::length)
                        .observe(expect(5, (it, length) -> {
                            System.out.println("second observer: " + it + " " + length);
                        }))
                        .subscribe())
                .observe(expect(5, (it, string) -> assertEquals(string, strings.get(it))))
                .subscribeJoin();
        Clock.sleepQuietly(100);
    }

    public void basicBuffer() {
        Pipeline.iterate(strings)
                .buffer()
                .observe(expect(5, (it, v) -> assertEquals(v, strings.get(it))))
                .map(String::length)
                .observe(expect(5, (it, v) -> assertEquals((int) v, strings.get(it).length())))
                .subscribeJoin(Executors.newSingleThreadExecutor());

        Pipeline.iterate(strings)
                .buffer(-1)
                .observe(expect(5, (it, v) -> assertEquals(v, strings.get(it))))
                .map(String::length)
                .observe(expect(5, (it, v) -> assertEquals((int) v, strings.get(it).length())))
                .subscribeJoin(Executors.newSingleThreadExecutor());
    }
}
