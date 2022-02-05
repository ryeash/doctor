package demo.app;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.aop.Aspect;
import vest.doctor.aop.AspectChain;
import vest.doctor.aop.AspectException;
import vest.doctor.aop.MethodInvocation;

@Singleton
public class TimingAspect implements Aspect {
    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);

    @Override
    public <T> T execute(MethodInvocation methodInvocation, AspectChain chain) {
        long start = System.nanoTime();
        try {
            return chain.next(methodInvocation);
        } catch (Exception e) {
            throw new AspectException("error executing", e);
        } finally {
            long duration = System.nanoTime() - start;
            log.info("invoking {}.{} took {}ns",
                    methodInvocation.getContainingInstance().getClass().getSimpleName(),
                    methodInvocation.getMethodName(),
                    duration);
        }
    }
}
