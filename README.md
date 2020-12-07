# doctor

Compile time dependency injection provider.

### Core Functionality

Pseudo-support for `javax.inject`. During compile, `@Scope` annotations (and others) are analyzed and the boilerplate
code to generate instances for the types is generated and wired into an infrastructure that relies on `ServiceProvider`
to load/initialize the application.

To say it in another way, the source code is analyzed to generate implementations of `javax.inject.Provider` and the
providers are automatically wired together to support dependency injection.

### Defining providers

There are two fundamental ways to create providers: annotated classes, and factory methods.

##### Class level

The following will tell the processor to create a provider for the JdbcDao:

```java

@Singleton
public class JdbcDao {
    // etc...
}
```

##### Factory

This method in this class does a similar thing:

```java

@Singleton // classes with factories must be marked with a scope since they themselves will be exposed via a Provider
public class ApplicationBeanFactories {
    @Factory
    @Singleton
    public JdbcDao daoFactory() {
        return new JdbcDao();
    }
}
```

As a result in other classes you can inject the JdbcDao:

```java

@Singleton
public class BookDao {
    @Inject // inject here tells the processor which constructor to use for dependency injection
    public BookDao(JdbcDao dao) {
        // do book things
    }
}
```

### Scoping

These are the built-in scopes supported:

- @Prototype: each call to Provider.get() creates a new instance
- @Singleton: one and only one instance is created per jvm
- @ThreadLocal: one instance is created per thread
- @Cached: an instance is created and shared for a configurable length of time

### @Modules

Providers can be optionally enabled only for a specific set of modules. Modules are set during bootstrap of the
application.

```java

@Singleton
@Modules({"dev", "test"}) // this class will only be available when either the "dev" or "test" modules is active.
public class MockDao implements Dao {
}
```

To start the app with this provider enabled:

```java
public class App {
    public static void main(String[] args) {
        Doctor.load("dev");
        // or
        // Doctor.load("test");
        // or
        // Doctor.load("dev", "test");
    }
}
```

Any provider that has modules will _only_ be active if the app is started with one of the modules listed in the active
list. Providers without modules will _always_ be active.

### @Eager

By default, providers will not instantiate an instance when they are initialized, i.e. they're lazy. If you want an
instance automatically created on startup, you can mark the class or factory method with `@Eager`.

```java

@Eager
@Singleton
public class Heater {
    // ...
}
```

On startup, one instance of Heater will be created automatically.

While `@Eager` can be used on any scope, it makes the most sense for singletons.

### @Primary

A qualified provider definition can be marked with @Primary to register the provider with both it's marked qualifier and
the `null` qualifier; effectively making the provider the default provider for the type.

```java

@Singleton
public class AppConfig {
    @Singleton
    @Primary
    @Named("primary")
    public DataSource primaryDataSource() {
        return ...;
    }
}
```

The previous will allow the datasource to be retrieved either with the qualifier or without:

```java
doctor.getInstance(DataSource.class)==doctor.getInstance(DataSource.class,"primary")
```

### @SkipInjection

There are rare occasions where it may be necessary to skip the post-instantiation injection phase for a provided
instance
(Skip calling `@Inject` marked methods and similar processing). In these cases use `@SkipInjection`.

```java

@SkipInjectin
@Prototype
public class NoInject {
    @Inject
    public void postInstantiation() {
        // this method will not be called automatically during initialization
    }
}
```

### @Scheduled

Methods in provided objects can be scheduled for periodic execution using the `@Scheduled` annotation.

```java

@Singleton
public class SomethingPeriodic {
    @Scheduled(interval = "10ms")
    public void every10Milliseconds() {
        // do something every 10 milliseconds
    }
}
```

### Limitations

Field injection is not supported. It requires reflective access to fields and requires changing access levels at
runtime. Neither of which is allowed for this project.

### EventBus

Messages can be published and consumed via the EventBus.

Basics of event produce and consume:

```java

@Singleton
public class EventExample {

    private final EventProducer producer;

    @Inject
    public TCEvent(EventProducer producer) {
        this.producer = producer;
    }

    @Inject
    @Async
    public void message() {
        // publish a string event when this class is instantiated 
        producer.publish("test");
    }

    // create a consumer of string events
    @EventListener
    public void stringMessages(String message) {
        System.out.println("message received: " + message);
    }
}
```

### @Async

The @Async annotation can be used to perform certain actions on a background threads.

```java

@Singleton
public class AsyncDemo {
    @Inject
    @Async // this method will be called asynchronously when this class is instantiated
    public void injectAsync() {
        // ...
    }

    @EventListener
    @Async // when a compatible message is published, this method will be called in a background thread
    public void asyncListener(Object message) {
        // ...
    }
}
```

### Properties and Configuration

Configuration is orchestrated via the ConfigurationFacade class.

#### @Property

Properties from the ConfigurationFacade can be automatically injected into provided types using the @Property
annotation.

```java

@Prototype
public class PropertiesDemo {

    @Inject
    public PropertiesDemo(@Property("string") String stringProp,
                          @Property("string") char c,
                          @Property("boolean") Boolean bool,
                          @Property("boolean") boolean primBool,
                          @Property("string") Optional<String> optionalString) {
        ...
    }

    @Inject
    public void injectProperties(@Property("list") List<String> stringList,
                                 @Property("list") Collection<Integer> numberList,
                                 @Property("set") Set<String> set) {
        ...
    }
}
```

Additionally, using @Properties, a properties class can be auto generated to provide a portable concrete class
encapsulating the properties for an application.

```java

@Singleton
@Properties("db.") // all property names marked on this class will be prefixed with `db.`
public interface DBProps {
    @Property("url")
        // this will use the property named `db.url`
    String url();

    @Property("username")
    String username();

    @Property("password")
    String password();
    
    ...
}
```