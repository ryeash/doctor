# doctor-aop

Doctor plugin supporting aspect oriented programming.

Supports constructor and factory providers, as well as interfaces and concrete classes.

### Basics

First, create an aspect class:

```java
@Singleton // aspects must have a scope to be eligible for injection
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
@Aspects(TimingAspect.class) 
// ^ aspects at the class level apply to all methods
// they can also be applied to individual methods 
public class Thing {
    public void doSomething(){
        System.out.println("something");
    }
}
```

Now, when you get an instance of Thing, all method calls will use the TimingAspect:

```java
Thing thing = doctor.getInstance(Thing.class);
thing.doSomething() // <- method will be both timed and observed
```

### Supported Aspects

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
    }

    @Override
    public void after(MethodInvocation invocation) {
        // inspect or modify the result of an invocation
    }
}
```