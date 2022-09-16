package demo.app;

import app.ext.Widget;
import demo.app.dao.DAO;
import demo.app.dao.DBProps;
import demo.app.dao.User;
import demo.app.ignored.TCIgnoredClass;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.hibernate.AssertionFailure;
import org.testng.annotations.Test;
import vest.doctor.AnnotationData;
import vest.doctor.DoctorProvider;
import vest.doctor.ThreadLocal;
import vest.doctor.conf.ConfigurationFacade;
import vest.doctor.event.EventBus;
import vest.doctor.event.ReloadConfiguration;
import vest.doctor.event.ReloadProviders;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
    public void event() throws InterruptedException, ExecutionException, TimeoutException {
        TCEvent event = providerRegistry().getInstance(TCEvent.class);
        event.eventListened.get(100, TimeUnit.MILLISECONDS);
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
    public void configurationTest() {
        ConfigurationFacade conf = configuration();
        assertEquals(conf.get("string"), "value");
        assertEquals((int) conf.<Integer>get("number", Integer::valueOf), 42);
        assertTrue(conf.get("boolean", Boolean::valueOf));
        assertEquals(conf.get("override.this"), "overridden");

        ConfigurationFacade db = configuration().prefix("db.");
        assertNotNull(db.get("url"));
        assertNotNull(db.get("username"));
        assertNotNull(db.get("password"));

        assertEquals(conf.get("dot.nested.object.a"), "b");
        assertEquals(conf.get("dot.nested.object.c"), "d");
    }

    @Test
    public void properties() {
        providerRegistry().getInstance(TCProperties.class);

        DBProps dbProps = providerRegistry().getInstance(DBProps.class);
        assertEquals(dbProps.url(), "jdbc:hsqldb:mem:mymemdb");
        assertEquals(dbProps.username(), "unused");
        assertEquals(dbProps.password(), "nothing");
        assertEquals(dbProps.timeout(), 12);
        assertNull(dbProps.willBeNull().orElse(null));
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
        assertEquals(instance.parrot("hi"), "hi altered");

        CoffeeMaker aspect = providerRegistry().getInstance(CoffeeMaker.class, "coffee-aspect");
        assertEquals(aspect.brew(), "french pressing altered");

        Map<String, Object> map = new HashMap<>();
        map.put("string", "str");
        Map<String, Object> mapping = instance.mapping(map);
        assertEquals(mapping.get("string"), "str");
        assertEquals(mapping.get("_pre"), true);
        assertEquals(mapping.get("_modified"), true);
        assertEquals(mapping.get("_arity"), 1);
        assertEquals(mapping.get("_paramName"), "input");
        assertEquals(mapping.get("_methodName"), "mapping");
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
        EventBus instance = providerRegistry().getInstance(EventBus.class);
        instance.publish(new ReloadConfiguration());
        assertTrue(TCConfigReload.reloaded);
    }

    @Test
    public void staticFactory() {
        String str = providerRegistry().getInstance(String.class, "static");
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

    @Test
    public void reloadable() {
        TCReloadable first = providerRegistry().getInstance(TCReloadable.class);
        TCReloadable second = providerRegistry().getInstance(TCReloadable.class);
        providerRegistry().getInstance(EventBus.class).publish(new ReloadProviders());
        TCReloadable third = providerRegistry().getInstance(TCReloadable.class);
        assertEquals(first, second);
        assertNotEquals(first, third);
    }

    @Test
    public void annotationMetadata() {
        DoctorProvider<TCQualifierInterpolation> p = providerRegistry().getProvider(TCQualifierInterpolation.class, "name-interpolated");
        String s = p.annotationMetadata().stream()
                .filter(m -> m.type() == Named.class)
                .map(m -> m.stringValue("value"))
                .findFirst()
                .orElseThrow(() -> new AssertionFailure("failure"));
        assertEquals(s, "name-${qualifierInterpolation}");

        DoctorProvider<TCAnnotationMetadata> provider = providerRegistry().getProvider(TCAnnotationMetadata.class);
        AnnotationData everything = provider.annotationMetadata().findOne(Everything.class).orElseThrow(() -> new AssertionFailure("missing everything"));
        assertEquals(everything.stringValue("string"), "a");
        assertEquals(everything.stringArrayValue("strings"), List.of("b", "c"));
        assertEquals(everything.byteValue("byteVal"), (byte) 1);
        assertEquals(everything.byteArrayValue("byteArr"), List.of((byte) 2, (byte) 3));
        assertEquals(everything.shortValue("shortVal"), (short) 4);
        assertEquals(everything.shortArrayValue("shortArr"), List.of((short) 5, (short) 6));
        assertEquals(everything.intValue("intVal"), 7);
        assertEquals(everything.intArrayValue("intArr"), List.of(8, 9));
        assertEquals(everything.longValue("longVal"), 10L);
        assertEquals(everything.longArrayValue("longArr"), List.of(11L, 12L));
        assertEquals(everything.floatValue("floatVal"), 13.1F);
        assertEquals(everything.floatArrayValue("floatArr"), List.of(14.2F, 15.3F));
        assertEquals(everything.doubleValue("doubleVal"), 16.4D);
        assertEquals(everything.doubleArrayValue("doubleArr"), List.of(17.5D, 18.6D));
        assertTrue(everything.booleanValue("boolVal"));
        assertEquals(everything.booleanArrayValue("boolArr"), List.of(true, false));
        assertEquals(everything.enumValue("enumeration"), demo.app.Everything.Letter.A);
        assertEquals(everything.enumArrayValue("enumerations"), List.of(demo.app.Everything.Letter.B, demo.app.Everything.Letter.C));
        assertEquals(everything.classValue("classVal"), String.class);
        assertEquals(everything.classArrayValue("classArr"), List.of(Integer.class, Long.class));

        AnnotationData annotationVal = everything.annotationValue("annotationVal");
        assertEquals(annotationVal.type(), CustomQualifier.class);
        assertEquals(annotationVal.stringValue("name"), "nested");
        assertEquals(annotationVal.enumValue("color"), CustomQualifier.Color.RED);

        List<AnnotationData> annotationArr = everything.annotationArrayValue("annotationArr");
        assertEquals(annotationArr.size(), 2);
        AnnotationData first = annotationArr.get(0);
        assertEquals(first.type(), CustomQualifier.class);
        assertEquals(first.stringValue("name"), "two");
        assertEquals(first.enumValue("color"), CustomQualifier.Color.BLACK);
        AnnotationData second = annotationArr.get(1);
        assertEquals(second.type(), CustomQualifier.class);
        assertEquals(second.stringValue("name"), "three");
        assertEquals(second.enumValue("color"), CustomQualifier.Color.RED);


        assertEquals(provider.annotationMetadata().stringValue(Everything.class, "string"), "a");
        assertEquals(provider.annotationMetadata().stringArrayValue(Everything.class, "strings"), List.of("b", "c"));
        assertEquals(provider.annotationMetadata().stringValue(Everything.class, "defaultString"), "default");
        assertEquals(provider.annotationMetadata().byteValue(Everything.class, "byteVal"), (byte) 1);
        assertEquals(provider.annotationMetadata().byteArrayValue(Everything.class, "byteArr"), List.of((byte) 2, (byte) 3));
        assertEquals(provider.annotationMetadata().shortValue(Everything.class, "shortVal"), (short) 4);
        assertEquals(provider.annotationMetadata().shortArrayValue(Everything.class, "shortArr"), List.of((short) 5, (short) 6));
        assertEquals(provider.annotationMetadata().intValue(Everything.class, "intVal"), 7);
        assertEquals(provider.annotationMetadata().intArrayValue(Everything.class, "intArr"), List.of(8, 9));
        assertEquals(provider.annotationMetadata().longValue(Everything.class, "longVal"), 10L);
        assertEquals(provider.annotationMetadata().longArrayValue(Everything.class, "longArr"), List.of(11L, 12L));
        assertEquals(provider.annotationMetadata().floatValue(Everything.class, "floatVal"), 13.1F);
        assertEquals(provider.annotationMetadata().floatArrayValue(Everything.class, "floatArr"), List.of(14.2F, 15.3F));
        assertEquals(provider.annotationMetadata().doubleValue(Everything.class, "doubleVal"), 16.4D);
        assertEquals(provider.annotationMetadata().doubleArrayValue(Everything.class, "doubleArr"), List.of(17.5D, 18.6D));
        assertTrue(provider.annotationMetadata().booleanValue(Everything.class, "boolVal"));
        assertEquals(provider.annotationMetadata().booleanArrayValue(Everything.class, "boolArr"), List.of(true, false));
        assertEquals(provider.annotationMetadata().enumValue(Everything.class, "enumeration"), demo.app.Everything.Letter.A);
        assertEquals(provider.annotationMetadata().enumArrayValue(Everything.class, "enumerations"), List.of(demo.app.Everything.Letter.B, demo.app.Everything.Letter.C));
        assertEquals(provider.annotationMetadata().classValue(Everything.class, "classVal"), String.class);
        assertEquals(provider.annotationMetadata().classArrayValue(Everything.class, "classArr"), List.of(Integer.class, Long.class));

        annotationVal = provider.annotationMetadata().annotationValue(Everything.class, "annotationVal");
        assertEquals(annotationVal.type(), CustomQualifier.class);
        assertEquals(annotationVal.stringValue("name"), "nested");
        assertEquals(annotationVal.enumValue("color"), CustomQualifier.Color.RED);

        annotationArr = provider.annotationMetadata().annotationArrayValue(Everything.class, "annotationArr");
        assertEquals(annotationArr.size(), 2);
        first = annotationArr.get(0);
        assertEquals(first.type(), CustomQualifier.class);
        assertEquals(first.stringValue("name"), "two");
        assertEquals(first.enumValue("color"), CustomQualifier.Color.BLACK);
        second = annotationArr.get(1);
        assertEquals(second.type(), CustomQualifier.class);
        assertEquals(second.stringValue("name"), "three");
        assertEquals(second.enumValue("color"), CustomQualifier.Color.RED);

        assertEquals(provider.annotationMetadata().objectValue(Everything.class, "string"), "a");
    }

    @Test
    public void activation() {
        assertTrue(providerRegistry().getProviderOpt(TCActivation.class).isPresent());
    }

    @Test
    public void imported() {
        Widget rotate = providerRegistry().getInstance(Widget.class);
        assertEquals(rotate.wonk(), "rotate");
        assertSame(providerRegistry().getProvider(Widget.class).scope(), ThreadLocal.class);
        Widget spring = providerRegistry().getInstance(Widget.class, "spring");
        assertEquals(spring.wonk(), "spring");
    }
}
