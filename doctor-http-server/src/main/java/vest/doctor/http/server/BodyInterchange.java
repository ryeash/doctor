package vest.doctor.http.server;

import vest.doctor.TypeInfo;
import vest.doctor.reactive.Rx;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.function.Function;

public final class BodyInterchange {
    private final List<BodyReader> readers;
    private final List<BodyWriter> writers;

    public BodyInterchange(List<BodyReader> readers, List<BodyWriter> writers) {
        this.readers = readers;
        this.writers = writers;
    }

    public <T> Flow.Publisher<T> read(RequestContext requestContext, TypeInfo typeInfo) {
        for (BodyReader reader : readers) {
            Flow.Publisher<T> read = reader.read(requestContext, typeInfo);
            if (read != null) {
                return read;
            }
        }
        throw new UnsupportedOperationException("unable to read http body to type: " + typeInfo);
    }

    public Flow.Publisher<Response> write(RequestContext requestContext, TypeInfo responseTypeInfo, Object responseData) {
        if (responseData instanceof CompletableFuture<?> future) {
            TypeInfo futureType = responseTypeInfo.getParameterTypes().get(0);
            return Rx.one(future)
                    .mapFuture(Function.identity())
                    .mapPublisher(object -> write(requestContext, futureType, object));
        } else {
            for (BodyWriter writer : writers) {
                Flow.Publisher<Response> response = writer.write(requestContext, responseTypeInfo, responseData);
                if (response != null) {
                    return response;
                }
            }
            throw new UnsupportedOperationException("unable to write object to http body: " + responseData);
        }
    }
}
