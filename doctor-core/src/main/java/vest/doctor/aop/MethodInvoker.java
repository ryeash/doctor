package vest.doctor.aop;

/**
 * Internal use only.
 */
@FunctionalInterface
public interface MethodInvoker<R> {
    R apply(MethodInvocation invocation) throws Exception;
}
