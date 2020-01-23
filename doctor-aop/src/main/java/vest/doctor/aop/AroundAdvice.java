package vest.doctor.aop;

public interface AroundAdvice extends Aspect {
    void execute(MethodInvocation invocation);
}
