package doctor.processor;

import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public final class UniqueMethod {
    private final ExecutableElement method;
    private final String methodName;
    private final List<String> parameterTypes;

    public UniqueMethod(ExecutableElement method) {
        this.method = method;
        this.methodName = method.getSimpleName().toString();
        this.parameterTypes = method.getParameters().stream().map(Element::asType).map(String::valueOf).collect(Collectors.toList());
    }

    public ExecutableElement unwrap() {
        return method;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        UniqueMethod that = (UniqueMethod) o;
        return Objects.equals(methodName, that.methodName)
                && Objects.equals(parameterTypes, that.parameterTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(methodName, parameterTypes);
    }
}