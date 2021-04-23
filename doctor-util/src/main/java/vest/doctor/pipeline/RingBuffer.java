package vest.doctor.pipeline;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RingBuffer<T> {

    private final T[] array;
    private final AtomicInteger write = new AtomicInteger(0);
    private final Map<Integer, Integer> pullConsumers;

    @SuppressWarnings("unchecked")
    public RingBuffer(int size) {
        this.array = (T[]) new Object[size];
        this.pullConsumers = new ConcurrentHashMap<>(4, 1, 4);
    }

    public List<Integer> add(T value) {
        Objects.requireNonNull(value);
        int writeIndex = write.incrementAndGet();
        array[writeIndex % array.length] = value;
        return inc();
    }

    public T poll(Integer id) {
        if (size(id) == 0) {
            return null;
        }
        int readIndex = pullConsumers.compute(id, (v, idx) -> Objects.requireNonNullElseGet(idx, this::minReadIndex) + 1);
        return array[readIndex % array.length];
    }

    public int size(Integer id) {
        int writeIndex = write.get();
        int readIndex = pullConsumers.getOrDefault(id, minReadIndex());
        return (writeIndex - readIndex);
    }

    private List<Integer> inc() {
        List<Integer> filled = new LinkedList<>();
        for (Integer id : pullConsumers.keySet()) {
            if (size(id) >= array.length) {
                pullConsumers.remove(id);
                filled.add(id);
            }
        }
        return filled;
    }

    private int minReadIndex() {
        return Math.max(write.get() - array.length, 0);
    }
}
