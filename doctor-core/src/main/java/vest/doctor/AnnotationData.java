package vest.doctor;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;

/**
 * A runtime wrapper around a static annotation attached to a provided type.
 * All values on the annotations are referenced via the string name of their method.
 */
public interface AnnotationData extends Iterable<Map.Entry<String, Object>> {

    /**
     * The annotation type.
     */
    Class<? extends Annotation> type();

    /**
     * Get a string value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * String str();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.stringValue("str");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    String stringValue(String attributeName);

    /**
     * Get a list of string values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * String[] strings();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.stringArrayValue("strings");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<String> stringArrayValue(String attributeName);

    /**
     * Get a boolean value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * boolean bool();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.booleanValue("bool");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    boolean booleanValue(String attributeName);

    /**
     * Get a list of boolean values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * boolean[] bools();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.booleanArrayValue("bools");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<Boolean> booleanArrayValue(String attributeName);

    /**
     * Get a byte value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * byte byteVal();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.byteValue("byteVal");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    byte byteValue(String attributeName);

    /**
     * Get a list of byte values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * byte[] bytes();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.byteArrayValue("bytes");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<Byte> byteArrayValue(String attributeName);

    /**
     * Get a short value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * short shortVal();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.shortValue("shortVal");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    short shortValue(String attributeName);

    /**
     * Get a list of short values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * short[] shorts();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.shortArrayValue("shorts");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<Short> shortArrayValue(String attributeName);

    /**
     * Get an int value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * int intVal();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.intValue("intVal");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    int intValue(String attributeName);

    /**
     * Get a list of integer values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * int[] ints();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.intArrayValue("ints");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<Integer> intArrayValue(String attributeName);

    /**
     * Get a long value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * long longVal();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.longValue("longVal");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    long longValue(String attributeName);

    /**
     * Get a list of long values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * long[] longs();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.longArrayValue("longs");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<Long> longArrayValue(String attributeName);

    /**
     * Get a float value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * float floatVal();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.floatValue("floatVal");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    float floatValue(String attributeName);

    /**
     * Get a list of float values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * float[] float();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.floatArrayValue("float");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<Float> floatArrayValue(String attributeName);

    /**
     * Get a double value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * double doubleVal();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.doubleValue("doubleVal");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    double doubleValue(String attributeName);

    /**
     * Get a list of double values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * double[] doubles();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.doubleArrayValue("doubles");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<Double> doubleArrayValue(String attributeName);

    /**
     * Get an enum value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * Color enumVal();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.enumValue("enumVal");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    Enum<?> enumValue(String attributeName);

    /**
     * Get a list of enum values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * SomeEnum[] enums();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.enumArrayValue("enums");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<Enum<?>> enumArrayValue(String attributeName);

    /**
     * Get a Class value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * Class<?> classVal();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.classValue("classVal");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    Class<?> classValue(String attributeName);

    /**
     * Get a list of class values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * Class<?>[] classes();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.classArrayValue("classes");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<Class<?>> classArrayValue(String attributeName);

    /**
     * Get an Annotation value.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * SomeAnnotation annotationVal();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.annotationValue("annotationVal");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    AnnotationData annotationValue(String attributeName);

    /**
     * Get a list of annotation values.
     * <br>
     * Example, for the given annotation method:<br>
     * <code>
     * SomeAnnotation[] annotations();
     * </code>
     * <br>
     * The value can be retrieved with:<br>
     * <code>annotationData.annotationArrayValue("annotations");</code>
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     * @throws IllegalArgumentException if the annotation does not have a method with the given name
     * @throws ClassCastException       if the annotation value is not the expected type
     */
    List<AnnotationData> annotationArrayValue(String attributeName);

    /**
     * Get an object value. Returns null if no value with the given name exists.
     *
     * @param attributeName the annotation method name
     * @return the annotation value
     */
    Object objectValue(String attributeName);
}
