/**
 * Provides the {@link vest.doctor.reactive.Rx} helper class that wraps the {@link java.util.concurrent.Flow}
 * interfaces in a fluent API allowing composition of reactive processing workflows.
 * <p>
 * Basics:
 * <code><pre>
 * // reactively process a list of strings
 * List<String> upper = Rx.each(list)
 *  .flatMapStream(s -> s.chars().mapToObj(c -> "" + (char) c))
 *  .mapPublisher(item -> Rx.one(item).map(String::toUpperCase))
 *  .collect(Collectors.toList())
 *  .subscribe()
 *  .join();
 * </pre></code>
 */
package vest.doctor.reactive;
