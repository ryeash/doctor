package vest.doctor.aop;

public interface AfterAdvice extends Aspect {
    void after(MethodInvocation invocation);
}
