package vest.doctor.aop;

import vest.doctor.TypeInfo;

import java.util.List;
import java.util.Map;

/**
 * Static metadata about an invoked method that can be pre-calculated before invocation.
 */
public record MethodMetadata(
        Object containingInstance,
        String methodName,
        List<TypeInfo> methodParameters,
        TypeInfo returnType,
        Map<String, String> attributes
) {
}
