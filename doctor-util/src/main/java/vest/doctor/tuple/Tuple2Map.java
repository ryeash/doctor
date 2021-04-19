package vest.doctor.tuple;

import vest.doctor.stream.Stream3;
import vest.doctor.stream.StreamExt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class Tuple2Map<K1, K2, V> extends AbstractDelegatedMap<Tuple2<K1, K2>, V> {

    public Tuple2Map() {
        this(new HashMap<>());
    }

    public Tuple2Map(Map<Tuple2<K1, K2>, V> delegate) {
        super(delegate);
    }

    public boolean containsKey(K1 key1, K2 key2) {
        return containsKey(Tuple.of(key1, key2));
    }

    public V get(K1 key1, K2 key2) {
        return get(Tuple.of(key1, key2));
    }

    public V put(K1 key1, K2 key2, V value) {
        return put(Tuple.of(key1, key2), value);
    }

    public V removeTuple(K1 key1, K2 key2) {
        return remove(Tuple.of(key1, key2));
    }

    public Stream3<K1, K2, V> stream() {
        return StreamExt.of(entrySet()).map3(entry -> Tuple.of(entry.getKey().first(), entry.getKey().second(), entry.getValue()));
    }

    public V getOrDefault(K1 key1, K2 key2, V defaultValue) {
        return getOrDefault(Tuple.of(key1, key2), defaultValue);
    }

    public void forEach(Tuple3Consumer<K1, K2, V> action) {
        stream().forEach(action);
    }

    public void replaceAll(Tuple3Function<K1, K2, V, ? extends V> function) {
        replaceAll((tuple, value) -> function.apply(tuple.first(), tuple.second(), value));
    }

    public V putIfAbsent(K1 key1, K2 key2, V value) {
        return putIfAbsent(Tuple.of(key1, key2), value);
    }

    public boolean remove(K1 key1, K2 key2, Object value) {
        return remove(Tuple.of(key1, key2), value);
    }

    public boolean replace(K1 key1, K2 key2, V oldValue, V newValue) {
        return replace(Tuple.of(key1, key2), oldValue, newValue);
    }

    public V replaceTuple(K1 key1, K2 key2, V value) {
        return replace(Tuple.of(key1, key2), value);
    }

    public V computeIfAbsent(K1 key1, K2 key2, Tuple2Function<K1, K2, ? extends V> mappingFunction) {
        return computeIfAbsent(Tuple.of(key1, key2), tuple -> mappingFunction.apply(tuple.first(), tuple.second()));
    }

    public V computeIfPresent(K1 key1, K2 key2, Tuple3Function<K1, K2, ? super V, ? extends V> remappingFunction) {
        return computeIfPresent(Tuple.of(key1, key2), (tuple, value) -> remappingFunction.apply(tuple.first(), tuple.second(), value));
    }

    public V compute(K1 key1, K2 key2, Tuple3Function<K1, K2, ? super V, ? extends V> remappingFunction) {
        return compute(Tuple.of(key1, key2), (tuple, value) -> remappingFunction.apply(tuple.first(), tuple.second(), value));
    }

    public V merge(K1 key1, K2 key2, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return merge(Tuple.of(key1, key2), value, remappingFunction);
    }
}
