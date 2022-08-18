package vest.doctor;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to import external packages, that are not part of the compilation environment, into the compilation
 * environment. For example, classes from a 3rd party library annotated using JSR-311 annotations can
 * be imported and made known to the doctor processor so those classes can be wired into providers.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Import {
    /**
     * Packages to import into the compilation environment. All top level classes found in the package
     * will be processed.
     *
     * @return an array of packages to import into the compilation environment
     * @see javax.lang.model.util.Elements#getAllPackageElements(CharSequence)
     */
    String[] value();
}
