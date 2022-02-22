package demo.app.reactor;

import demo.app.Person;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.multipart.HttpData;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import org.testng.Assert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vest.doctor.reactor.http.DELETE;
import vest.doctor.reactor.http.GET;
import vest.doctor.reactor.http.HttpRequest;
import vest.doctor.reactor.http.HttpResponse;
import vest.doctor.reactor.http.OPTIONS;
import vest.doctor.reactor.http.POST;
import vest.doctor.reactor.http.PUT;
import vest.doctor.reactor.http.Param;
import vest.doctor.reactor.http.Param.Attribute;
import vest.doctor.reactor.http.Param.Bean;
import vest.doctor.reactor.http.Param.Body;
import vest.doctor.reactor.http.Param.Context;
import vest.doctor.reactor.http.Param.Cookie;
import vest.doctor.reactor.http.Param.Header;
import vest.doctor.reactor.http.Param.Provided;
import vest.doctor.reactor.http.Param.Query;
import vest.doctor.reactor.http.Path;
import vest.doctor.reactor.http.RequestContext;
import vest.doctor.reactor.http.ResponseBody;
import vest.doctor.reactor.http.RunOn;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

@Singleton
@Path("/root")
public class ReactorEndpoint {

    @GET
    @Path("/hello")
    public String hello() {
        return "Hello World!";
    }

    @POST
    @Path("/throughput")
    public byte[] throughput(@Body byte[] body) {
        return body;
    }

    @GET
    @Path("/params/{path}")
    @RunOn("background")
    public String params(@Param.Path String path,
                         @Query Integer size,
                         @Header("X-Param") String param,
                         @Cookie String cook,
                         @Bean BeanParamObject bean,
                         @Provided ProvidedThing thing,
                         @Provided Provider<ProvidedThing> thingProvider,
                         @Attribute("test.attribute") String attribute,
                         @Context RequestContext ctx,
                         @Context HttpRequest request,
                         @Context HttpResponse response) {
        Assert.assertNotNull(ctx);
        Assert.assertNotNull(request);
        Assert.assertNotNull(response);
        System.out.println(Thread.currentThread().getName() + "  " + ctx);
        return path + " " + size + " " + param + " " + cook + " " + bean + " " + thing + " " + thingProvider.get() + " " + attribute;
    }

    @POST
    public String noPath() {
        return "noPath";
    }

    @GET
    @POST
    @Path({"/multimethod", "/multimethod2"})
    public String multiMethodAndPath(@Context HttpRequest request) {
        return request.method().toString();
    }

    @POST
    @Path("/json")
    public Person json(@Body Person person) {
        return person;
    }

    @POST
    @Path("/jsonpub")
    public Publisher<Person> json(@Body Publisher<Person> person) {
        return person;
    }

    @PUT
    @Path("/echo")
    public Publisher<ByteBuf> echo(@Body Publisher<ByteBuf> dataStream) {
        return Flux.from(dataStream)
                .map(ByteBuf::retain);
    }

    @DELETE
    @Path("/responseobject")
    public HttpResponse responseObject(@Context HttpResponse response) {
        return response.header("X-RouteHeader", "true")
                .body(ResponseBody.of("responseObject"));
    }

    @DELETE
    @Path("/responseobjectpub")
    public Publisher<HttpResponse> responseObjectPub(@Context HttpResponse response) {
        return Mono.just(response)
                .map(r -> r.header("X-RouteHeader", "true")
                        .body(ResponseBody.of("responseObjectPub")));
    }

    @GET
    @Path("/future")
    public CompletableFuture<String> futureString() {
        return CompletableFuture.supplyAsync(() -> "future", ForkJoinPool.commonPool());
    }

    @GET
    @Path("/locale")
    public String locale(@Header("Accept-Language") Locale locale) {
        return locale.toString();
    }

    @POST
    @Path("/multipart")
    public Publisher<String> multipart(@Body Publisher<HttpData> form) {
        return Flux.from(form)
                .map(data -> data.content().toString(StandardCharsets.UTF_8))
                .collect(Collectors.joining());
    }

    @GET
    @Path("/splat/**")
    public String splat(@Context RequestContext ctx) {
        return ctx.request().uri().toString();
    }

    @OPTIONS
    @Path("/asyncError")
    public Publisher<String> asyncError() {
        return Flux.error(new IOException("error"));
    }

    @OPTIONS
    @Path("/syncError")
    public ByteBuf syncError() throws IOException {
        throw new IOException("error");
    }
}
