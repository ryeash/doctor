package demo.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.aop.Around;
import vest.doctor.aop.AspectException;
import vest.doctor.aop.MethodInvocation;

import javax.inject.Singleton;

@Singleton
public class TimingAspect implements Around {
    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);

    @Override
    public void execute(MethodInvocation methodInvocation) {
        long start = System.nanoTime();
        try {
            methodInvocation.invoke();
        } catch (Exception e) {
            throw new AspectException("error executing", e);
        }
        long duration = System.nanoTime() - start;
        log.info("invoking {}.{} took {}ns",
                methodInvocation.getContainingInstance().getClass().getSimpleName(),
                methodInvocation.getMethodName(),
                duration);
    }
}
