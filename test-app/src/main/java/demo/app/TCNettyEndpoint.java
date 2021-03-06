package demo.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.http.server.rest.ANY;
import vest.doctor.http.server.rest.Attribute;
import vest.doctor.http.server.rest.BeanParam;
import vest.doctor.http.server.rest.Body;
import vest.doctor.http.server.rest.GET;
import vest.doctor.http.server.rest.HeaderParam;
import vest.doctor.http.server.rest.POST;
import vest.doctor.http.server.rest.Path;
import vest.doctor.http.server.rest.PathParam;
import vest.doctor.http.server.rest.Provided;
import vest.doctor.http.server.rest.QueryParam;
import vest.doctor.http.server.rest.R;

import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Singleton
@Path("netty")
public class TCNettyEndpoint {
    private static final Logger log = LoggerFactory.getLogger(TCNettyEndpoint.class);

    @GET
    @Path("/hello")
    public String basic(@QueryParam("q") Optional<String> q,
                        @QueryParam("number") int num,
                        @QueryParam("number") Optional<Integer> optNum,
                        @Attribute("list") List<InputStream> streams,
                        @BeanParam NettyBeanParam<?> beanParam) {
        Assert.assertNull(streams);
        Assert.assertEquals(q.get(), beanParam.getQ().get());
        Assert.assertEquals(num, beanParam.getNum());
        Assert.assertEquals(beanParam.getNumberViaMethod(), num);
        return "ok " + q.orElse(null) + " " + num + " " + optNum.orElse(-1);
    }

    @GET
    @Path("/hello2")
    public byte[] hello2() {
        Locale.forLanguageTag("");
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
    public R staticFiles(Request ctx, @PathParam("*") String file) {
        log.info("{}", ctx.uri());
        return R.file(ctx, "./", file);
    }

    @GET
    @Path("/paramtest/{normal}/{custom:\\d+}")
    public String paramtest(@PathParam("normal") String normal,
                            @PathParam("custom") int custom,
                            @Provided @Named("pourOver") CoffeeMaker pourOver) {
        return normal + " " + custom + " " + pourOver.brew();
    }

    @GET
    @Path("/attribute")
    public String attribute(@Attribute("attr") String attribute) {
        return attribute;
    }

    @ANY
    @Path("/anything")
    public String any(Request request) {
        return request.method().toString();
    }

    @GET
    @Path("/locale")
    public String locale(@HeaderParam("Accept-Language") Locale locale) {
        return locale.toString();
    }

    @GET
    @Path("/fullresponse")
    public CompletableFuture<Response> responder(Request request) {
        return request.body()
                .completionFuture()
                .thenApplyAsync(v -> request.createResponse().body(ResponseBody.of("response")));
    }
}
