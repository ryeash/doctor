package demo.app;

import demo.app.dao.DAO;
import demo.app.dao.User;
import demo.app.ignored.TCIgnoredClass;
import jakarta.inject.Provider;
import org.testng.annotations.Test;
import vest.doctor.ConfigurationFacade;
import vest.doctor.event.EventProducer;
import vest.doctor.event.ReloadConfiguration;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DoctorTest extends AbstractTestAppTest {

    @Test
    public void getInstance() {
        CoffeeMaker cm = providerRegistry().getInstance(CoffeeMaker.class);
        assertEquals(cm.brew(), "french pressing");
    }

    @Test
    public void injectedMethods() {
        TCInjectMethods instance = providerRegistry().getInstance(TCInjectMethods.class);
        assertTrue(instance.injected);
    }

    @Test
    public void eager() {
        assertTrue(TCEager.created);
    }

    @Test
    public void event() throws InterruptedException {
        TCEvent event = providerRegistry().getInstance(TCEvent.class);
        assertTrue(event.eventListened);
        Thread.sleep(5);
        assertEquals(event.messageReceived, "test");
    }

    @Test
    public void modules() {
        // No modules defined see Dev and Test ModuleTest
        assertTrue(providerRegistry().getProviderOpt(CoffeeMaker.class, "modules-test").isEmpty());
        assertTrue(providerRegistry().getProviderOpt(CoffeeMaker.class, "class-level-module").isEmpty());
    }

    @Test
    public void primary() {
        assertEquals(providerRegistry().getInstance(TCPrimary.class), providerRegistry().getInstance(TCPrimary.class, "primary"));
        providerRegistry().getInstance(OutputStream.class, "using-primary-test");
    }

    @Test
    public void configuration() {
        ConfigurationFacade conf = providerRegistry().configuration();
        assertEquals(conf.get("string"), "value");
        assertEquals((int) conf.<Integer>get("number", Integer::valueOf), 42);
        assertTrue(conf.get("boolean", Boolean::valueOf));
        assertEquals(conf.get("override.this"), "overridden");

        List<String> dbProps = providerRegistry().configuration().subsection("db.")
                .propertyNames().collect(Collectors.toList());
        assertEquals(dbProps, List.of("url", "username", "password"));
    }

    @Test
    public void properties() {
        providerRegistry().getInstance(TCProperties.class);
    }

    @Test
    public void skipInjection() {
        assertFalse(providerRegistry().getInstance(TCSkipInjection.class).injected);
    }

    @Test
    public void scope() throws ExecutionException, InterruptedException {
        Set<TCScope> singleton = IntStream.range(0, 100).parallel().mapToObj(i -> providerRegistry().getInstance(TCScope.class, "singleton")).collect(Collectors.toSet());
        assertEquals(singleton.size(), 1);
        Set<TCScope> prototype = IntStream.range(0, 100).parallel().mapToObj(i -> providerRegistry().getInstance(TCScope.class, "prototype")).collect(Collectors.toSet());
        assertEquals(prototype.size(), 100);

        Set<TCScope> threadLocal = Executors.newWorkStealingPool(3).submit(() ->
                IntStream.range(0, 100).parallel().mapToObj(i -> {
                    try {
                        Thread.sleep(3);
                    } catch (InterruptedException e) {
                        // ignored
                    }
                    return providerRegistry().getInstance(TCScope.class, "threadLocal");
                }).collect(Collectors.toSet())).get();
        assertEquals(threadLocal.size(), 3);

        Set<TCScope> cached = IntStream.range(0, 1000)
                .parallel()
                .mapToObj(i -> {
                    try {
                        Thread.sleep(1);
                    } catch (Throwable t) {
                        // no-op
                    }
                    return providerRegistry().getInstance(TCScope.class, "cached");
                })
                .collect(Collectors.toSet());
        assertTrue(cached.size() < 100 && cached.size() > 5);
    }

    @Test
    public void scheduled() throws InterruptedException {
        TCScheduled instance = providerRegistry().getInstance(TCScheduled.class);
        TimeUnit.MILLISECONDS.sleep(110);
        assertTrue(instance.every10Milliseconds.get() >= 10, "" + instance.every10Milliseconds);
        assertTrue(instance.every50Milliseconds.get() >= 2, "" + instance.every50Milliseconds);
        TimeUnit.MILLISECONDS.sleep(900);
        assertTrue(instance.cronEverySecond.get() >= 1, "" + instance.cronEverySecond);
    }

    @Test
    public void providerInjection() {
        TCProviderInject instance = providerRegistry().getInstance(TCProviderInject.class);
        assertTrue(instance.postConstructCalled);
    }

    @Test
    public void qualifierInterpolation() {
        TCQualifierInterpolation instance = providerRegistry().getInstance(TCQualifierInterpolation.class, "name-interpolated");
        assertNotNull(instance);
    }

    @Test
    public void customQualifiers() {
        providerRegistry().getInstance(TCCustomQualifierHolder.class);
    }

    @Test
    public void optional() {
        providerRegistry().getProvider(TCOptionalDependencies.class);
    }

    @Test
    public void providersWithAnnotation() {
        List<Object> collect = providerRegistry().getProvidersWithAnnotation(Service.class).map(Provider::get).collect(Collectors.toList());
        assertEquals(collect.size(), 2);
        for (Object o : collect) {
            assertTrue(o instanceof TCService1 || o instanceof TCService2);
        }
    }

    @Test
    public void injectedMethodsTest() {
        TCInjectedMethodsC instance = providerRegistry().getInstance(TCInjectedMethodsC.class);
        assertNotNull(instance.coffeeMaker);
        assertTrue(instance.injectedEmpty);
        assertNotEquals(instance.injectAsync, Thread.currentThread().getName());
        TCInjectedMethodsM instance1 = providerRegistry().getInstance(TCInjectedMethodsM.class);
        assertNotNull(instance1.coffeeMaker);
        assertTrue(instance1.injectedEmpty);
    }

    @Test
    public void aspects() throws IOException {
        TCAspects instance = providerRegistry().getInstance(TCAspects.class);
        instance.execute("name", List.of("a", "b"));
        assertEquals(instance.parrot("hi"), "hi altered42M");

        CoffeeMaker aspect = providerRegistry().getInstance(CoffeeMaker.class, "coffee-aspect");
        assertEquals(aspect.brew(), "french pressing altered1L");
    }

    @Test
    public void dao() {
        DAO dao = providerRegistry().getInstance(DAO.class);
        User user = new User();
        user.setId(1L);
        user.setFirstName("doug");
        user.setLastName("fernwaller");
        dao.store(user);

        User user1 = dao.findUser(1L);
        assertEquals(user1.getFirstName(), "doug");
        assertEquals(user1.getLastName(), "fernwaller");
    }

    @Test
    public void configurationReload() {
        EventProducer instance = providerRegistry().getInstance(EventProducer.class);
        instance.publish(new ReloadConfiguration());
        assertTrue(TCConfigReload.reloaded);
    }

    @Test
    public void staticFactory() {
        Object str = providerRegistry().getInstance(Object.class, "static");
        assertEquals(str, "static");
    }

    @Test
    public void ignoredClass() {
        assertFalse(providerRegistry().getProviderOpt(TCIgnoredClass.class).isPresent());
    }

    @Test
    public void parameterized() {
        TCParamterizedInject instance = providerRegistry().getInstance(TCParamterizedInject.class);
        assertEquals(instance.getInjectedList().getValue(), "worked");
    }
}
