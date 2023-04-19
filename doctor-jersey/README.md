doctor-jersey
-------------
A JAX-RS HTTP server provider backed by netty and jersey, and wired together using doctor injection.

To enable, add the [@JerseyFeature](/src/main/java/vest/doctor/jersey/JerseyFeature.java) annotation
to any provided type (typically a bean factory class):

```java

@Singleton
@JerseyFeature
public class AppConfig {
    // ...
}
```

And ensure at least one bind address is set:

```properties
doctor.jersey.http.bind = 0.0.0.0:19998
```

## Unsupported JAX-RS features

- Field injection for request scoped values is not supported
- The HttpServletResponse can not be used as a resource method parameter

## Extensions to JAX-RS

- Any type provided via the ProviderRegistry that started the server is injectable as a resource method parameter using
  the [@Provided](/src/main/java/vest/doctor/jersey/Provided.java) annotation.
- Any attribute attached to the request in a filter, via ContainerRequestContext.setProperty is injectable as a resource
  method parameter using the [@Attribute](/src/main/java/vest/doctor/jersey/Attribute.java) annotation.