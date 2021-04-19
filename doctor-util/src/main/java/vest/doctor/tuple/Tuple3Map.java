package vest.doctor.tuple;

import vest.doctor.stream.Stream4;
import vest.doctor.stream.StreamExt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;

public class Tuple3Map<K1, K2, K3, V> extends AbstractDelegatedMap<Tuple3<K1, K2, K3>, V> {

    public Tuple3Map() {
        this(new HashMap<>());
    }

    public Tuple3Map(Map<Tuple3<K1, K2, K3>, V> delegate) {
        super(delegate);
    }

    public boolean containsKey(K1 key1, K2 key2, K3 key3) {
        return containsKey(Tuple.of(key1, key2, key3));
    }

    public V get(K1 key1, K2 key2, K3 key3) {
        return get(Tuple.of(key1, key2, key3));
    }

    public V put(K1 key1, K2 key2, K3 key3, V value) {
        return put(Tuple.of(key1, key2, key3), value);
    }

    public V removeTuple(K1 key1, K2 key2, K3 key3) {
        return remove(Tuple.of(key1, key2, key3));
    }

    public Stream4<K1, K2, K3, V> stream() {
        return StreamExt.of(entrySet()).map4(entry -> Tuple.of(entry.getKey().first(), entry.getKey().second(), entry.getKey().third(), entry.getValue()));
    }

    public V getOrDefault(K1 key1, K2 key2, K3 key3, V defaultValue) {
        return getOrDefault(Tuple.of(key1, key2, key3), defaultValue);
    }

    public void forEach(Tuple4Consumer<K1, K2, K3, V> action) {
        stream().forEach(action);
    }

    public void replaceAll(Tuple4Function<K1, K2, K3, V, ? extends V> function) {
        replaceAll((tuple, value) -> function.apply(tuple.first(), tuple.second(), tuple.third(), value));
    }

    public V putIfAbsent(K1 key1, K2 key2, K3 key3, V value) {
        return putIfAbsent(Tuple.of(key1, key2, key3), value);
    }

    public boolean remove(K1 key1, K2 key2, K3 key3, Object value) {
        return remove(Tuple.of(key1, key2, key3), value);
    }

    public boolean replace(K1 key1, K2 key2, K3 key3, V oldValue, V newValue) {
        return replace(Tuple.of(key1, key2, key3), oldValue, newValue);
    }

    public V replaceTuple(K1 key1, K2 key2, K3 key3, V value) {
        return replace(Tuple.of(key1, key2, key3), value);
    }

    public V computeIfAbsent(K1 key1, K2 key2, K3 key3, Tuple3Function<K1, K2, K3, ? extends V> mappingFunction) {
        return computeIfAbsent(Tuple.of(key1, key2, key3), mappingFunction);
    }

    public V computeIfPresent(K1 key1, K2 key2, K3 key3, Tuple4Function<K1, K2, K3, ? super V, ? extends V> remappingFunction) {
        return computeIfPresent(Tuple.of(key1, key2, key3), (tuple, value) -> remappingFunction.apply(tuple.first(), tuple.second(), tuple.third(), value));
    }

    public V compute(K1 key1, K2 key2, K3 key3, Tuple4Function<K1, K2, K3, ? super V, ? extends V> remappingFunction) {
        return compute(Tuple.of(key1, key2, key3), (tuple, value) -> remappingFunction.apply(tuple.first(), tuple.second(), tuple.third(), value));
    }

    public V merge(K1 key1, K2 key2, K3 key3, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return merge(Tuple.of(key1, key2, key3), value, remappingFunction);
    }
}
