package demo.app;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.aop.Aspect;
import vest.doctor.aop.AspectChain;
import vest.doctor.aop.MethodInvocation;

import java.lang.reflect.Method;
import java.util.Arrays;

@Singleton
public class LoggingAspect implements Aspect {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Override
    public <T> T execute(MethodInvocation methodInvocation, AspectChain chain) {
        try {
            Method method = methodInvocation.getMethod();
            log.info("{}", Arrays.toString(method.getDeclaredAnnotations()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        log.info("entering {}.{}",
                methodInvocation.getContainingInstance().getClass().getSimpleName(),
                methodInvocation.getMethodName());
        try {
            return chain.next(methodInvocation);
        } finally {
            log.info("leaving {}.{}",
                    methodInvocation.getContainingInstance().getClass().getSimpleName(),
                    methodInvocation.getMethodName());
        }
    }
}
