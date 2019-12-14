package vest.doctor;

import javax.lang.model.element.TypeElement;

public interface ProviderDependency {

    TypeElement type();

    String qualifier();

    boolean required();
}
