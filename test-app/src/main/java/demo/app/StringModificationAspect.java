package demo.app;

import vest.doctor.aop.AfterAdvice;
import vest.doctor.aop.MethodInvocation;

import javax.inject.Singleton;

@Singleton
public class StringModificationAspect implements AfterAdvice {
    @Override
    public void after(MethodInvocation invocation) {
        if (invocation.getResult() instanceof String) {
            invocation.setResult(invocation.getResult() + " altered");
        }
    }
}
