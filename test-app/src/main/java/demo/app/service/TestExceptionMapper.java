package demo.app.service;

import jakarta.inject.Singleton;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class TestExceptionMapper implements ExceptionMapper<Throwable> {

    private static final Logger log = LoggerFactory.getLogger(TestExceptionMapper.class);

    @Override
    public Response toResponse(Throwable exception) {
        log.error("exception in request", exception);
        if (exception instanceof WebApplicationException wae) {
            return wae.getResponse();
        }
        return Response.status(500)
                .entity(exception.getMessage())
                .build();
    }
}
