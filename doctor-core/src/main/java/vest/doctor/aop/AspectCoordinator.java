package vest.doctor.aop;

import vest.doctor.AnnotationMetadata;
import vest.doctor.TypeInfo;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

/**
 * Internally used to coordinate aspect execution.
 */
public final class AspectCoordinator {
    private final List<Aspect> aspects;

    public AspectCoordinator(Aspect... delegates) {
        aspects = List.of(delegates);
    }

    public <T> T call(MethodInvocation methodInvocation) {
        return new ChainedInvocation(methodInvocation, aspects.iterator()).next();
    }

    record ChainedInvocation(MethodInvocation delegate,
                             Iterator<Aspect> iterator) implements MethodInvocation {
        @Override
        public Object getContainingInstance() {
            return delegate.getContainingInstance();
        }

        @Override
        public String getMethodName() {
            return delegate.getMethodName();
        }

        @Override
        public List<TypeInfo> getMethodParameters() {
            return delegate.getMethodParameters();
        }

        @Override
        public TypeInfo getReturnType() {
            return delegate.getReturnType();
        }

        @Override
        public int arity() {
            return delegate.arity();
        }

        @Override
        public List<ArgValue> getArgumentValues() {
            return delegate.getArgumentValues();
        }

        @Override
        public ArgValue getArgumentValue(int i) {
            return delegate.getArgumentValue(i);
        }

        @Override
        public <T> T invoke() throws Exception {
            return delegate.invoke();
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T next() {
            if (iterator.hasNext()) {
                return (T) iterator.next().execute(this);
            } else {
                return delegate.next();
            }
        }

        @Override
        public Method getMethod() throws NoSuchMethodException {
            return delegate.getMethod();
        }

        @Override
        public AnnotationMetadata annotationMetadata() {
            return delegate.annotationMetadata();
        }
    }
}
