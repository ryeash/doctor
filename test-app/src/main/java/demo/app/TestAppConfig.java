package demo.app;

import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.testng.Assert;
import vest.doctor.Cached;
import vest.doctor.Factory;
import vest.doctor.Modules;
import vest.doctor.Primary;
import vest.doctor.Prototype;
import vest.doctor.SkipInjection;
import vest.doctor.ThreadLocal;
import vest.doctor.aop.Aspects;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.OutputStream;

@Singleton
@Named("duck")
public class TestAppConfig {

    @Factory
    public InputStream io() {
        return new ByteArrayInputStream(new byte[0]);
    }

    @Modules("test")
    @Named("modules-test")
    @Factory
    @Singleton
    public CoffeeMaker testCoffeeMaker() {
        return () -> "test";
    }

    @Modules("dev")
    @Named("modules-test")
    @Factory
    @Singleton
    public CoffeeMaker devCoffeeMaker() {
        return () -> "dev";
    }

    @Factory
    @Singleton
    @Primary
    @Named("primary")
    public TCPrimary primaryOne() {
        return new TCPrimary();
    }

    @Factory
    @Singleton
    @Named("using-primary-test")
    public OutputStream usingPrimary(TCPrimary primary, @Named("primary") TCPrimary qualifiedPrimary) {
        Assert.assertSame(primary, qualifiedPrimary);
        return new OutputStream() {
            @Override
            public void write(int b) {
            }
        };
    }

    @Factory
    @SkipInjection
    public TCSkipInjection skipInjection() {
        return new TCSkipInjection();
    }

    @Factory
    @Singleton
    @Named("singleton")
    public TCScope singletonScope() {
        return new TCScope();
    }

    @Factory
    @ThreadLocal
    @Named("threadLocal")
    public TCScope threadLocalScope() {
        return new TCScope();
    }

    @Factory
    @Prototype
    @Named("prototype")
    public TCScope prototypeScope() {
        return new TCScope();
    }

    @Factory
    @Prototype
    @Named("weirdNa\"me\na\rb\tc\bd\\e\ff")
    public TCScope weirdName() {
        return new TCScope();
    }

    @Factory
    @Cached("10ms")
    @Named("cached")
    public TCScope cachedScope() {
        return new TCScope();
    }

    @Factory
    @CustomQualifier(name = "one", color = CustomQualifier.Color.BLACK)
    public TCCustomQualifier customQualifier1() {
        return new TCCustomQualifier("blackOne");
    }

    @Factory
    @CustomQualifier(color = CustomQualifier.Color.BLUE, name = "two")
    public TCCustomQualifier customQualifier2() {
        return new TCCustomQualifier("blueTwo");
    }

    @Factory
    @CustomQualifier(name = "defaultColor")
    public TCCustomQualifier customQualifier3() {
        return new TCCustomQualifier("defaultColor");
    }

    @Factory
    @Singleton
    @Named("coffee-aspect")
    @Aspects({TimingAspect.class, LoggingAspect.class, StringModificationAspect.class})
    public CoffeeMaker coffeeMakerAspect() {
        return new FrenchPress();
    }

    @Factory
    @Singleton
    @Named("static")
    public static Object staticFactory() {
        return "static";
    }

    @Factory
    @Singleton
    @Named("string")
    public ParameterizedThing<String> parameterizedType() {
        return new ParameterizedThing<>("worked");
    }

    @Factory
    @Named("complex-return-type")
    @SuppressWarnings("unchecked")
    public <T extends CoffeeMaker & AutoCloseable> T complexReturnType() {
        return (T) new ClosableCoffeeMaker();
    }

    public static final class ClosableCoffeeMaker implements CoffeeMaker, AutoCloseable {

        @Override
        public String brew() {
            return "closable";
        }

        @Override
        public void close() {
        }
    }
}
