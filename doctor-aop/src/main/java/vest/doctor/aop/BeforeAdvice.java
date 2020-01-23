package vest.doctor.aop;

public interface BeforeAdvice extends Aspect {
    void before(MethodInvocation invocation);
}
