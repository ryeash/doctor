package vest.doctor.aop;

import vest.doctor.AnnotationMetadata;
import vest.doctor.TypeInfo;

import java.util.List;

/**
 * Metadata about an invoked method that can be statically compiled.
 */
public record MethodMetadata(
        Object containingInstance,
        String methodName,
        List<TypeInfo> methodParameters,
        TypeInfo returnType,
        AnnotationMetadata annotationData) {
}
