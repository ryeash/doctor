package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * A container for the {@link AnnotationData} marked on a source element.
 */
public interface AnnotationMetadata extends Iterable<AnnotationData> {

    /**
     * Metadata with no annotation values.
     */
    AnnotationMetadata EMPTY = new EmptyMetadata();

    /**
     * Get a stream of all annotation data.
     *
     * @return a stream of {@link AnnotationData}
     */
    Stream<AnnotationData> stream();

    /**
     * Find one annotation that matches the given type
     *
     * @param type the annotation type
     * @return an optional value expressing the found annotation
     */
    default Optional<AnnotationData> findOne(Class<? extends Annotation> type) {
        return findAll(type).findFirst();
    }

    /**
     * Find all annotations that match the given type.
     *
     * @param type the annotation type
     * @return a stream of matching annotations
     */
    default Stream<AnnotationData> findAll(Class<? extends Annotation> type) {
        return stream().filter(ad -> ad.type() == type);
    }

    /**
     * Find the first annotation of the given type and map the attribute name to a value.
     *
     * @param type          the annotation type
     * @param attributeName the attribute name
     * @param mapper        the mapping function
     * @return the value mapped value
     */
    default <T> T findOneMap(Class<? extends Annotation> type, String attributeName, BiFunction<AnnotationData, String, T> mapper) {
        return findOne(type).map(ad -> mapper.apply(ad, attributeName))
                .orElseThrow(() -> new IllegalArgumentException("missing annotation or unknown attribute: " + type + "#" + attributeName));
    }

    /**
     * Find the first annotation of the given type and get the string value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#stringValue(String)
     */
    default String stringValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::stringValue);
    }

    /**
     * Find the first annotation of the given type and get the string list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#stringArrayValue(String)
     */
    default List<String> stringArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::stringArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the boolean value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#booleanValue(String)
     */
    default boolean booleanValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::booleanValue);
    }

    /**
     * Find the first annotation of the given type and get the boolean list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#booleanArrayValue(String)
     */
    default List<Boolean> booleanArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::booleanArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the byte value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#byteValue(String)
     */
    default byte byteValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::byteValue);
    }

    /**
     * Find the first annotation of the given type and get the byte list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#byteArrayValue(String)
     */
    default List<Byte> byteArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::byteArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the short value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#shortValue(String)
     */
    default short shortValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::shortValue);
    }

    /**
     * Find the first annotation of the given type and get the short list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#shortArrayValue(String)
     */
    default List<Short> shortArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::shortArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the integer value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#intValue(String)
     */
    default int intValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::intValue);
    }

    /**
     * Find the first annotation of the given type and get the integer list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#intArrayValue(String)
     */
    default List<Integer> intArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::intArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the long value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#longValue(String)
     */
    default long longValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::longValue);
    }

    /**
     * Find the first annotation of the given type and get the long list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#longArrayValue(String)
     */
    default List<Long> longArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::longArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the float value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#floatValue(String)
     */
    default float floatValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::floatValue);
    }

    /**
     * Find the first annotation of the given type and get the float list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#floatArrayValue(String)
     */
    default List<Float> floatArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::floatArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the double value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#doubleValue(String)
     */
    default double doubleValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::doubleValue);
    }

    /**
     * Find the first annotation of the given type and get the double list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#doubleArrayValue(String)
     */
    default List<Double> doubleArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::doubleArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the enum value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#enumValue(String)
     */
    default Enum<?> enumValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::enumValue);
    }

    /**
     * Find the first annotation of the given type and get the enum list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#enumArrayValue(String)
     */
    default List<Enum<?>> enumArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::enumArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the class value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#classValue(String)
     */
    default Class<?> classValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::classValue);
    }

    /**
     * Find the first annotation of the given type and get the class list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#classArrayValue(String)
     */
    default List<Class<?>> classArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::classArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the annotation value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#annotationValue(String)
     */
    default AnnotationData annotationValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::annotationValue);
    }

    /**
     * Find the first annotation of the given type and get the annotation list value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#annotationArrayValue(String)
     */
    default List<AnnotationData> annotationArrayValue(Class<? extends Annotation> type, String attributeName) {
        return findOneMap(type, attributeName, AnnotationData::annotationArrayValue);
    }

    /**
     * Find the first annotation of the given type and get the object value for the given method name.
     *
     * @param type          the annotation type
     * @param attributeName the annotation method name
     * @return the annotation value
     * @see AnnotationData#objectValue(String)
     */
    Object objectValue(Class<? extends Annotation> type, String attributeName);

    record EmptyMetadata() implements AnnotationMetadata {

        @Override
        public Iterator<AnnotationData> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Stream<AnnotationData> stream() {
            return Stream.empty();
        }

        @Override
        public Object objectValue(Class<? extends Annotation> type, String attributeName) {
            return null;
        }
    }
}
