package vest.doctor.aop;

import java.util.LinkedList;
import java.util.List;

/**
 * Internally used to coordinate aspects and method invocations.
 */
public class AspectCoordinator implements Before, Around, After {
    private final List<Before> befores = new LinkedList<>();
    private final List<Around> arounds = new LinkedList<>();
    private final List<After> afters = new LinkedList<>();

    public AspectCoordinator(Aspect... delegates) {
        for (Aspect delegate : delegates) {
            if (delegate instanceof Before before) {
                befores.add(before);
            }
            if (delegate instanceof Around around) {
                arounds.add(around);
            }
            if (delegate instanceof After after) {
                afters.add(after);
            }
        }
    }

    public <T> T call(MethodInvocation invocation) {
        before(invocation);
        execute(invocation);
        after(invocation);
        return invocation.getResult();
    }

    @Override
    public void before(MethodInvocation invocation) {
        ((MethodInvocationImpl) invocation).setInvokable(false);
        if (!befores.isEmpty()) {
            for (Before before : befores) {
                before.before(invocation);
            }
        }
    }

    @Override
    public void execute(MethodInvocation invocation) {
        ((MethodInvocationImpl) invocation).setInvokable(true);
        try {
            if (!arounds.isEmpty()) {
                for (Around around : arounds) {
                    around.execute(invocation);
                }
            } else {
                invocation.invoke();
            }
        } catch (Throwable t) {
            throw new AspectException("error executing aspects", t);
        }
    }

    @Override
    public void after(MethodInvocation invocation) {
        ((MethodInvocationImpl) invocation).setInvokable(false);
        if (!afters.isEmpty()) {
            for (After after : afters) {
                after.after(invocation);
            }
        }
    }
}
