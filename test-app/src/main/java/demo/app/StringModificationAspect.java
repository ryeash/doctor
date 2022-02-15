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
    public Object execute(MethodInvocation methodInvocation) {
        Object result = methodInvocation.next();

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

        if (result instanceof String str) {
            return (str + " altered");
        } else {
            return result;
        }
    }
}
