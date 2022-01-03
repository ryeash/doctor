package vest.doctor.test;

import jakarta.inject.Qualifier;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Qualifier
@Documented
@Retention(RUNTIME)
public @interface AnimalColor {

    enum Color {
        RED, BLUE, BLACK
    }

    String name() default "";

    Color color() default Color.RED;
}
