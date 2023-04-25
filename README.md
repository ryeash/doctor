# doctor

Compile time dependency injection processor for JDK 18.

## Getting Started

Include the `doctor-processor` and `doctor-core` libraries in your build as well as any other plugins that you will
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
relies on `java.util.ServiceLoader` to load/initialize the application.

To say it in another way, it analyzes the source code to generate implementations of
[Provider](https://jakarta.ee/specifications/platform/8/apidocs/javax/inject/provider) and the providers are wired
together for dependency injection.

### Defining providers

There are two ways to define providers: annotated classes, and factory methods.

##### Class level

Any scope annotation attached to a class declaration will tell the processor to create a provider for the class, e.g.:

```java

@Singleton // <-- the provider scope
public class JdbcDao {
    // etc...
}
```

##### [@Factory](doctor-core/src/main/java/vest/doctor/Factory.java)

This factory method in this class does a similar thing (though having both in your project will cause a compilation
error, so
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
> Parameterized types are allowed as factory return types as long as the factory is qualified, e.g.:
> ```java
> @Factory
> @Named("stringThing") // <-- required or else it's a compilation error
> public List<String> parameterizedFactory(){
>   return List.of("a", "b", "c");
> }
> ```
>
> Multi-type type parameters are supported, e.g.:
> ```java
> @Factory
> public <T extends BookDao & PurchaseDao> T multiDaoFactory(){
>   return ...
> }
> ```
> The resulting provider will satisfy both BookDao and PurchaseDao dependencies. The primary
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
- [@Reloadable](doctor-core/src/main/java/vest/doctor/Reloadable.java): an instance is created and cached until a
  [ReloadProviders](doctor-core/src/main/java/vest/doctor/event/ReloadProviders.java) event is published, at which point
  the cached instance will be cleared and a new instance will be created the next time an instance is requested (lazily)

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
@Cached("1 day")
public class MorningRoutine {
  @Inject
  public void wakeup(@Named("his") CoffeeMaker his, @Named("hers") CoffeeMaker hers) {
    hers.brew();
    his.brew();
  }
}
```

Only one qualifier is allowed per provided type.

Doctor does not provide any built-in qualifiers beyond
the [@Named](https://jakarta.ee/specifications/platform/8/apidocs/javax/inject/named)
qualifier packaged in java.inject. To create qualifiers see example here:
[@Qualifier](https://jakarta.ee/specifications/platform/8/apidocs/javax/inject/qualifier).


### [@Activation](doctor-core/src/main/java/vest/doctor/Activation.java)

Providers can be conditionally activated based on predicates defined in an @Activation annotation
attached to the provider source.

Example:
First create an activation predicate:

```java
public class IsActivationPropertyPresent implements BiPredicate<ProviderRegistry, DoctorProvider<?>> {
    // NOTE: activation predicates MUST have a public no-arg constructor
    @Override
    public boolean test(ProviderRegistry providerRegistry, DoctorProvider<?> doctorProvider) {
        // will activate providers based on a property value
        return providerRegistry.configuration().get("activateOptionals", false, Boolean::parseBoolean);
    }
}
```

Then use the @Activation annotation to attach the predicate to a provider:

```java
@Singleton
@Activation(IsActivationPropertyPresent.class)
public class OptionalThing {
  // OptionalThing provider will only be registered when activateOptionals=true
}
```

@Activation can be used on an annotation type to bundle multiple predicates under a single named annotation:

```java
@Documented
@Retention(RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Activation({DevStackPredicate.class, OnlyOnLinuxPredicate.class, OnlyWithTestDBPredicate.class})
public @interface TestStackOnly {
}
```

```java
@Singleton
@TestStackOnly
public class OptionalThing {
  // only available if all three predicates on TestStackOnly activation annotation evaluate true
}
```

A note on factory providers: Activation predicates marked at the class level are inherited by
factory providers. So in this example:

```java
@Configuration
@Activation(SomePredicate.class)
public class AppConfig {
    @Factory
    @Singleton
    @Activation(AnotherPredicate.class)
    public CoffeeMaker coffeeMakerFactory(){
        return ...
    }
}
```

The provider generated from coffeeMakerFactory will be subject to both SomePredicate and AnotherPredicate.


#### [@Modules](doctor-core/src/main/java/vest/doctor/Modules.java)

@Modules is a built-in @Activation that enables providers based on ProviderRegistry.getActiveModules().

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

Any provider that has modules will _only_ be active if the app is started with one of the modules listed in the
activation list. Providers without modules (or any other @Activation requirements) will _always_ be active.

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
the `null` qualifier; making the provider available with and without its qualifier.

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

This will make the datasource retrievable with the qualifier or without:

```java
doctor.getInstance(DataSource.class) == doctor.getInstance(DataSource.class, "primary")
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

Basics of event publish and consume:

```java
// This class implements EventConsumer and will receive String events.
@Singleton
public class EventExample implements EventConsumer<String> {

  private final EventBus bus;

  @Inject
  public EventExample(EventBus bus) {
    this.bus = bus;
  }
  
  @Inject
  public void message(@Named("background") ExecutorService exec) {
    // publish a string event when this class is instantiated 
    exec.submit(() -> bus.publish("test"));
  }

  @Override
  public void receive(String message) {
    System.out.println("message received: " + message);
  }
}
```


### [@Import](doctor-core/src/main/java/vest/doctor/Import.java)

If you use library dependencies that use jakarta.inject-api annotations (or doctor-core annotations)
on their classes and want to include those classes in the processing of providers, you can use the @Import
annotation to make the external packages known to the compilation environment.

Typical usage is to add the @Import to a provider factory class:

```java
@Configuration
@Import({"org.company", "org.company.ext"})
public class AppConfig {
  @Factory
  @Singleton
  public Thing myThing(){
        return new Thing();
    }
}
```

This will cause all top level (note: non-recursive) classes and interfaces from the packages `org.company`
and `org.company.ext` to be processed for relevant annotations by the doctor-processor, making available
any providers that it finds.

### Properties and Configuration

Configuration properties are retrieved via the
[ConfigurationFacade](doctor-core/src/main/java/vest/doctor/ConfigurationFacade.java).

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

Additionally, using [@Properties](doctor-core/src/main/java/vest/doctor/Properties.java), a properties interface
can be defined and then auto generated at compile time to provide a concrete class encapsulating the properties
for an application.

```java

@Singleton
@Properties("db.") // all property names marked on methods will be prefixed with `db.`
public interface DBProps { // must be an interface
  @Property("url") // this will use the property named `db.url`
  String url();

  @Property("username")
  String username();

  @Property("password")
  String password();
  
  ...
}
```

#### Injectable property types

Properties are defined as strings and require conversion to specific types when injected. Out of the box, these
conversions are supported:

* primitives and their associated java.lang wrappers
* any type that has a public constructor with a single string argument and does not throw an exception (e.g. BigInteger)
* any type that has a public static method that takes a single string argument and does not throw an exception (like
  URI::create)

If these built in conversions do not satisfy your requirements, the string conversion system can be extended by creating
implementations of
[StringConversionGenerator](doctor-core/src/main/java/vest/doctor/processing/StringConversionGenerator.java)
and wiring them in via
[ProcessorConfiguration](doctor-core/src/main/java/vest/doctor/processing/ProcessorConfiguration.java).

# Aspect Oriented Programming (AOP)

AOP is supported for any provided type that is an interface or a public, non-final class with a zero-arg constructor.

### Basics

First, create an aspect class:

```java
@Prototype // aspects must have a scope to be eligible for use
public class TimingAspect implements Aspect {
    @Override
    public Object execute(MethodInvocation methodInvocation) {
        return timeAndObserve(methodInvocation::next);
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
Thing thing = doctor.getInstance(Thing.class);
thing.doSomething() // <- method will be both timed and observed
```

#### A note on Aspect scoping

In the previous example, the TimingAspect class is marked as @Prototype, but each instance of the aspect-ed class will
call Provider.get() once. So for the lifetime of the aspect-ed Thing singleton, only one instance of the TimingAspect
will be created and used.

## Property injection with annotation values

All string values in supported annotations can be parameterized using the macro format
`${property.name}`, e.g. `@Scheduled(interval = "${configurableInterval}")`. An unfortunate
side effect of this configurability is that these strings can not be validated at compile time.
In the previous example, if the realized value for the `configurableInterval` property
was invalid, it would not be known at compile time, and only checked at runtime.

# Limitations

Field injection is not supported. It requires reflective access to fields and requires changing access levels at
runtime. Neither of which is allowed for this project.
