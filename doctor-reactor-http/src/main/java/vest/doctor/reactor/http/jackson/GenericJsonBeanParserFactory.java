package vest.doctor.reactor.http.jackson;

import com.fasterxml.jackson.databind.JavaType;
import reactor.core.publisher.SynchronousSink;
import vest.doctor.Prioritized;
import vest.doctor.reactor.http.RequestContext;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class GenericJsonBeanParserFactory implements AsyncParserFactory {
    @Override
    public AsyncParser<?> build(RequestContext requestContext, JavaType javaType) {
        return new GenericJsonBeanParser<>(javaType);
    }

    private static final class GenericJsonBeanParser<T> implements AsyncParser<T> {
        private final JavaType type;
        private final Deque<Object> stack;

        public GenericJsonBeanParser(JavaType type) {
            this.type = type;
            stack = new LinkedList<>();
        }

        @Override
        public void accept(ParseToken json, SynchronousSink<T> sink) {
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
                        sink.next(complete(json, obj));
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

        @Override
        public int priority() {
            return Prioritized.LOWEST_PRIORITY;
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

        private T complete(ParseToken json, Object obj) {
            return json.objectMapper().convertValue(obj, type);
        }

        private void setValue(ParseToken token, Object o) {
            if (inObject()) {
                object().put(token.fieldName(), o);
            } else if (inArray()) {
                array().add(o);
            } else {
                throw new IllegalStateException("not the right place");
            }
        }
    }
}
