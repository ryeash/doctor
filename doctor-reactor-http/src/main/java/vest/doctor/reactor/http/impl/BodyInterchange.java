package vest.doctor.reactor.http.impl;

import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import vest.doctor.TypeInfo;
import vest.doctor.reactor.http.BodyReader;
import vest.doctor.reactor.http.BodyWriter;
import vest.doctor.reactor.http.HttpResponse;
import vest.doctor.reactor.http.RequestContext;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BodyInterchange implements BodyReader, BodyWriter {
    private final List<BodyReader> readers;
    private final List<BodyWriter> writers;

    public BodyInterchange(List<BodyReader> readers, List<BodyWriter> writers) {
        this.readers = readers;
        this.writers = writers;
    }

    @Override
    public <T> Publisher<T> read(RequestContext requestContext, TypeInfo typeInfo) {
        for (BodyReader reader : readers) {
            Publisher<T> read = reader.read(requestContext, typeInfo);
            if (read != null) {
                return read;
            }
        }
        throw new UnsupportedOperationException("unable to read http to type: " + typeInfo);
    }

    @Override
    public Publisher<HttpResponse> write(RequestContext requestContext, TypeInfo responseTypeInfo, Object responseData) {
        if (responseData instanceof CompletableFuture<?> future) {
            TypeInfo futureType = responseTypeInfo.getParameterTypes().get(0);
            return Mono.fromFuture(future)
                    .flux()
                    .switchMap(object -> write(requestContext, futureType, object));
        } else {
            for (BodyWriter writer : writers) {
                Publisher<HttpResponse> response = writer.write(requestContext, responseTypeInfo, responseData);
                if (response != null) {
                    return response;
                }
            }
            throw new UnsupportedOperationException("unable to write object to http body: " + responseData);
        }
    }
}
