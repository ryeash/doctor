package vest.doctor.http.jackson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.flow.AbstractProcessor;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;

public class AsyncTokenizer extends AbstractProcessor<ByteBuffer, JsonParseToken> {
    private static final Logger log = LoggerFactory.getLogger(AsyncTokenizer.class);
    private final JsonParser async;
    private final ByteArrayFeeder feeder;

    private int depth = 0;

    public AsyncTokenizer(ObjectMapper mapper) {
        try {
            this.async = mapper.getFactory().createNonBlockingByteArrayParser();
            this.feeder = (ByteArrayFeeder) async.getNonBlockingInputFeeder();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void onNext(ByteBuffer buffer) {
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
                            publishDownstream(new JsonParseToken(t, null, null, null, null));
                            break;
                        case END_OBJECT:
                        case END_ARRAY:
                            depth--;
                            publishDownstream(new JsonParseToken(t, null, null, null, null));
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
                            publishDownstream(new JsonParseToken(t, async.getCurrentName(), async.getText(), null, null));
                            break;
                        case VALUE_NUMBER_FLOAT:
                        case VALUE_NUMBER_INT:
                            publishDownstream(new JsonParseToken(t, async.getCurrentName(), null, async.getNumberValueExact(), null));
                            break;
                        case VALUE_TRUE:
                        case VALUE_FALSE:
                            publishDownstream(new JsonParseToken(t, async.getCurrentName(), null, null, async.getBooleanValue()));
                            break;
                        case VALUE_NULL:
                            publishDownstream(new JsonParseToken(t, async.getCurrentName(), null, null, null));
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
    }
}
