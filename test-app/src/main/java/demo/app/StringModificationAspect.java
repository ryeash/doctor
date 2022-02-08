package demo.app;

import jakarta.inject.Singleton;
import org.testng.Assert;
import vest.doctor.AnnotationData;
import vest.doctor.TypeInfo;
import vest.doctor.aop.Aspect;
import vest.doctor.aop.MethodInvocation;

@Singleton
public class StringModificationAspect implements Aspect {

    @Override
    public <T> T execute(MethodInvocation methodInvocation) {
        T result = methodInvocation.next();

        if (methodInvocation.getContainingInstance() instanceof TCAspects
                && methodInvocation.getMethodName().equals("parrot")) {
            String value = methodInvocation.getMethodParameters()
                    .stream()
                    .map(TypeInfo::annotationMetadata)
                    .map(am -> am.findOneMap(ParameterAnnotation.class, "value", AnnotationData::stringValue))
                    .findFirst()
                    .orElse(null);
            Assert.assertEquals(value, "toast");
        }
        methodInvocation.getMethodParameters()
                .stream()
                .map(TypeInfo::annotationMetadata)
                .map(am -> am.findOneMap(ParameterAnnotation.class, "value", AnnotationData::stringValue))
                .findFirst()
                .ifPresent(str -> {
                    System.out.println("FOUND A PARAMETER ANNOTATION: " + str);
                });

        if (result instanceof String str) {
            return (T) (str + " altered");
        } else {
            return result;
        }
    }
}
