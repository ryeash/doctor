package vest.doctor.http.jackson;

import com.fasterxml.jackson.core.JsonToken;

public record JsonParseToken(
        JsonToken token,
        String fieldName,
        String textValue,
        Number numberValue,
        Boolean booleanValue) {
}
