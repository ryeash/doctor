package vest.doctor.aop;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Internally used to coordinate aspects and method invocations.
 */
public class AspectCoordinator implements AroundAdvice, BeforeAdvice, AfterAdvice {
    private final List<BeforeAdvice> befores = new ArrayList<>(3);
    private final List<AroundAdvice> arounds = new ArrayList<>(3);
    private final List<AfterAdvice> afters = new ArrayList<>(3);

    public AspectCoordinator(Aspect... delegates) {
        this(Arrays.asList(delegates));
    }

    public AspectCoordinator(List<Aspect> delegates) {
        for (Aspect delegate : delegates) {
            if (delegate instanceof BeforeAdvice) {
                befores.add((BeforeAdvice) delegate);
            }
            if (delegate instanceof AroundAdvice) {
                arounds.add((AroundAdvice) delegate);
            }
            if (delegate instanceof AfterAdvice) {
                afters.add((AfterAdvice) delegate);
            }
        }
    }

    @Override
    public void before(MethodInvocation invocation) {
        UnInvokableMethodInvocation unInvocation = new UnInvokableMethodInvocation(invocation);
        if (!befores.isEmpty()) {
            for (BeforeAdvice before : befores) {
                before.before(unInvocation);
            }
        }
    }

    @Override
    public void execute(MethodInvocation invocation) {
        try {
            if (!arounds.isEmpty()) {
                for (AroundAdvice around : arounds) {
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
        UnInvokableMethodInvocation unInvocation = new UnInvokableMethodInvocation(invocation);
        if (!afters.isEmpty()) {
            for (AfterAdvice after : afters) {
                after.after(unInvocation);
            }
        }
    }
}
