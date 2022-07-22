package demo.app.service;

import demo.app.Person;
import io.netty.buffer.ByteBuf;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.testng.Assert;
import vest.doctor.Eager;
import vest.doctor.http.server.MultiPartData;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.RequestContext;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.reactive.Rx;
import vest.doctor.restful.http.Endpoint;
import vest.doctor.restful.http.HttpMethod.ANY;
import vest.doctor.restful.http.Param.Attribute;
import vest.doctor.restful.http.Param.Bean;
import vest.doctor.restful.http.Param.Body;
import vest.doctor.restful.http.Param.Context;
import vest.doctor.restful.http.Param.Cookie;
import vest.doctor.restful.http.Param.Header;
import vest.doctor.restful.http.Param.Path;
import vest.doctor.restful.http.Param.Provided;
import vest.doctor.restful.http.Param.Query;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

import static vest.doctor.restful.http.HttpMethod.DELETE;
import static vest.doctor.restful.http.HttpMethod.GET;
import static vest.doctor.restful.http.HttpMethod.OPTIONS;
import static vest.doctor.restful.http.HttpMethod.POST;
import static vest.doctor.restful.http.HttpMethod.PUT;

@Singleton
@Eager
@Endpoint("/root")
public class ReactorEndpoint {

    @GET
    @Endpoint("/hello")
    public String hello() {
        return "Hello World!";
    }

    @POST
    @Endpoint("/throughput")
    public byte[] throughput(@Body byte[] body) {
        return body;
    }

    @GET
    @Endpoint("/params/{path}")
    public String params(@Path String path,
                         @Query Integer size,
                         @Header("X-Param") String param,
                         @Cookie String cook,
                         @Bean BeanParamObject bean,
                         @Provided ProvidedThing thing,
                         @Provided Provider<ProvidedThing> thingProvider,
                         @Attribute("test.attribute") String attribute,
                         @Context RequestContext ctx,
                         @Context Request request,
                         @Context Response response) {
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
    @Endpoint({"/multimethod", "/multimethod2"})
    public String multiMethodAndPath(@Context Request request) {
        return request.method().toString();
    }

    @POST
    @Endpoint("/json")
    public Person json(@Body Person person) {
        return person;
    }

    @POST
    @Endpoint("/jsonpub")
    public Flow.Publisher<Person> json(@Body Flow.Publisher<Person> person) {
        return person;
    }

    @PUT
    @Endpoint("/echo")
    public Flow.Publisher<ByteBuf> echo(@Body Flow.Publisher<ByteBuf> dataStream) {
        return Rx.from(dataStream)
                .map(ByteBuf::retain);
    }

    @DELETE
    @Endpoint("/responseobject")
    public Response responseObject(@Context Response response) {
        return response.header("X-RouteHeader", "true")
                .body(ResponseBody.of("responseObject"));
    }

    @DELETE
    @Endpoint("/responseobjectpub")
    public Flow.Publisher<Response> responseObjectPub(@Context Response response) {
        return Rx.one(response)
                .map(r -> r.header("X-RouteHeader", "true")
                        .body(ResponseBody.of("responseObjectPub")));
    }

    @GET
    @Endpoint("/future")
    public CompletableFuture<String> futureString() {
        return CompletableFuture.supplyAsync(() -> "future", ForkJoinPool.commonPool());
    }

    @GET
    @Endpoint("/locale")
    public String locale(@Header("Accept-Language") Locale locale) {
        return locale.toString();
    }

    @POST
    @Endpoint("/multipart")
    public Flow.Publisher<String> multipart(@Body Flow.Publisher<MultiPartData.Part> form) {
        return Rx.from(form)
                .map(data -> data.data().toString(StandardCharsets.UTF_8))
                .collect(Collectors.joining());
    }

    @GET
    @Endpoint("/splat/**")
    public String splat(@Context RequestContext ctx) {
        return ctx.request().uri().toString();
    }

    @OPTIONS
    @Endpoint("/asyncError")
    public Flow.Publisher<String> asyncError() {
        return Rx.error(new IOException("error"));
    }

    @OPTIONS
    @Endpoint("/syncError")
    public ByteBuf syncError() throws IOException {
        throw new IOException("error");
    }

    @KILL
    @Endpoint("/kill")
    public String customMethod() {
        return "kill";
    }

    @ANY
    @Endpoint("/anymethod")
    public String anyMethod(@Context io.netty.handler.codec.http.HttpMethod method) {
        return method.toString();
    }

}
