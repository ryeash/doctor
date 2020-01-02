package vest.doctor.netty;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@HttpMethod("GET")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface GET {
}
