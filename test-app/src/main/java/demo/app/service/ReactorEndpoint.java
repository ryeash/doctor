package demo.app.service;

import io.netty.buffer.ByteBuf;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.OPTIONS;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.testng.Assert;
import vest.doctor.Eager;

import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinPool;

@Singleton
@Eager
@Path("/root")
public class ReactorEndpoint {

    @GET
    @Path("/hello")
    public String hello() {
        return "Hello World!";
    }

    @POST
    @Path("/throughput")
    public byte[] throughput(byte[] body) {
        return body;
    }

    @GET
    @Path("/params/{path}")
    public String params(@PathParam("path") String path,
                         @QueryParam("size") Integer size,
                         @HeaderParam("X-Param") String param,
                         @CookieParam("cook") String cook,
                         @BeanParam BeanParamObject bean,
                         @Context Request request) {
        Assert.assertNotNull(request);
        System.out.println(Thread.currentThread().getName() + "  " + request);
        return path + " " + size + " " + param + " " + cook + " " + bean;
    }

    @POST
    public String noPath() {
        return "noPath";
    }

    @PUT
    @Path("/echo")
    public CompletableFuture<byte[]> echo(byte[] dataStream) {
        return CompletableFuture.completedFuture(dataStream);
    }

    @DELETE
    @Path("/responseobject")
    public Response responseObject() {
        return Response.ok("responseObject")
                .header("X-RouteHeader", "true")
                .build();
    }

    @GET
    @Path("/future")
    public CompletableFuture<String> futureString() {
        return CompletableFuture.supplyAsync(() -> "future", ForkJoinPool.commonPool());
    }

    @GET
    @Path("/locale")
    public String locale(@HeaderParam("Accept-Language") Locale locale) {
        return locale.toString();
    }

    @GET
    @Path("/splat/{splat:.*}")
    public String splat(@Context UriInfo uri,
                        @PathParam("splat") String splat) {
        return uri.getPath() + " " + splat;
    }

    @OPTIONS
    @Path("/asyncError")
    public CompletableFuture<String> asyncError() {
        return CompletableFuture.failedFuture(new IOException("error"));
    }

    @OPTIONS
    @Path("/syncError")
    public ByteBuf syncError() throws IOException {
        throw new IOException("error");
    }

    @KILL
    @Path("/kill")
    public String customMethod() {
        return "kill";
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/json")
    public JSONModel getJson(){
        JSONModel json = new JSONModel();
        json.setName("Emerson");
        json.setSuperhero(true);
        return json;
    }
}
