package vest.doctor;

import javax.lang.model.element.TypeElement;

/**
 * Represents a type/qualifier dependency.
 */
public interface ProviderDependency {

    /**
     * The type.
     */
    TypeElement type();

    /**
     * The qualifier.
     */
    String qualifier();

    /**
     * True if the dependency is strictly required by the target.
     */
    boolean required();
}
