/**
 * Provides the {@link vest.doctor.reactive.Rx} helper class that wraps the {@link java.util.concurrent.Flow}
 * interfaces in a fluent API allowing composition of reactive processing workflows.
 * <p>
 * Basics:
 * <code><pre>
 * // reactively process a list of strings
 * // static methods for starting the flow composition
 * Rx.each(list)
 *
 *  // intermediate stages to process items
 *  .flatMapStream(s -> s.chars().mapToObj(c -> "" + (char) c))
 *  .mapPublisher(item -> Rx.one(item).map(String::toUpperCase))
 *  .collect(Collectors.toList())
 *
 *   // subscribe to the processing flow to start moving items through it (returns a completable future)
 *  .subscribe()
 *
 *   // CompletableFuture#join to block the current thread until the complete signal is sent
 *  .join();
 * </pre></code>
 */
package vest.doctor.reactive;
