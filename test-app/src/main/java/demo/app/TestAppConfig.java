package demo.app;

import vest.doctor.Factory;
import vest.doctor.Modules;
import vest.doctor.Primary;
import vest.doctor.Prototype;
import vest.doctor.SkipInjection;
import vest.doctor.ThreadLocal;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

@Singleton
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
        return new CoffeeMaker() {
            @Override
            public String brew() {
                return "test";
            }
        };
    }

    @Modules("dev")
    @Named("modules-test")
    @Factory
    @Singleton
    public CoffeeMaker devCoffeeMaker() {
        return new CoffeeMaker() {
            @Override
            public String brew() {
                return "dev";
            }
        };
    }

    @Factory
    @Singleton
    @Primary
    @Named("primary")
    public TCPrimary primaryOne() {
        return new TCPrimary();
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
}
