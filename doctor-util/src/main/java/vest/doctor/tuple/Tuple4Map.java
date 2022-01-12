package vest.doctor.tuple;

import vest.doctor.stream.Stream5;
import vest.doctor.stream.StreamExt;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * Extension of the {@link Map} interface to add multi-key support (via Tuples). The standard
 * methods of {@link Map} have tuple based counterparts in this implementation, e.g.
 * {@link Map#get(Object)} has {@link #get(Object, Object, Object, Object)},
 * For methods whose signatures for the multi-key variant would interfere with other methods
 * the methods are appended with `Tuple`, e.g. {@link #removeTuple(Object, Object, Object, Object)}.
 */
public class Tuple4Map<K1, K2, K3, K4, V> extends AbstractDelegatedMap<Tuple4<K1, K2, K3, K4>, V> {

    /**
     * Create a new map with 4-arity keys backed by a {@link HashMap}.
     */
    public Tuple4Map() {
        this(new HashMap<>());
    }

    /**
     * Create a new map with 4-arity keys backed by the given map.
     *
     * @param delegate the delegate map
     */
    public Tuple4Map(Map<Tuple4<K1, K2, K3, K4>, V> delegate) {
        super(delegate);
    }

    /**
     * @see Map#containsKey(Object)
     */
    public boolean containsKey(K1 key1, K2 key2, K3 key3, K4 key4) {
        return containsKey(Tuple.of(key1, key2, key3, key4));
    }

    /**
     * @see Map#get(Object)
     */
    public V get(K1 key1, K2 key2, K3 key3, K4 key4) {
        return get(Tuple.of(key1, key2, key3, key4));
    }

    /**
     * @see Map#put(Object, Object)
     */
    public V put(K1 key1, K2 key2, K3 key3, K4 key4, V value) {
        return put(Tuple.of(key1, key2, key3, key4), value);
    }

    /**
     * Remove a mapping from this map, returning the existing value if present.
     *
     * @param key1 the first key
     * @param key2 the second key
     * @param key3 the third key
     * @param key4 the fourth key
     * @return the value removed, if present, else null
     * @see Map#remove(Object)
     */
    public V removeTuple(K1 key1, K2 key2, K3 key3, K4 key4) {
        return remove(Tuple.of(key1, key2, key3, key4));
    }

    /**
     * Stream the keys and values stored in this map.
     *
     * @return a stream of 5-arity tuples containing the compound keys and values in this map
     */
    public Stream5<K1, K2, K3, K4, V> stream() {
        return StreamExt.of(entrySet()).map5(entry -> Tuple.of(entry.getKey().first(), entry.getKey().second(), entry.getKey().third(), entry.getKey().fourth(), entry.getValue()));
    }

    /**
     * @see Map#getOrDefault(Object, Object)
     */
    public V getOrDefault(K1 key1, K2 key2, K3 key3, K4 key4, V defaultValue) {
        return getOrDefault(Tuple.of(key1, key2, key3, key4), defaultValue);
    }

    /**
     * @see Map#forEach(BiConsumer)
     */
    public void forEach(Tuple5.Tuple5Consumer<K1, K2, K3, K4, V> action) {
        stream().forEach(action);
    }

    /**
     * @see Map#replaceAll(BiFunction)
     */
    public void replaceAll(Tuple5.Tuple5Function<K1, K2, K3, K4, V, ? extends V> function) {
        replaceAll((tuple, value) -> function.apply(tuple.first(), tuple.second(), tuple.third(), tuple.fourth(), value));
    }

    /**
     * @see Map#putIfAbsent(Object, Object)
     */
    public V putIfAbsent(K1 key1, K2 key2, K3 key3, K4 key4, V value) {
        return putIfAbsent(Tuple.of(key1, key2, key3, key4), value);
    }

    /**
     * @see Map#remove(Object, Object)
     */
    public boolean remove(K1 key1, K2 key2, K3 key3, K4 key4, Object value) {
        return remove(Tuple.of(key1, key2, key3, key4), value);
    }

    /**
     * @see Map#replace(Object, Object, Object)
     */
    public boolean replace(K1 key1, K2 key2, K3 key3, K4 key4, V oldValue, V newValue) {
        return replace(Tuple.of(key1, key2, key3, key4), oldValue, newValue);
    }

    /**
     * Replace the entry for the specified compound key if it exists.
     *
     * @param key1  the first key
     * @param key2  the second key
     * @param key3  the third key
     * @param key4  the fourth key
     * @param value the value to set
     * @return the previous value associated with the compound key
     * @see Map#replace(Object, Object)
     */
    public V replaceTuple(K1 key1, K2 key2, K3 key3, K4 key4, V value) {
        return replace(Tuple.of(key1, key2, key3, key4), value);
    }

    /**
     * @see Map#computeIfAbsent(Object, Function)
     */
    public V computeIfAbsent(K1 key1, K2 key2, K3 key3, K4 key4, Tuple4.Tuple4Function<K1, K2, K3, K4, ? extends V> mappingFunction) {
        return computeIfAbsent(Tuple.of(key1, key2, key3, key4), tuple -> mappingFunction.apply(tuple.first(), tuple.second(), tuple.third(), tuple.fourth()));
    }

    /**
     * @see Map#computeIfPresent(Object, BiFunction)
     */
    public V computeIfPresent(K1 key1, K2 key2, K3 key3, K4 key4, Tuple5.Tuple5Function<K1, K2, K3, K4, ? super V, ? extends V> remappingFunction) {
        return computeIfPresent(Tuple.of(key1, key2, key3, key4), (tuple, value) -> remappingFunction.apply(tuple.first(), tuple.second(), tuple.third(), tuple.fourth(), value));
    }

    /**
     * @see Map#compute(Object, BiFunction)
     */
    public V compute(K1 key1, K2 key2, K3 key3, K4 key4, Tuple5.Tuple5Function<K1, K2, K3, K4, ? super V, ? extends V> remappingFunction) {
        return compute(Tuple.of(key1, key2, key3, key4), (tuple, value) -> remappingFunction.apply(tuple.first(), tuple.second(), tuple.third(), tuple.fourth(), value));
    }

    /**
     * @see Map#merge(Object, Object, BiFunction)
     */
    public V merge(K1 key1, K2 key2, K3 key3, K4 key4, V value, BiFunction<? super V, ? super V, ? extends V> remappingFunction) {
        return merge(Tuple.of(key1, key2, key3, key4), value, remappingFunction);
    }
}
