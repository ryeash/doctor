# doctor-rest-service

Netty based jax-rs-like webservice library.

### Basics

With the doctor-rest-service library included on the compile/runtime paths just set the bind addresses for netty in your
configuration, example:

```java
Doctor.load(DefaultConfigurationFacade.defaultConfigurationFacade()
        .addSource(new MapConfigurationSource("doctor.netty.bind","localhost:8080");
```

and create your service endpoints:

```java
// to be initialized correctly endpoint classes must
// include both the @Path annotation and a scope
@Singleton
@Path("/netty")
public class TCNettyEndpoint {
    @GET
    @Path("/hello")
    public String basic(@QueryParam("q") Optional<String> q) {
        ...
    }
}
```

### Configuration

| Property                                  | Description                                                                    | Default                                                                               |
|-------------------------------------------|--------------------------------------------------------------------------------|---------------------------------------------------------------------------------------|
| doctor.netty.bind                         | The host:port list of interfaces to bind to                                    | Empty; the netty server will not start if the bind addresses have not been configured |
| doctor.netty.tcp.threads                  | The number of threads to use for the TCP event loop                            | 1                                                                                     |
| doctor.netty.tcp.threadPrefix             | The thread prefix to use for the TCP event loop threads                        | "netty-tcp"                                                                           |
| doctor.netty.worker.threads               | The number of threads to use for the request thread pool                       | 16                                                                                    |
| doctor.netty.worker.threadPrefix          | The thread prefix to use for the request thread pool threads                   | "netty-worker"                                                                        |
| doctor.netty.tcp.socketBacklog            | The number of sockets to queue for processing before rejecting the connections | 1024                                                                                  |
| doctor.netty.ssl.selfSigned               | Whether or not to enable TLS with a self-signed certificate                    | false                                                                                 |
| doctor.netty.ssl.keyCertChainFile         | If set, will enable TLS using the defined cert chain file                      | Empty                                                                                 |
| doctor.netty.ssl.keyFile                  | The ssl key file; required when keyCertChainFile is set                        | Empty                                                                                 |
| doctor.netty.ssl.keyPassword              | The password for the ssl key file                                              | Empty                                                                                 |
| doctor.netty.http.maxInitialLineLength    | The maximum size for the HTTP request line                                     | 8192                                                                                  |
| doctor.netty.http.maxHeaderSize           | The maximum size for request headers                                           | 8192                                                                                  |
| doctor.netty.http.maxChunkSize            | The maximum size for chunks read from request bodies                           | 8192                                                                                  |
| doctor.netty.http.validateHeaders         | Whether or not to have the netty http library validate headers                 | false                                                                                 |
| doctor.netty.http.initialBufferSize       | The initial size of the request processing buffer                              | 8192                                                                                  |
| doctor.netty.http.maxContentLength        | The maximum size allowed for request bodies                                    | 8388608 (8 megabytes)                                                                 |
| doctor.netty.http.caseInsensitiveMatching | Whether or not to match routes with strict case-sensitivity                    | false (case matters on request uris)                                                      |
