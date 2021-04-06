package demo.app;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.aop.After;
import vest.doctor.aop.Before;
import vest.doctor.aop.MethodInvocation;

import java.lang.reflect.Method;
import java.util.Arrays;

@Singleton
public class LoggingAspect implements Before, After {
    private static final Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Override
    public void before(MethodInvocation methodInvocation) {
        try {
            Method method = methodInvocation.getMethod();
            log.info("{}", Arrays.toString(method.getDeclaredAnnotations()));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
        log.info("entering {}.{}",
                methodInvocation.getContainingInstance().getClass().getSimpleName(),
                methodInvocation.getMethodName());
    }

    @Override
    public void after(MethodInvocation methodInvocation) {
        log.info("leaving {}.{}",
                methodInvocation.getContainingInstance().getClass().getSimpleName(),
                methodInvocation.getMethodName());
    }
}
