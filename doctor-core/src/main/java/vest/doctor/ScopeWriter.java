package vest.doctor;

import java.lang.annotation.Annotation;

public interface ScopeWriter extends ProviderWrappingWriter {

    Class<? extends Annotation> scope();
}
