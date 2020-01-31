package vest.doctor.aop;

import java.lang.reflect.Method;

public interface MethodInvocation {

    MethodMetadata getMetadata();

    int argumentSize();

    MutableMethodArgument getArgument(int i);

    <T> T getArgumentValue(int i);

    void setArgumentValue(int i, Object o);

    <T> T invoke() throws Exception;

    boolean invoked();

    <T> T getResult();

    void setResult(Object result);

    Method getMethod() throws NoSuchMethodException;
}
