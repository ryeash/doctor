package vest.doctor.reactor.http.jackson;

import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * A token parsed asynchronously from a data stream.
 *
 * @param objectMapper the object mapper responsible for parsing
 * @param token        the type parsed from the data
 * @param fieldName    the current field name of the parsed object
 * @param textValue    the text value for the token (if applicable)
 * @param numberValue  the number value for the token (if applicable)
 * @param booleanValue the boolean value for the token (if applicable)
 */
public record ParseToken(
        ObjectMapper objectMapper,
        JsonToken token,
        String fieldName,
        String textValue,
        Number numberValue,
        Boolean booleanValue) {
}
