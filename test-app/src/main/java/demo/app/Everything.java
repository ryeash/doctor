package demo.app;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Documented
@Retention(RUNTIME)
public @interface Everything {

    enum Letter {
        A, B, C
    }

    String string();

    String[] strings();

    String defaultString() default "default";

    byte byteVal();

    byte[] byteArr();

    short shortVal();

    short[] shortArr();

    int intVal();

    int[] intArr();

    long longVal();

    long[] longArr();

    float floatVal();

    float[] floatArr();

    double doubleVal();

    double[] doubleArr();

    boolean boolVal();

    boolean[] boolArr();

    CustomQualifier annotationVal();

    CustomQualifier[] annotationArr();

    Letter enumeration();

    Letter[] enumerations();

    Class<?> classVal();

    Class<?>[] classArr();
}
