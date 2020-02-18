package demo.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.handler.codec.http.HttpResponseStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.doctor.netty.Attribute;
import vest.doctor.netty.BeanParam;
import vest.doctor.netty.Body;
import vest.doctor.netty.Filter;
import vest.doctor.netty.FilterStage;
import vest.doctor.netty.GET;
import vest.doctor.netty.HeaderParam;
import vest.doctor.netty.POST;
import vest.doctor.netty.Path;
import vest.doctor.netty.PathParam;
import vest.doctor.netty.QueryParam;
import vest.doctor.netty.R;
import vest.doctor.netty.RequestContext;
import vest.doctor.netty.StreamFile;

import javax.inject.Singleton;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Path("netty")
@Singleton
public class TCNettyEndpoint {
    private static final Logger log = LoggerFactory.getLogger(TCNettyEndpoint.class);

    @Filter(FilterStage.BEFORE_MATCH)
    @Path("/*")
    public void beforeMatchFilter(RequestContext ctx) {
        ctx.responseHeader("X-BEFORE-MATCH", true);
    }

    @Filter(FilterStage.BEFORE_ROUTE)
    @Path("/*")
    public void filter(RequestContext ctx, @QueryParam("halt") Optional<Boolean> halt) {
        ctx.responseHeader("X-BEFORE-ROUTE", true);
        ctx.attribute("filter", true);
        if (halt.orElse(false)) {
            ctx.halt(HttpResponseStatus.ACCEPTED, "halted");
        }
    }

    @Filter(FilterStage.AFTER_ROUTE)
    @Path("/*")
    public void afterRouterFilter(RequestContext ctx) {
        ctx.responseHeader("X-AFTER-ROUTE", true);
    }

    @GET
    @Path("/hello")
    public String basic(@QueryParam("q") Optional<String> q,
                        @QueryParam("number") int num,
                        @QueryParam("number") Optional<Integer> optNum,
                        @Attribute("list") List<InputStream> streams,
                        @BeanParam NettyBeanParam<?> beanParam) {
        Assert.assertNull(streams);
        return "ok " + q.orElse(null) + " " + num + " " + optNum.orElse(-1);
    }

    @GET
    @Path("/hello2")
    public byte[] hello2() {
        return "bytes".getBytes();
    }

    @GET
    @POST
    @Path("/goodbye")
    public CompletableFuture<String> goodbye(@Body String s) {
        return CompletableFuture.completedFuture("goodbye");
    }

    @GET
    @POST
    @Path("/usingr")
    public R usingR() {
        return R.ok()
                .header("used-r", true)
                .body("R");
    }

    @POST
    @Path("/pojolist")
    public CompletableFuture<String> pojolist(@Body CompletableFuture<List<Person>> people) throws JsonProcessingException {
        return people.thenApply(l -> {
            try {
                return new ObjectMapper().writeValueAsString(l);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        });
//        return new ObjectMapper().writeValueAsString(person);
    }

    @POST
    @Path("/pojo")
    public CompletableFuture<String> pojo(@Body CompletableFuture<Person> person) {
        log.info("pojo endpoint");
        return person.thenApply(p -> {
            try {
                return new ObjectMapper().writeValueAsString(p);
            } catch (JsonProcessingException e) {
                throw new UncheckedIOException(e);
            }
        });
    }

    @GET
    @Path("/headerparam")
    public R headerParam(@HeaderParam("x-param") String param) {
        return R.ok(param);
    }

    @GET
    @Path("/void")
    public void returnsVoid() {
    }

    @GET
    @Path("/file/*")
    public StreamFile staticFiles(RequestContext ctx, @PathParam("*") String file) {
        log.info("{}", ctx.requestUri());
        return new StreamFile("./", file);
    }
}
