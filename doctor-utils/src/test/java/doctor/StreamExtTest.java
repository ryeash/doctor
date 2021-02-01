package doctor;

import doctor.stream.StreamExt;
import doctor.tuple.NTuple;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StreamExtTest extends Assert {

    public static StreamExt<String> strings() {
        return StreamExt.of("alpha", "bravo", "charlie", "delta", "echo");
    }

    @Test
    public void takeWhile() {
        List<Integer> integers = StreamExt.of(1, 2, 3, 4, 5, 6)
                .takeWhile(i -> i < 4)
                .toList();
        assertEquals(integers, Arrays.asList(1, 2, 3));
    }

    @Test
    public void arity() {
        StreamExt.of("a", "b", "c", "d", "e")
                .map2(letter -> NTuple.of(letter, (int) letter.charAt(0)))
                .keep((letter, value) -> value < 99)
                .affixRight((letter, value) -> "]")
                .affixLeft((letter, value, suffix) -> "[")
                .filter(t -> t.third() != 0)
                .map4(Function.identity())
                .forEach(((prefix, letter, value, suffix) -> System.out.println(prefix + " " + letter + " " + value + " " + suffix)));
    }

    @Test
    public void safeMap() {
        List<Integer> integers = StreamExt.of("a", null, "c")
                .safeMap(String::length)
                .toList();
        assertEquals(integers, Arrays.asList(1, 1));
    }

    @Test
    public void flatMapCollection() {
        List<Integer> collect = StreamExt.of("")
                .flatMapCollection(str -> Arrays.asList(1, 2, 3, 5))
                .collect(Collectors.toList());
        assertEquals(collect, Arrays.asList(1, 2, 3, 5));
    }

    @Test
    public void keepAndDrop() {
        List<String> a = strings()
                .keep(s -> s.endsWith("a"))
                .toList();
        assertEquals(a, Arrays.asList("alpha", "delta"));

        a = strings()
                .drop(s -> s.endsWith("a"))
                .toList();
        assertEquals(a, Arrays.asList("bravo", "charlie", "echo"));
    }

    @Test
    public void safeFlatMap() {
        List<String> collect = strings()
                .map(s -> s.endsWith("a") ? s : null)
                .safeFlatMap(s -> Stream.of(s.split("l")))
                .toList();
        assertEquals(collect, Arrays.asList("a", "pha", "de", "ta"));
    }

    @Test
    public void distinctByKey() {
        List<String> collect = strings()
                .distinct(String::length)
                .toList();
        assertEquals(collect, Arrays.asList("alpha", "charlie", "echo"));
    }

    @Test
    public void autoClosing() {
        AtomicBoolean b = new AtomicBoolean(false);
        strings()
                .onClose(() -> b.set(true))
                .sink();
        assertTrue(b.get());
    }

    @Test
    public void append() {
        List<String> collect = strings()
                .append("foxtrot", "golf")
                .toList();
        assertEquals(collect, Arrays.asList("alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf"));
    }

    @Test
    public void toSet() {
        Set<String> strings = strings()
                .map(s -> s.substring(s.length() - 1))
                .toSet();
        assertTrue(strings.containsAll(Arrays.asList("a", "o", "e")));
        assertEquals(strings.size(), 3);
    }

    @Test
    public void join() {
        String join = strings()
                .map(s -> s.substring(0, 1))
                .join(",");
        assertEquals(join, "a,b,c,d,e");

        join = strings()
                .map(s -> s.substring(0, 1))
                .join(",", "(", ")");
        assertEquals(join, "(a,b,c,d,e)");
    }

    @Test
    public void affixRightLeft() {
        List<String> strings = strings()
                .affixRight(String::length)
                .map((str, length) -> str + length)
                .toList();
        assertEquals(strings, Arrays.asList("alpha5", "bravo5", "charlie7", "delta5", "echo4"));

        strings = strings()
                .affixLeft(String::length)
                .flip()
                .map((str, length) -> str + length)
                .toList();
        assertEquals(strings, Arrays.asList("alpha5", "bravo5", "charlie7", "delta5", "echo4"));
    }

    @Test
    public void flatAffixRight() {
        List<String> strings = strings()
                .flatAffixRight(s -> Stream.of(1, 2))
                .map((str, num) -> str + num)
                .toList();
        assertEquals(strings,
                Arrays.asList("alpha1", "alpha2", "bravo1", "bravo2", "charlie1", "charlie2", "delta1", "delta2", "echo1", "echo2"));
    }

    @Test
    public void withIndex() {
        List<String> strings = strings()
                .withIndex()
                .flip()
                .map((str, i) -> str + i)
                .toList();
        assertEquals(strings, Arrays.asList("alpha0", "bravo1", "charlie2", "delta3", "echo4"));
    }

    @Test
    public void feedForward() {
        strings()
                .feedForward(stream -> {
                    List<String> strings = stream.filter(s -> s.length() < 5)
                            .toList();
                    assertEquals(strings, Collections.singletonList("echo"));
                });

        List<String> strings = strings()
                .feedForwardAndReturn(stream -> {
                    return stream.filter(s -> s.length() < 5);
                })
                .toList();
        assertEquals(strings, Collections.singletonList("echo"));
    }

    @Test
    public void iterator() {
        AtomicBoolean closed = new AtomicBoolean(false);
        strings()
                .onClose(() -> closed.set(true))
                .sink();
        assertTrue(closed.get());
    }

    @Test
    public void tupleSorting() {
        List<String> sorted = StreamExt.of("b", "a", "c")
                .flatAffixRight(c -> Stream.of(2, 1))
                .sorted()
                .map(t -> t.first() + t.second())
                .collect(Collectors.toList());
        assertEquals(sorted, Arrays.asList("a1", "a2", "b1", "b2", "c1", "c2"));
    }
}