package vest.doctor.util;

import org.testng.Assert;
import org.testng.annotations.AfterMethod;

import java.lang.reflect.Method;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

public abstract class BaseUtilTest extends Assert {
    final Queue<AssertingConsumer<?>> asserting = new LinkedBlockingQueue<>();

    @AfterMethod(alwaysRun = true)
    public void checkAssertions(Method method) {
        AssertingConsumer<?> c;
        while ((c = asserting.poll()) != null) {
            c.assertCallCount(method.getName());
        }
    }

    protected <T> Consumer<T> expect(Consumer<T> expect) {
        return expect((i, v) -> expect.accept(v));
    }

    protected <T> Consumer<T> expect(BiConsumer<Integer, T> expect) {
        return expect(-1, expect);
    }

    protected <T> Consumer<T> expect(int callsExpected, BiConsumer<Integer, T> expect) {
        AssertingConsumer<T> c = new AssertingConsumer<T>(callsExpected, expect);
        asserting.add(c);
        return c;
    }

    private static final class AssertingConsumer<T> implements Consumer<T> {
        private final AtomicInteger i = new AtomicInteger(0);
        private final BiConsumer<Integer, T> assertion;
        private final int callsExpected;

        private AssertingConsumer(int callsExpected, BiConsumer<Integer, T> assertion) {
            this.assertion = assertion;
            this.callsExpected = callsExpected;
        }

        @Override
        public void accept(T t) {
            assertion.accept(i.getAndIncrement(), t);
        }

        public void assertCallCount(String method) {
            if (callsExpected >= 0) {
                Assert.assertEquals(i.get(), callsExpected, "this asserting consumer was not called the expected number of times: " + method);
            }
        }
    }
}
