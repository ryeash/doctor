package vest.doctor.jersey;

import jakarta.inject.Singleton;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.Response;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Singleton
public class TestFilter implements ContainerRequestFilter, ContainerResponseFilter {
    @Override
    public void filter(ContainerRequestContext containerRequestContext) {
        containerRequestContext.setProperty("start", System.nanoTime());
        if (Objects.equals(containerRequestContext.getUriInfo().getQueryParameters().getFirst("halt"), "true")) {
            containerRequestContext.abortWith(Response.status(503).build());
        }
    }

    @Override
    public void filter(ContainerRequestContext containerRequestContext, ContainerResponseContext containerResponseContext) {
        containerResponseContext.getHeaders().putSingle("X-After", "true");
        long nanoStart = (long) containerRequestContext.getProperty("start");
        containerResponseContext.getHeaders().putSingle("TIME", TimeUnit.MICROSECONDS.convert((System.nanoTime() - nanoStart), TimeUnit.NANOSECONDS));
    }
}
