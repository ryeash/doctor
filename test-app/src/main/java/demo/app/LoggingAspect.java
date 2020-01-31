package demo.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.aop.AfterAdvice;
import vest.doctor.aop.BeforeAdvice;
import vest.doctor.aop.MethodInvocation;

import javax.inject.Singleton;
import java.lang.reflect.Method;
import java.util.Arrays;

@Singleton
public class LoggingAspect implements BeforeAdvice, AfterAdvice {
    private static Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Override
    public void before(MethodInvocation methodInvocation) {
        try {
            Method method = methodInvocation.getMethod();
            log.info("{}", Arrays.toString(method.getDeclaredAnnotations()));
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        log.info("entering {}.{}",
                methodInvocation.getMetadata().getContainingInstance().getClass().getSimpleName(),
                methodInvocation.getMetadata().getMethodName());
    }

    @Override
    public void after(MethodInvocation methodInvocation) {
        log.info("leaving {}.{}",
                methodInvocation.getMetadata().getContainingInstance().getClass().getSimpleName(),
                methodInvocation.getMetadata().getMethodName());
    }
}
