package vest.doctor.reactor.http.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

public class AsyncTokenizer implements Function<ByteBuffer, List<ParseToken>> {
    private static final Logger log = LoggerFactory.getLogger(AsyncTokenizer.class);
    private final ObjectMapper objectMapper;
    private final JsonParser async;
    private final ByteArrayFeeder feeder;

    private int depth = 0;

    public AsyncTokenizer(ObjectMapper mapper) {
        try {
            this.objectMapper = mapper;
            this.async = mapper.getFactory().createNonBlockingByteArrayParser();
            this.feeder = (ByteArrayFeeder) async.getNonBlockingInputFeeder();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public List<ParseToken> apply(ByteBuffer buffer) {
        List<ParseToken> tokens = new LinkedList<>();
        byte[] b = new byte[1024];
        while (buffer.hasRemaining()) {
            int toRead = Math.min(buffer.remaining(), b.length);
            buffer.get(b, 0, toRead);
            try {
                feeder.feedInput(b, 0, toRead);

                JsonToken t;
                while ((t = async.nextToken()) != JsonToken.NOT_AVAILABLE) {
                    switch (t) {
                        case START_OBJECT:
                        case START_ARRAY:
                            depth++;
                            tokens.add(new ParseToken(objectMapper, t, null, null, null, null));
                            break;
                        case END_OBJECT:
                        case END_ARRAY:
                            depth--;
                            tokens.add(new ParseToken(objectMapper, t, null, null, null, null));
                            if (depth <= 0) {
                                if (buffer.hasRemaining()) {
                                    log.warn("complete json data read, but data still left in buffer {}", buffer);
                                }
                            }
                            break;
                        case FIELD_NAME:
                            // no-op
                            break;
                        case VALUE_STRING:
                            tokens.add(new ParseToken(objectMapper, t, async.getCurrentName(), async.getText(), null, null));
                            break;
                        case VALUE_NUMBER_FLOAT:
                        case VALUE_NUMBER_INT:
                            tokens.add(new ParseToken(objectMapper, t, async.getCurrentName(), null, async.getNumberValueExact(), null));
                            break;
                        case VALUE_TRUE:
                        case VALUE_FALSE:
                            tokens.add(new ParseToken(objectMapper, t, async.getCurrentName(), null, null, async.getBooleanValue()));
                            break;
                        case VALUE_NULL:
                            tokens.add(new ParseToken(objectMapper, t, async.getCurrentName(), null, null, null));
                            break;

                        case VALUE_EMBEDDED_OBJECT:
                            throw new UnsupportedOperationException("value embedded objects are not supported by this parser");
                        default:
                            throw new IllegalStateException("Unexpected parse token: " + t);
                    }
                }
            } catch (Throwable t) {
                throw new RuntimeException("error parsing data", t);
            }
        }
        return tokens;
    }
}
