package vest.doctor.restful.http.jackson;

import vest.doctor.Prioritized;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * An asynchronous parser of jackson tokens.
 * <p>
 * When a token is parsed by the {@link AsyncTokenizer} the token and a consumer to emit
 * completed items is handed to the implementation.
 */
public interface AsyncParser<T> extends BiConsumer<ParseToken, Consumer<T>>, Prioritized {
}
