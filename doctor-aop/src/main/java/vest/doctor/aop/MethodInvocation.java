package vest.doctor.aop;

public interface MethodInvocation {

    MethodMetadata getMetadata();

    int argumentSize();

    MutableMethodArgument getArgument(int i);

    <T> T getArgumentValue(int i);

    void setArgumentValue(int i, Object o);

    @SuppressWarnings("unchecked")
    <T> T invoke() throws Exception;

    boolean invoked();

    @SuppressWarnings("unchecked")
    <T> T getResult();

    void setResult(Object result);
}
