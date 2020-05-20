package vest.doctor.netty;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.async.ByteArrayFeeder;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.ByteBuf;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Internally used to assist in asynchronous parsing of json objects with Jackson.
 */
public class AsyncMapper<T> {
    private final ObjectMapper mapper;
    private final JavaType type;
    private final JsonParser async;
    private final ByteArrayFeeder feeder;

    private final Deque<Object> stack;

    public AsyncMapper(ObjectMapper mapper, Class<T> type) {
        this(mapper, mapper.getTypeFactory().constructType(type));
    }

    public AsyncMapper(ObjectMapper mapper, TypeReference<T> type) {
        this(mapper, mapper.getTypeFactory().constructType(type));
    }

    public AsyncMapper(ObjectMapper mapper, JavaType type) {
        this.mapper = mapper;
        this.type = type;
        try {
            this.async = mapper.getFactory().createNonBlockingByteArrayParser();
            this.feeder = (ByteArrayFeeder) async.getNonBlockingInputFeeder();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        stack = new LinkedList<>();
    }

    public T feed(ByteBuf buf, boolean finished) {
        byte[] b = new byte[1024];
        T result = null;
        while (buf.readableBytes() > 0) {
            int toRead = Math.min(buf.readableBytes(), b.length);
            buf.readBytes(b, 0, toRead);
            result = feed(b, 0, toRead);
            if (result != null) {
                break;
            }
        }
        // TODO: warning about leftover buffer data?
        if (finished && result == null) {
            throw new IllegalStateException("data stream terminated before full document was sent");
        }
        return result;
    }

    public T feed(byte[] data, int offset, int length) {
        try {
            feeder.feedInput(data, offset, length);

            JsonToken t;
            while ((t = async.nextToken()) != JsonToken.NOT_AVAILABLE) {
                switch (t) {
                    case START_OBJECT:
                        Map<String, Object> nextObject = new LinkedHashMap<>();
                        if (inObject() || inArray()) {
                            setValue(nextObject);
                        }
                        stack.push(nextObject);
                        break;
                    case END_OBJECT:
                        Object obj = stack.pop();
                        if (stack.isEmpty()) {
                            return complete(obj);
                        }
                        break;
                    case START_ARRAY:
                        List<Object> nextArray = new ArrayList<>();
                        if (inObject() || inArray()) {
                            setValue(nextArray);
                        }
                        stack.push(nextArray);
                        break;
                    case END_ARRAY:
                        Object arr = stack.pop();
                        if (stack.isEmpty()) {
                            return complete(arr);
                        }
                        break;
                    case FIELD_NAME:
                        // no-op
                        break;
                    case VALUE_EMBEDDED_OBJECT:
                        throw new UnsupportedOperationException("value embedded objects are not supported by this parser");
                    case VALUE_STRING:
                        setValue(async.getText());
                        break;
                    case VALUE_NUMBER_INT:
                        setValue(async.getLongValue());
                        break;
                    case VALUE_NUMBER_FLOAT:
                        setValue(async.getFloatValue());
                        break;
                    case VALUE_TRUE:
                    case VALUE_FALSE:
                        setValue(async.getBooleanValue());
                        break;
                    case VALUE_NULL:
                        setValue(null);
                        break;

                    default:
                        throw new IllegalStateException("Unexpected value: " + t);
                }
            }
            return null;
        } catch (Throwable t) {
            throw new RuntimeException("error parsing data", t);
        }
    }

    private boolean inObject() {
        return stack.peek() instanceof Map;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> object() {
        return (Map<String, Object>) stack.peek();
    }

    private boolean inArray() {
        return stack.peek() instanceof Collection;
    }

    @SuppressWarnings("unchecked")
    private Collection<Object> array() {
        return (Collection<Object>) stack.peek();
    }

    private T complete(Object obj) {
        T t = mapper.convertValue(obj, type);
        feeder.endOfInput();
        return t;
    }

    private void setValue(Object o) throws IOException {
        if (inObject()) {
            object().put(async.getCurrentName(), o);
        } else if (inArray()) {
            array().add(o);
        } else {
            throw new IllegalStateException("not the right place");
        }
    }
}
