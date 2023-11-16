package vest.doctor.http.server;

import vest.doctor.TypeInfo;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BodyInterchange {
    private final List<BodyReader> readers;
    private final List<BodyWriter> writers;

    public BodyInterchange(List<BodyReader> readers, List<BodyWriter> writers) {
        this.readers = readers;
        this.writers = writers;
    }

    public <T> T read(RequestContext requestContext, TypeInfo typeInfo) {
        for (BodyReader reader : readers) {
            T read = reader.read(requestContext, typeInfo);
            if (read != null) {
                return read;
            }
        }
        throw new UnsupportedOperationException("unable to read http body to type: " + typeInfo);
    }

    public CompletableFuture<RequestContext> write(RequestContext requestContext, TypeInfo responseTypeInfo, Object responseData) {
        for (BodyWriter writer : writers) {
            CompletableFuture<RequestContext> response = writer.write(requestContext, responseTypeInfo, responseData);
            if (response != null) {
                return response;
            }
        }
        throw new UnsupportedOperationException("unable to write object to http body: " + responseData);
    }
}
