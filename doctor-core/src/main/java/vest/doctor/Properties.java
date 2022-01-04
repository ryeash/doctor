package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a properties object. An implementation of the interface will be created by the annotation
 * processor and exposed via a provider. The interface must use {@link Property} annotations on its methods and
 * have a {@link jakarta.inject.Scope} (e.g. {@link jakarta.inject.Singleton}).
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Properties {

    /**
     * Prefix to prepend before the values in {@link Property} annotations on the interface methods. For example:
     * setting this to `db.` will result in a method with <code>@Property("url")</code> to be bound to the property name
     * `db.url`.
     */
    String value() default "";
}
