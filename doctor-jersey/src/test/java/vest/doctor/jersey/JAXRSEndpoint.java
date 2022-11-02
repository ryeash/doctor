package vest.doctor.jersey;

import io.netty.handler.codec.http.HttpHeaderNames;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.http.HttpServletRequest;
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
import jakarta.ws.rs.core.Application;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Request;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.core.UriInfo;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.testng.Assert;
import vest.doctor.ProviderRegistry;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
@JerseyFeature
@Path("/jaxrs")
public class JAXRSEndpoint {

    @GET
    @Path("/get")
    public String get(@Context HttpServletRequest request,
                      @Provided ProviderRegistry providerRegistry,
                      @Attribute("start") Long start) {
        Assert.assertNotNull(request);
        Assert.assertTrue(start > 0);
        Assert.assertNotNull(providerRegistry);
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
    public void async(@Suspended AsyncResponse ar,
                      @Provided @Named("default") ExecutorService executorService) {
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
    @Path("/context")
    public String contextualThings(@Context SecurityContext securityContext,
                                   @Context Request request,
                                   @Context Application application,
                                   @Context ServletRequest servletRequest,
                                   @Context HttpServletRequest httpServletRequest,
                                   @Context HttpHeaders httpHeaders,
                                   @Context UriInfo uriInfo,
                                   @Context ExtendedUriInfo extendedUriInfo) {
        Assert.assertNotNull(securityContext);
        Assert.assertNotNull(request);
        Assert.assertNotNull(application);
        Assert.assertNotNull(servletRequest);
        Assert.assertNotNull(httpServletRequest);
        Assert.assertNotNull(httpHeaders);
        Assert.assertNotNull(uriInfo);
        Assert.assertNotNull(extendedUriInfo);
        return "ok";
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
        Assert.assertNotNull(beanParam.getRequest());
        Assert.assertTrue(TimeUnit.SECONDS.convert(System.nanoTime() - beanParam.getStart(), TimeUnit.NANOSECONDS) < 5);
        Assert.assertNotNull(beanParam.getTestFilter());
        return pathParam + " " + queryParam + " " + header + " " + cookie;
    }
}
