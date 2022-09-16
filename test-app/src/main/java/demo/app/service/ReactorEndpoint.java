package demo.app.service;

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
import vest.doctor.http.server.rest.Endpoint;
import vest.doctor.http.server.rest.HttpMethod.ANY;
import vest.doctor.http.server.rest.HttpMethod.DELETE;
import vest.doctor.http.server.rest.HttpMethod.GET;
import vest.doctor.http.server.rest.HttpMethod.OPTIONS;
import vest.doctor.http.server.rest.HttpMethod.POST;
import vest.doctor.http.server.rest.HttpMethod.PUT;
import vest.doctor.http.server.rest.Param.Attribute;
import vest.doctor.http.server.rest.Param.Bean;
import vest.doctor.http.server.rest.Param.Body;
import vest.doctor.http.server.rest.Param.Context;
import vest.doctor.http.server.rest.Param.Cookie;
import vest.doctor.http.server.rest.Param.Header;
import vest.doctor.http.server.rest.Param.Path;
import vest.doctor.http.server.rest.Param.Provided;
import vest.doctor.http.server.rest.Param.Query;
import vest.doctor.reactive.Rx;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

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
                .map(data -> {
                    try {
                        return data.data().toString(StandardCharsets.UTF_8);
                    } finally {
                        data.data().release();
                    }
                })
                .collect(Collectors.joining());
    }

    @GET
    @Endpoint("/splat/*")
    public String splat(@Context RequestContext ctx,
                        @Path("*") String splat) {
        return ctx.request().uri().toString() + " " + splat;
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
