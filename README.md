# doctor

Compile time dependency injection processor for JDK 15.

## Getting Started

Include the `doctor-processor` and `doctor-runtime` libraries in your build as well as any other plugins that you will
use. Then create a main method that loads the doctor with the desired configuration.

```java
public static void main(String[]args){
    Doctor.load(DefaultConfigurationFacade.defaultConfigurationFacade());
}
```

## Core Functionality

Pseudo-support
for [jakarta.inject](https://jakarta.ee/specifications/platform/8/apidocs/javax/inject/package-summary.html). During
compile, [@Scope](https://jakarta.ee/specifications/platform/8/apidocs/javax/inject/scope) annotations (and others) are
analyzed, and the boilerplate code to generate instances for the types is written and wired into an infrastructure that
relies on `ServiceProvider` to load/initialize the application.

To say it in another way, it analyzes the source code to generate implementations of
[Provider](https://jakarta.ee/specifications/platform/8/apidocs/javax/inject/provider) and the providers are wired
together for dependency injection.

### Defining providers

There are two ways to define providers: annotated classes, and factory methods.

##### Class level

The following will tell the processor to create a provider for the JdbcDao:

```java

@Singleton
public class JdbcDao {
    // etc...
}
```

##### [@Factory](doctor-core/src/main/java/vest/doctor/Factory.java)

This method in this class does a similar thing (though having both in your project will cause a compilation error, so
just pick one):

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

As a result you can inject the JdbcDao:

```java
@Singleton
public class BookDao {
    @Inject // inject here tells the processor which constructor to use for dependency injection
    public BookDao(JdbcDao dao) {
      // do book things
    }
}
```

> ##### Notes on factory return types
>
> Parameterized types are not allowed as factory return types, e.g.:
> ```java
> @Factory
> public List<String> stringsFactory(){
>   return List.of("a", "b", "c");
> }
> ```
> This will cause a compilation error.
>
> Multi-type type parameters, however, _are_ supported, e.g.:
> ```java
> @Factory
> public <T extends BookDao & PurchaseDao> T multiDaoFactory(){
>   return ...
> }
> ```
> The resulting provider will satisfy both BookDao and PurchaseDao dependencies. The main
> type for the resulting provider will be the first listed type (in this case BookDao), with any additional
> types (PurchaseDao) being treated as satisfied super types. This means for the purposes of verifying
> duplicate providers only the first bound will be considered.
>

### Scoping

These are the built-in scopes supported:

- [@Prototype](doctor-core/src/main/java/vest/doctor/Prototype.java): each call to Provider.get() creates a new instance
- [@Singleton](https://jakarta.ee/specifications/platform/8/apidocs/javax/inject/singleton): one and only one instance
  is created per jvm
- [@ThreadLocal](doctor-core/src/main/java/vest/doctor/ThreadLocal.java): one instance is created per thread
- [@Cached](doctor-core/src/main/java/vest/doctor/Cached.java): an instance is created and shared for a configurable
  length of time

### Qualifiers

All providers have a qualifier, even when one isn't defined it's considered the `null` qualifier and that provider is
the default provider for the type. Qualifiers allow applications to differentiate between multiple variations of the
same provided type. For example:

```java
public class AppConfig {
  @Singleton
  @Factory
  @Named("his") // <- this is the qualifier
  public CoffeeMaker his() {
    return new FrenchPress();
  }

  @Singleton
  @Factory
  @Named("hers")
  public CoffeeMaker hers() {
    return new PourOver();
  }
}
```

Here we've created two CoffeeMaker providers each with its own name. Attempting to do this without using qualifiers
would cause a compilation error because type and qualifier define the uniqueness of providers.

Both of these coffee makers can be injected into a target using their qualifiers:

```java
public class MorningRoutine {
  @Inject
  public void wakeup(@Named("his") CoffeeMaker his, @Named("hers") CoffeeMaker hers) {
    hers.brew();
    his.brew();
  }
}
```

Only one qualifier is allowed per provided type.

The [@Named](https://jakarta.ee/specifications/platform/8/apidocs/javax/inject/named) qualifier is the only qualifier
included with doctor. To create qualifiers see example here:
[@Qualifier](https://jakarta.ee/specifications/platform/8/apidocs/javax/inject/qualifier).

### [@Modules](doctor-core/src/main/java/vest/doctor/Modules.java)

Providers can be enabled for a specific set of modules.

```java
@Singleton
@Modules({"dev", "test"}) // this class will only be available when either the "dev" or "test" module is active.
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

### [@Eager](doctor-core/src/main/java/vest/doctor/Eager.java)

By default, providers will not instantiate an instance when they are initialized; they're lazy. If you want an instance
automatically created on startup, you can mark the class or factory method with `@Eager`.

```java
@Eager
@Singleton
public class Heater {
    // ...
}
```

On startup, one instance of Heater will be created automatically.

While `@Eager` can be used on any scope, it makes the most sense for singletons.

### [@Primary](doctor-core/src/main/java/vest/doctor/Primary.java)

A qualified provider definition can be marked with @Primary to register the provider with both it's marked qualifier and
the `null` qualifier; effectively making the provider the default provider for the type.

```java
@Singleton
public class AppConfig {
    @Singleton
    @Primary
    @Named("primary")
    public DataSource primaryDataSource() {
        return ...
    }
}
```

The previous will allow the datasource to be retrieved either with the qualifier or without:

```java
doctor.getInstance(DataSource.class) == doctor.getInstance(DataSource.class, "primary")
```

### [@SkipInjection](doctor-core/src/main/java/vest/doctor/SkipInjection.java)

There are rare occasions where it may be necessary to skip the post-instantiation injection phase for a provided
instance (Skip calling `@Inject` marked methods and similar processing). In these cases use `@SkipInjection`.

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

### [@Scheduled](doctor-core/src/main/java/vest/doctor/scheduled/Scheduled.java)

Methods in provided objects can be scheduled for periodic execution using the `@Scheduled` annotation.

```java

@Singleton
public class SomethingPeriodic {
    @Scheduled(interval = "10ms")
    public void every10Milliseconds() {
        // do something every 10 milliseconds
    }

    @Scheduled(cron = "0 0 * * * *")
    public void everyHourOnTheHour() {
        // do something every hour
    }
}
```

Internally, the object instances for scheduled methods are tracked using weak references so scheduling method execution
will _not_ prevent the provided object from being garbage collected.

> Aspect interactions:
> Due to the initialization order for providers, scheduled methods will use the non-aspected instance
> of provided objects. In effect, if a method is marked with @Scheduled and an aspect, the aspect will
> be ignored during execution of the resulting scheduled task; however, any place where the class is
> injected, calling the scheduled method explicitly will go through the aspect(s).

### Event Bus

Messages can be published and consumed via the EventBus.

Basics of event produce and consume:

```java
@Singleton
public class EventExample implements EventConsumer<String> {

  private final EventProducer producer;

    @Inject
    public EventExample(EventProducer producer) {
      this.producer = producer;
    }

  @Inject
  @Async
  public void message() {
    // publish a string event when this class is instantiated 
    producer.publish("test");
  }

  @Override
  public void receive(String message) {
    System.out.println("message received: " + message);
  }
}
```

### [@Async](doctor-core/src/main/java/vest/doctor/Async.java)

The @Async annotation can be used to perform certain actions in a background thread.

```java

@Singleton
public class AsyncDemo {
    @Inject
    @Async // this method will be called asynchronously when this class is instantiated
    public void injectAsync() {
        ...
    }
}
```

### Properties and Configuration

Configuration is orchestrated via the ConfigurationFacade class.

#### [@Property]((doctor-core/src/main/java/vest/doctor/Property.java))

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

Additionally, using [@Properties](doctor-core/src/main/java/vest/doctor/Properties.java), a properties class can be auto
generated to provide a concrete class encapsulating the properties for an application.

```java

@Singleton
@Properties("db.") // all property names marked on methods will be prefixed with `db.`
public interface DBProps { // must be an interface
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

# Aspect Oriented Programming (AOP)

AOP is supported for any provided type that is an interface or a public, non-final class.

### Basics

First, create an aspect class:

```java
@Prototype // aspects must have a scope to be eligible for use
public class TimingAspect implements Around {
    @Override
    public void execute(MethodInvocation methodInvocation) {
        timeAndObserve(methodInvocation::invoke);
    }
}
```

Then, apply the aspect to a provided type:

```java
@Singleton
@Aspects({TimingAspect.class})
// ^ aspects at the class level apply to all methods
// they can also be applied to individual methods 
public class Thing {
    public void doSomething() {
        System.out.println("something");
    }
}
```

or if the type is provided via a factory method:

```java

@Singleton
public class AppConfig {
  @Factory
  @Singleton
  @Aspects({TimingAspect.class})
  public Thing coffeeMakerAspect() {
    return ...create a thing...;
  }
}
```

Now, when you get an instance of Thing, all method calls will use the TimingAspect:

```java
Thing thing=doctor.getInstance(Thing.class);
thing.doSomething() // <- method will be both timed and observed
```

#### A note on Aspect scoping

In the previous example, the TimingAspect class is marked as @Prototype, but each instance of the aspected class will
call Provider.get() once. So for the lifetime of the aspected Thing class, only one instance of the TimingAspect will be
created and used.

### Aspect Stages

There are three stages of aspect injection: before, around, and after; each aspect can implement any or all of them.

```java
@Singleton
public class AspectDemo implements Before, Around, After {

    @Override
    public void before(MethodInvocation invocation) {
        // inspect or modify arguments to a method call
    }

    @Override
    public void execute(MethodInvocation methodInvocation) {
        // should call methodInvocation.invoke()
        // this is the only stage where invoke() can be called
    }

    @Override
    public void after(MethodInvocation invocation) {
        // inspect or modify the result of an invocation
    }
}
```

# Limitations

Field injection is not supported. It requires reflective access to fields and requires changing access levels at
runtime. Neither of which is allowed for this project.
