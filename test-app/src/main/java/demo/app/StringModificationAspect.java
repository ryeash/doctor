package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.aop.Aspect;
import vest.doctor.aop.AspectChain;
import vest.doctor.aop.MethodInvocation;

@Singleton
public class StringModificationAspect implements Aspect {

    @Override
    public <T> T execute(MethodInvocation methodInvocation, AspectChain chain) {
        T result = chain.next(methodInvocation);
        if (result instanceof String str) {
            return (T) (str + " altered");
        } else {
            return result;
        }
    }
}
