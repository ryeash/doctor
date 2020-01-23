package demo.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.aop.AfterAdvice;
import vest.doctor.aop.BeforeAdvice;
import vest.doctor.aop.MethodInvocation;

import javax.inject.Singleton;

@Singleton
public class LoggingAspect implements BeforeAdvice, AfterAdvice {
    private static Logger log = LoggerFactory.getLogger(LoggingAspect.class);

    @Override
    public void before(MethodInvocation methodInvocation) {
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
