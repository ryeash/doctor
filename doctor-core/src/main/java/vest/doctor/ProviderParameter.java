package vest.doctor;

import javax.lang.model.element.VariableElement;

public interface ProviderParameter {

    VariableElement parameter();

    String lookupCode(String nurseRef);

    String dependencyCheckCode(String nurseRef);
}
