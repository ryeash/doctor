package vest.doctor.util;

import org.testng.annotations.Test;
import vest.doctor.pipeline.PipelineBuilder;

import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

public class RxTest extends BaseUtilTest {

    static final List<String> strings = List.of("alpha", "bravo", "charlie", "delta", "foxtrot");

    @Test
    public void basicObserve() {
        PipelineBuilder.iterate(strings)
                .observe(expect(5, (it, string) -> assertEquals(string, strings.get(it))))
                .subscribe();
    }

    @Test
    public void basicMap() {
        PipelineBuilder.iterate(strings)
                .map(String::length)
                .observe(expect(5, (it, length) -> assertEquals((int) length, strings.get(it).length())))
                .subscribe();
    }

    @Test
    public void basicFlatMap() {
        String test = "test";
        PipelineBuilder.single(test)
                .flatMap(string -> string.chars().mapToObj(Character::toString).collect(Collectors.toList()))
                .observe(expect(4, (it, c) -> assertEquals(c, "" + test.charAt(it))))
                .subscribe();
    }

    @Test
    public void basicFlatStream() {
        String test = "test";
        PipelineBuilder.single(test)
                .flatStream(string -> string.chars().mapToObj(Character::toString))
                .observe(expect(4, (it, c) -> assertEquals(c, "" + test.charAt(it))))
                .subscribe();
    }

    @Test
    public void pipeline() {
        PipelineBuilder.<String>adHoc()
                .observe(s -> System.out.println(Thread.currentThread().getName() + " raw string: " + s))
                .branch(p -> p.collect(Collectors.toList()).observe(l -> System.out.println(Thread.currentThread().getName() + " collected list: " + l)))
                .map(String::length)
                .observe(s -> System.out.println(Thread.currentThread().getName() + " string len: " + s))
                .subscribe(ForkJoinPool.commonPool())
                .publish("alpha")
                .publish("bravo")
                .publish("charlie")
                .onComplete();

        System.out.println("------------------------------------------------------------");

        PipelineBuilder.iterate(List.of("abc", "def", "ghi"))
                .buffer(52)
                .observe(s -> System.out.println(Thread.currentThread().getName() + " raw string: " + s))
                .map(String::length)
                .observe(s -> System.out.println(Thread.currentThread().getName() + " string len: " + s))
                .subscribe(ForkJoinPool.commonPool())
                .completionFuture()
                .join();

    }
}
