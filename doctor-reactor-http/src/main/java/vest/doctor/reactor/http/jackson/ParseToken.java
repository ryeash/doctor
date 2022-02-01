package vest.doctor.reactor.http.jackson;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A token parsed asynchronously from a data stream.
 */
public record ParseToken(
        ObjectMapper objectMapper,
        JsonToken token,
        String fieldName,
        String textValue,
        Number numberValue,
        Boolean booleanValue) {
}
