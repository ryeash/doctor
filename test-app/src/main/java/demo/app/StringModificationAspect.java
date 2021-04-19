package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.aop.After;
import vest.doctor.aop.MethodInvocation;

@Singleton
public class StringModificationAspect implements After {
    @Override
    public void after(MethodInvocation invocation) {
        if (invocation.getResult() instanceof String) {
            String number = invocation.attributes().getOrDefault("number", "1");
            String letter = invocation.attributes().getOrDefault("letter", "L");
            invocation.setResult(invocation.getResult() + " altered" + number + letter);
        }
    }
}
