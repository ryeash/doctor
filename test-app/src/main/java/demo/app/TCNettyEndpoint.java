package demo.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.netty.GET;
import vest.doctor.netty.Path;
import vest.doctor.netty.RequestContext;

import javax.inject.Singleton;

@Path("netty")
@Singleton
public class TCNettyEndpoint {
    private static final Logger log = LoggerFactory.getLogger(TCNettyEndpoint.class);

    @GET
    @Path("/hello")
    public void basic(RequestContext requestContext) {
        log.info("{}", requestContext);
        requestContext.response(200, "ok");
        requestContext.complete();
    }
}
