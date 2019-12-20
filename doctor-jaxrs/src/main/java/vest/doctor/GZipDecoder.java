package vest.doctor;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.ext.Provider;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

@Provider
@Priority(Priorities.ENTITY_CODER)
public class GZipDecoder implements ReaderInterceptor {

    GZipDecoder() {
    }

    @Override
    public Object aroundReadFrom(ReaderInterceptorContext context) throws IOException {
        String contentEncoding = context.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING);
        if (contentEncoding != null && contentEncoding.contains("gzip")) {
            context.setInputStream(new GZIPInputStream(context.getInputStream()));
        }
        return context.proceed();
    }
}