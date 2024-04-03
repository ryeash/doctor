package vest.doctor.jaxrs;

import io.netty.handler.codec.http.HttpHeaderNames;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.container.AsyncResponse;
import jakarta.ws.rs.container.Suspended;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.testng.Assert;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
@Path("/jaxrs")
public class JAXRSEndpoint {

    private final ExecutorService executorService;

    @Inject
    public JAXRSEndpoint(ExecutorService executorService) {
        this.executorService = executorService;
    }

    @GET
    @Path("/basic")
    public String get() {
        return "ok";
    }

    @POST
    public Response throughput(byte[] bytes) {
        return Response.ok()
                .entity(bytes)
                .header(HttpHeaderNames.CONTENT_TYPE.toString(), MediaType.APPLICATION_OCTET_STREAM)
                .build();
    }

    @POST
    @Path("/pojo")
    public User user(User user) {
        return user;
    }

    @GET
    @Path("/async")
    public void async(@Suspended AsyncResponse ar) {
        Assert.assertNotNull(executorService);
        CompletableFuture.supplyAsync(() -> {
                    try {
                        TimeUnit.MILLISECONDS.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    return "async";
                }, executorService)
                .thenApply(Response.ok()::entity)
                .thenApply(Response.ResponseBuilder::build)
                .thenAccept(ar::resume);
    }

    @GET
    @Path("/params/{pathParam}")
    public String params(@PathParam("pathParam") String pathParam,
                         @QueryParam("queryParam") String queryParam,
                         @HeaderParam("X-Header") String header,
                         @CookieParam("_cookie") String cookie,
                         @BeanParam TestBeanParam beanParam) {
        Assert.assertNotNull(beanParam);
        Assert.assertEquals(beanParam.getPathParam(), pathParam);
        Assert.assertEquals(beanParam.getQueryParam(), queryParam);
        Assert.assertEquals(beanParam.getHeader(), header);
        Assert.assertEquals(beanParam.getCookie(), cookie);
        return pathParam + " " + queryParam + " " + header + " " + cookie;
    }
}
