package vest.doctor.runtime;

import vest.doctor.AnnotationData;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record AnnotationDataImpl(Class<? extends Annotation> type,
                                 Map<String, Object> values) implements AnnotationData {

    @Override
    public String stringValue(String attributeName) {
        return getValueWithCheck(attributeName, String.class);
    }

    @Override
    public List<String> stringArrayValue(String attributeName) {
        return getListValueWithCheck(attributeName, String.class);
    }

    @Override
    public boolean booleanValue(String attributeName) {
        return getValueWithCheck(attributeName, Boolean.class);
    }

    @Override
    public List<Boolean> booleanArrayValue(String attributeName) {
        return getListValueWithCheck(attributeName, Boolean.class);
    }

    @Override
    public byte byteValue(String attributeName) {
        return getValueWithCheck(attributeName, Byte.class);
    }

    @Override
    public List<Byte> byteArrayValue(String attributeName) {
        return getListValueWithCheck(attributeName, Byte.class);
    }

    @Override
    public short shortValue(String attributeName) {
        return getValueWithCheck(attributeName, Short.class);
    }

    @Override
    public List<Short> shortArrayValue(String attributeName) {
        return getListValueWithCheck(attributeName, Short.class);
    }

    @Override
    public int intValue(String attributeName) {
        return getValueWithCheck(attributeName, Integer.class);
    }

    @Override
    public List<Integer> intArrayValue(String attributeName) {
        return getListValueWithCheck(attributeName, Integer.class);
    }

    @Override
    public long longValue(String attributeName) {
        return getValueWithCheck(attributeName, Long.class);
    }

    @Override
    public List<Long> longArrayValue(String attributeName) {
        return getListValueWithCheck(attributeName, Long.class);
    }

    @Override
    public float floatValue(String attributeName) {
        return getValueWithCheck(attributeName, Float.class);
    }

    @Override
    public List<Float> floatArrayValue(String attributeName) {
        return getListValueWithCheck(attributeName, Float.class);
    }

    @Override
    public double doubleValue(String attributeName) {
        return getValueWithCheck(attributeName, Double.class);
    }

    @Override
    public List<Double> doubleArrayValue(String attributeName) {
        return getListValueWithCheck(attributeName, Double.class);
    }

    @Override
    public Enum<?> enumValue(String attributeName) {
        return getValueWithCheck(attributeName, Enum.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Enum<?>> enumArrayValue(String attributeName) {
        return (List) getListValueWithCheck(attributeName, Enum.class);
    }

    @Override
    public Class<?> classValue(String attributeName) {
        return getValueWithCheck(attributeName, Class.class);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Class<?>> classArrayValue(String attributeName) {
        return (List) getListValueWithCheck(attributeName, Class.class);
    }

    @Override
    public AnnotationData annotationValue(String attributeName) {
        return getValueWithCheck(attributeName, AnnotationData.class);
    }

    @Override
    public List<AnnotationData> annotationArrayValue(String attributeName) {
        return getListValueWithCheck(attributeName, AnnotationData.class);
    }

    @Override
    public Object objectValue(String attributeName) {
        return values.get(attributeName);
    }

    @Override
    public Iterator<Map.Entry<String, Object>> iterator() {
        return values.entrySet().iterator();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("@").append(type.getCanonicalName());

        String valuesString = values.entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .sorted()
                .collect(Collectors.joining(", ", "(", ")"));
        sb.append(valuesString);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> T getValueWithCheck(String attributeName, Class<T> check) {
        if (!values.containsKey(attributeName)) {
            throw new IllegalArgumentException("unknown annotation attribute \"" + attributeName + "\" for annotation type " + type);
        }
        Object value = values.get(attributeName);
        if (check.isInstance(value)) {
            return (T) value;
        } else {
            throw new ClassCastException("annotation attribute " + attributeName + " is not of type " + check);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> getListValueWithCheck(String attributeName, Class<T> check) {
        if (!values.containsKey(attributeName)) {
            throw new IllegalArgumentException("unknown annotation attribute \"" + attributeName + "\" for annotation type " + type);
        }
        Object value = values.get(attributeName);
        if (value instanceof List list) {
            if (list.isEmpty()) {
                return list;
            } else if (check.isInstance(list.get(0))) {
                return list;
            }
        }
        throw new ClassCastException("annotation array attribute " + attributeName + " is not of type " + check);
    }
}
