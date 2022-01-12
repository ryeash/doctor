package vest.doctor.tuple;

import vest.doctor.stream.Stream4;
import vest.doctor.stream.StreamExt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Extension of the {@link Map} interface to add multi-key support (via Tuples). The standard
 * methods of {@link Map} have tuple based counterparts in this implementation, e.g.
 * {@link Map#get(Object)} has {@link #get(Object, Object, Object)}.
 * For methods whose signatures for the multi-key variant would interfere with other methods
 * the methods are appended with `Tuple`, e.g. {@link #removeTuple(Object, Object, Object)}.
 */
public class Tuple3Map<K1, K2, K3, V> extends AbstractDelegatedMap<Tuple3<K1, K2, K3>, V> {

    /**
     * Create a new map with 3-arity keys backed by a {@link HashMap}.
     */
    public Tuple3Map() {
        this(new HashMap<>());
    }

    /**
     * Create a new map with 3-arity keys backed by the given map.
     *
     * @param delegate the delegate map
     */
    public Tuple3Map(Map<Tuple3<K1, K2, K3>, V> delegate) {
        super(delegate);
    }

    /**
     * @see Map#containsKey(Object)
     */
    public boolean containsKey(K1 key1, K2 key2, K3 key3) {
        return containsKey(Tuple.of(key1, key2, key3));
    }

    /**
     * @see Map#get(Object)
     */
    public V get(K1 key1, K2 key2, K3 key3) {
        return get(Tuple.of(key1, key2, key3));
    }

    /**
     * @see Map#put(Object, Object)
     */
    public V put(K1 key1, K2 key2, K3 key3, V value) {
        return put(Tuple.of(key1, key2, key3), value);
    }

    /**
     * Remove a mapping from this map, returning the existing value if present.
     *
     * @param key1 the first key
     * @param key2 the second key
     * @param key3 the third key
     * @return the value removed, if present, else null
     * @see Map#remove(Object)
     */
    public V removeTuple(K1 key1, K2 key2, K3 key3) {
        return remove(Tuple.of(key1, key2, key3));
    }

    /**
     * Stream the keys and values stored in this map.
     *
     * @return a stream of 4-arity tuples containing the compound keys and values in this map
     */
    public Stream4<K1, K2, K3, V> stream() {
        return StreamExt.of(entrySet()).map4(entry -> Tuple.of(entry.getKey().first(), entry.getKey().second(), entry.getKey().third(), entry.getValue()));
    }

    /**
     * @see Map#getOrDefault(Object, Object)
     */
    public V getOrDefault(K1 key1, K2 key2, K3 key3, V defaultValue) {
        return getOrDefault(Tuple.of(key1, key2, key3), defaultValue);
    }

    /**
     * @see Map#forEach(BiConsumer)
     */
    public void forEach(Tuple4.Tuple4Consumer<K1, K2, K3, V> action) {
        stream().forEach(action);
    }

    /**
     * @see Map#replaceAll(BiFunction)
     */
    public void replaceAll(Tuple4.Tuple4Function<K1, K2, K3, V, ? extends V> function) {
        replaceAll((tuple, value) -> function.apply(tuple.first(), tuple.second(), tuple.third(), value));
    }

    /**
     * @see Map#putIfAbsent(Object, Object)
     */
    public V putIfAbsent(K1 key1, K2 key2, K3 key3, V value) {
        return putIfAbsent(Tuple.of(key1, key2, key3), value);
    }

    /**
     * @see Map#remove(Object, Object)
     */
    public boolean remove(K1 key1, K2 key2, K3 key3, Object value) {
        return remove(Tuple.of(key1, key2, key3), value);
    }

    /**
     * @see Map#replace(Object, Object, Object)
     */
    public boolean replace(K1 key1, K2 key2, K3 key3, V oldValue, V newValue) {
        return replace(Tuple.of(key1, key2, key3), oldValue, newValue);
    }

    /**
     * Replace the entry for the specified compound key if it exists.
     *
     * @param key1  the first key
     * @param key2  the second key
     * @param key3  the third key
     * @param value the value to set
     * @return the previous value associated with the compound key
     * @see Map#replace(Object, Object)
     */
    public V replaceTuple(K1 key1, K2 key2, K3 key3, V value) {
        return replace(Tuple.of(key1, key2, key3), value);
    }

    /**
     * @see Map#computeIfAbsent(Object, Function)
     */
    public V computeIfAbsent(K1 key1, K2 key2, K3 key3, Tuple3.Tuple3Function<K1, K2, K3, ? extends V> mappingFunction) {
        return computeIfAbsent(Tuple.of(key1, key2, key3), mappingFunction);
    }

    /**
     * @see Map#computeIfPresent(Object, BiFunction)
     */
    public V computeIfPresent(K1 key1, K2 key2, K3 key3, Tuple4.Tuple4Function<K1, K2, K3, ? super V, ? extends V> remappingFunction) {
        return computeIfPresent(Tuple.of(key1, key2, key3), (tuple, value) -> remappingFunction.apply(tuple.first(), tuple.second(), tuple.third(), value));
    }

    /**
     * @see Map#compute(Object, BiFunction)
     */
    public V compute(K1 key1, K2 key2, K3 key3, Tuple4.Tuple4Function<K1, K2, K3, ? super V, ? extends V> remappingFunction) {
        return compute(Tuple.of(key1, key2, key3), (tuple, value) -> remappingFunction.apply(tuple.first(), tuple.second(), tuple.third(), value));
    }

    /**
     * @see Map#merge(Object, Object, BiFunction)
     */
    public V merge(K1 key1, K2 key2, K3 key3, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return merge(Tuple.of(key1, key2, key3), value, remappingFunction);
    }
}
