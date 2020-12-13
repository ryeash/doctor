# doctor-jaxrs

Doctor plugin supporting JAX-RS webservices using Jetty and RESTeasy.

### Basics

With the doctor-jaxrs library included on the compile/runtime paths set the bind addresses for jaxrs in your
configuration, example:

```java
Doctor.load(DefaultConfigurationFacade.defaultConfigurationFacade()
        .addSource(new MapConfigurationSource(
        "jaxrs.bind","localhost:8080")));
```

and create your service endpoints:

```java
// to be initialized correctly endpoint classes must
// include both the @Path annotation and a scope
@Path("/rest")
@Singleton
@Consumes({MediaType.APPLICATION_JSON})
@Produces({MediaType.APPLICATION_JSON})
public class RestEndpoint {

    @GET
    @Path("/hello")
    public Response goodbyeWorld() {
        return Response.ok().build();
    }
}
```

### Configuration

| Property                    | Description                                                                                                   | Default                                                                               |
|-----------------------------|---------------------------------------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| jaxrs.bind                  | The host:port list of interfaces to bind to                                                                   | Empty; the jaxrs server will not start if the bind addresses have not been configured |
| jaxrs.rootPath              | The root path for all endpoints; will prefix all route paths                                                  | "/"                                                                                   |
| jaxrs.maxRequestThreads     | The maximum threads to use to service requests                                                                | 16                                                                                    |
| jaxrs.minRequestThreads     | The minimum threads to keep alive to service requests                                                         | 1                                                                                     |
| jaxrs.threadPrefix          | The prefix for the thread names servicing requests                                                            | "request"                                                                             |
| jaxrs.threadIdleTimeout     | The duration in milliseconds to keep idle request threads alive                                               | 120000                                                                                |
| jaxrs.minGzipSize           | The minimum response body size to gzip; responses smaller than this size (in bytes) will not be gzipped       | 814                                                                                   |
| jaxrs.maxRequestHeaderSize  | The max size in bytes for request headers; headers beyond this size will be terminated with an exception      | 8192                                                                                  |
| jaxrs.maxResponseHeaderSize | The max size in bytes for the response headers; headers beyond this size will be terminated with an exception | 8192                                                                                  |
| jaxrs.maxRequestBodySize    | The max size in bytes for the request body; bodies beyond this size will be terminated with an exception      | 2097152 (2 megabytes)                                                                 |
| jaxrs.socketIdleTimeout     | The duration in milliseconds to keep idle tcp connections open                                                | 60000                                                                                 |
| jaxrs.socketBacklog         | The number of sockets to queue for processing before rejecting the connections                                | 1024                                                                                  |
| jaxrs.websocketRootPath     | The root path for websocket endpoints; this path will prefix all websocket endpoint paths                     | "/ws"                                                                                 |