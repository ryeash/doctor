package demo.app;

import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import vest.doctor.ConfigurationFacade;
import vest.doctor.Doctor;

import javax.inject.Provider;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class AppTest extends Assert {

    Doctor doctor = Doctor.load();

    static {
        System.setProperty("qualifierInterpolation", "interpolated");
        System.setProperty("properties", "test-override.properties,test.properties");
    }

    @AfterClass(alwaysRun = true)
    public void shutdown() throws Exception {
        System.out.println(doctor);
        doctor.close();
        assertTrue(TCCloseable.closed);
    }

    @Test
    public void basic() {
        CoffeeMaker cm = doctor.getInstance(CoffeeMaker.class);
        assertEquals(cm.brew(), "french pressing");
    }

    @Test
    public void injectedMethods() {
        TCInjectMethods instance = doctor.getInstance(TCInjectMethods.class);
        assertTrue(instance.injected);
    }

    @Test
    public void eager() {
        assertTrue(TCEager.created);
    }

    @Test
    public void event() {
        TCEvent event = doctor.getInstance(TCEvent.class);
        assertTrue(event.eventListened);
        event.message();
        assertEquals(event.messageReceived, "test");
    }

    @Test
    public void modules() {
        try (Doctor doc = Doctor.load("dev")) {
            CoffeeMaker cm = doc.getInstance(CoffeeMaker.class, "modules-test");
            assertEquals(cm.brew(), "dev");
        } catch (Exception e) {
            fail("no exception expected", e);
        }

        try (Doctor doc = Doctor.load("test")) {
            CoffeeMaker cm = doc.getInstance(CoffeeMaker.class, "modules-test");
            assertEquals(cm.brew(), "test");
        } catch (Exception e) {
            fail("no exception expected", e);
        }
    }

    @Test
    public void primary() {
        assertEquals(doctor.getInstance(TCPrimary.class), doctor.getInstance(TCPrimary.class, "primary"));
    }

    @Test
    public void configuration() {
        ConfigurationFacade conf = doctor.configuration();
        assertEquals(conf.get("string"), "value");
        assertEquals((int) conf.<Integer>get("number", Integer::valueOf), 42);
        assertTrue(conf.get("boolean", Boolean::valueOf));
        assertEquals(conf.get("override.this"), "overriden");
    }

    @Test
    public void properties() {
        doctor.getInstance(TCProperties.class);
    }

    @Test
    public void skipInjection() {
        assertFalse(doctor.getInstance(TCSkipInjection.class).injected);
    }

    @Test
    public void scope() throws ExecutionException, InterruptedException {
        Set<TCScope> singleton = IntStream.range(0, 100).parallel().mapToObj(i -> doctor.getInstance(TCScope.class, "singleton")).collect(Collectors.toSet());
        assertEquals(singleton.size(), 1);
        Set<TCScope> prototype = IntStream.range(0, 100).parallel().mapToObj(i -> doctor.getInstance(TCScope.class, "prototype")).collect(Collectors.toSet());
        assertEquals(prototype.size(), 100);

        Set<TCScope> threadLocal = Executors.newWorkStealingPool(3).submit(() -> {
            return IntStream.range(0, 100).parallel().mapToObj(i -> doctor.getInstance(TCScope.class, "threadLocal")).collect(Collectors.toSet());
        }).get();
        assertEquals(threadLocal.size(), 3);

        Set<TCScope> cached = IntStream.range(0, 1000)
                .parallel()
                .mapToObj(i -> {
                    try {
                        Thread.sleep(1);
                    } catch (Throwable t) {
                        // no-op
                    }
                    return doctor.getInstance(TCScope.class, "cached");
                })
                .collect(Collectors.toSet());
        assertTrue(cached.size() < 100 && cached.size() > 5);
    }

    @Test
    public void scheduled() throws InterruptedException {
        TCScheduled instance = doctor.getInstance(TCScheduled.class);
        TimeUnit.MILLISECONDS.sleep(105);
        assertTrue(instance.every10Milliseconds.get() >= 10);
        assertTrue(instance.every50Milliseconds.get() >= 2);
    }

    @Test
    public void providerInjection() {
        doctor.getInstance(TCProviderInject.class);
    }

    @Test
    public void qualifierInterpolation() {
        TCQualifierInterpolation instance = doctor.getInstance(TCQualifierInterpolation.class, "name-interpolated");
        assertNotNull(instance);
    }

    @Test
    public void customQualifiers() {
        doctor.getInstance(TCCustomQualifierHolder.class);
    }

    @Test
    public void optional() {
        doctor.getProvider(TCOptionalDependencies.class);
    }

    @Test
    public void providersWithAnnotation() {
        List<Object> collect = doctor.getProvidersWithAnnotation(Service.class).map(Provider::get).collect(Collectors.toList());
        assertEquals(collect.size(), 2);
        for (Object o : collect) {
            assertTrue(o instanceof TCService1 || o instanceof TCService2);
        }
    }

    @Test
    public void injectedMethodsTest() {
        TCInjectedMethodsC instance = doctor.getInstance(TCInjectedMethodsC.class);
        assertNotNull(instance.coffeeMaker);
        assertTrue(instance.injectedEmpty);
        assertNotEquals(instance.injectAsync, Thread.currentThread().getName());
        TCInjectedMethodsM instance1 = doctor.getInstance(TCInjectedMethodsM.class);
        assertNotNull(instance1.coffeeMaker);
        assertTrue(instance1.injectedEmpty);
    }
}
