package vest.doctor.test;

import org.testng.IMethodInstance;
import org.testng.IMethodInterceptor;
import org.testng.ITestContext;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

public class TestOrderRandomizer implements IMethodInterceptor {
    @Override
    public List<IMethodInstance> intercept(List<IMethodInstance> list, ITestContext iTestContext) {
        boolean randomize = Boolean.parseBoolean(System.getProperty("doctor.test.randomize", "true"));
        if (randomize) {
            Collections.shuffle(list, ThreadLocalRandom.current());
        }
        return list;
    }
}
