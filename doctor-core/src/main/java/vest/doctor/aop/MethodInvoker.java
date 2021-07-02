package vest.doctor.aop;

@FunctionalInterface
public interface MethodInvoker<T, R> {
    R apply(T t) throws Exception;
}
