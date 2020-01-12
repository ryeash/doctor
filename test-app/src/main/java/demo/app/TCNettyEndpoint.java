package demo.app;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import vest.doctor.netty.Attribute;
import vest.doctor.netty.Body;
import vest.doctor.netty.Filter;
import vest.doctor.netty.GET;
import vest.doctor.netty.HeaderParam;
import vest.doctor.netty.POST;
import vest.doctor.netty.Path;
import vest.doctor.netty.QueryParam;
import vest.doctor.netty.R;
import vest.doctor.netty.RequestContext;

import javax.inject.Singleton;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Path("netty")
@Singleton
public class TCNettyEndpoint {
    private static final Logger log = LoggerFactory.getLogger(TCNettyEndpoint.class);

    @Filter
    @Path("/*")
    public void filter(RequestContext ctx) {
        ctx.attribute("filter", true);
    }

    @GET
    @Path("/hello")
    public String basic(@QueryParam("q") Optional<String> q,
                        @QueryParam("number") int num,
                        @QueryParam("number") Optional<Integer> optNum,
                        @Attribute("list") List<InputStream> streams) {
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
    @Path("/pojo")
    public String pojo(@Body List<Person> person) throws JsonProcessingException {
        return new ObjectMapper().writeValueAsString(person);
    }

    @GET
    @Path("/headerparam")
    public R headerParam(@HeaderParam("x-param") String param) {
        return R.ok(param);
    }
}
