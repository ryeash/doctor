package vest.doctor.aop;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * Internally used to coordinate aspects and method invocations.
 */
public class AspectCoordinator implements Around, Before, After {
    private final List<Before> befores = new LinkedList<>();
    private final List<Around> arounds = new LinkedList<>();
    private final List<After> afters = new LinkedList<>();

    public AspectCoordinator(Aspect... delegates) {
        this(Arrays.asList(delegates));
    }

    public AspectCoordinator(List<Aspect> delegates) {
        for (Aspect delegate : delegates) {
            if (delegate instanceof Before) {
                befores.add((Before) delegate);
            }
            if (delegate instanceof Around) {
                arounds.add((Around) delegate);
            }
            if (delegate instanceof After) {
                afters.add((After) delegate);
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
