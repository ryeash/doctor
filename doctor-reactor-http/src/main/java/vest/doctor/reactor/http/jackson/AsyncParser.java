package vest.doctor.reactor.http.jackson;

import reactor.core.publisher.SynchronousSink;
import vest.doctor.Prioritized;

import java.util.function.BiConsumer;

/**
 * An asynchronous parser of jackson tokens.
 */
public interface AsyncParser<T> extends BiConsumer<ParseToken, SynchronousSink<T>>, Prioritized {
}
