package vest.doctor.aop;

@FunctionalInterface
public interface MethodInvoker<R> {
    R apply(MethodInvocation invocation) throws Exception;
}
