package vest.doctor.http.jackson;

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import vest.doctor.flow.Emitter;
import vest.doctor.flow.Step;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Flow;

public class GenericJsonBeanMapping<T> implements Step<JsonParseToken, T> {
    private final ObjectMapper mapper;
    private final JavaType type;
    private final Deque<Object> stack;

    public GenericJsonBeanMapping(ObjectMapper mapper, Class<T> type) {
        this(mapper, mapper.getTypeFactory().constructType(type));
    }

    public GenericJsonBeanMapping(ObjectMapper mapper, JavaType type) {
        this.mapper = mapper;
        this.type = type;
        stack = new LinkedList<>();
    }

    @Override
    public void accept(JsonParseToken json, Flow.Subscription subscription, Emitter<T> emitter) {
        switch (json.token()) {
            case START_OBJECT:
                Map<String, Object> nextObject = new LinkedHashMap<>();
                if (inObject() || inArray()) {
                    setValue(json, nextObject);
                }
                stack.push(nextObject);
                break;
            case START_ARRAY:
                List<Object> nextArray = new ArrayList<>();
                if (inObject() || inArray()) {
                    setValue(json, nextArray);
                }
                stack.push(nextArray);
                break;
            case END_OBJECT:
            case END_ARRAY:
                Object obj = stack.pop();
                if (stack.isEmpty()) {
                    emitter.emit(complete(obj));
                }
                break;
            case FIELD_NAME:
                // no-op
                break;
            case VALUE_EMBEDDED_OBJECT:
                throw new UnsupportedOperationException("value embedded objects are not supported by this parser");
            case VALUE_STRING:
                setValue(json, json.textValue());
                break;
            case VALUE_NUMBER_INT:
                setValue(json, json.numberValue().longValue());
                break;
            case VALUE_NUMBER_FLOAT:
                setValue(json, json.numberValue());
                break;
            case VALUE_TRUE:
            case VALUE_FALSE:
                setValue(json, json.booleanValue());
                break;
            case VALUE_NULL:
                setValue(json, null);
                break;

            default:
                throw new IllegalStateException("Unexpected value: " + json.token());
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
        return mapper.convertValue(obj, type);
    }

    private void setValue(JsonParseToken token, Object o) {
        if (inObject()) {
            object().put(token.fieldName(), o);
        } else if (inArray()) {
            array().add(o);
        } else {
            throw new IllegalStateException("not the right place");
        }
    }
}
