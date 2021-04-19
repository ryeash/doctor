package vest.doctor.tuple;

import vest.doctor.stream.Stream5;
import vest.doctor.stream.StreamExt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class Tuple4Map<K1, K2, K3, K4, V> extends AbstractDelegatedMap<Tuple4<K1, K2, K3, K4>, V> {

    public Tuple4Map() {
        this(new HashMap<>());
    }

    public Tuple4Map(Map<Tuple4<K1, K2, K3, K4>, V> delegate) {
        super(delegate);
    }

    public boolean containsKey(K1 key1, K2 key2, K3 key3, K4 key4) {
        return containsKey(Tuple.of(key1, key2, key3, key4));
    }

    public V get(K1 key1, K2 key2, K3 key3, K4 key4) {
        return get(Tuple.of(key1, key2, key3, key4));
    }

    public V put(K1 key1, K2 key2, K3 key3, K4 key4, V value) {
        return put(Tuple.of(key1, key2, key3, key4), value);
    }

    public V removeTuple(K1 key1, K2 key2, K3 key3, K4 key4) {
        return remove(Tuple.of(key1, key2, key3, key4));
    }

    public Stream5<K1, K2, K3, K4, V> stream() {
        return StreamExt.of(entrySet()).map5(entry -> Tuple.of(entry.getKey().first(), entry.getKey().second(), entry.getKey().third(), entry.getKey().fourth(), entry.getValue()));
    }

    public V getOrDefault(K1 key1, K2 key2, K3 key3, K4 key4, V defaultValue) {
        return getOrDefault(Tuple.of(key1, key2, key3, key4), defaultValue);
    }

    public void forEach(Tuple5Consumer<K1, K2, K3, K4, V> action) {
        stream().forEach(action);
    }

    public void replaceAll(Tuple5Function<K1, K2, K3, K4, V, ? extends V> function) {
        replaceAll((tuple, value) -> function.apply(tuple.first(), tuple.second(), tuple.third(), tuple.fourth(), value));
    }

    public V putIfAbsent(K1 key1, K2 key2, K3 key3, K4 key4, V value) {
        return putIfAbsent(Tuple.of(key1, key2, key3, key4), value);
    }

    public boolean remove(K1 key1, K2 key2, K3 key3, K4 key4, Object value) {
        return remove(Tuple.of(key1, key2, key3, key4), value);
    }

    public boolean replace(K1 key1, K2 key2, K3 key3, K4 key4, V oldValue, V newValue) {
        return replace(Tuple.of(key1, key2, key3, key4), oldValue, newValue);
    }

    public V replaceTuple(K1 key1, K2 key2, K3 key3, K4 key4, V value) {
        return replace(Tuple.of(key1, key2, key3, key4), value);
    }

    public V computeIfAbsent(K1 key1, K2 key2, K3 key3, K4 key4, Tuple4Function<K1, K2, K3, K4, ? extends V> mappingFunction) {
        return computeIfAbsent(Tuple.of(key1, key2, key3, key4), tuple -> mappingFunction.apply(tuple.first(), tuple.second(), tuple.third(), tuple.fourth()));
    }

    public V computeIfPresent(K1 key1, K2 key2, K3 key3, K4 key4, Tuple5Function<K1, K2, K3, K4, ? super V, ? extends V> remappingFunction) {
        return computeIfPresent(Tuple.of(key1, key2, key3, key4), (tuple, value) -> remappingFunction.apply(tuple.first(), tuple.second(), tuple.third(), tuple.fourth(), value));
    }

    public V compute(K1 key1, K2 key2, K3 key3, K4 key4, Tuple5Function<K1, K2, K3, K4, ? super V, ? extends V> remappingFunction) {
        return compute(Tuple.of(key1, key2, key3, key4), (tuple, value) -> remappingFunction.apply(tuple.first(), tuple.second(), tuple.third(), tuple.fourth(), value));
    }

    public V merge(K1 key1, K2 key2, K3 key3, K4 key4, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return merge(Tuple.of(key1, key2, key3, key4), value, remappingFunction);
    }
}
