package vest.doctor.http.server.rest;

import jakarta.inject.Provider;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.flow.Flo;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

/**
 * Combines the {@link BodyReader BodyReaders} and {@link BodyWriter BodyWriters} that are provided
 * by a {@link ProviderRegistry} to provide an aggregate read/write mechanism for HTTP body data.
 */
public final class BodyInterchange {
    private final List<BodyReader> readers;
    private final List<BodyWriter> writers;

    BodyInterchange(ProviderRegistry providerRegistry) {
        DefaultReaderWriter defaultRW = new DefaultReaderWriter();

        this.readers = new ArrayList<>();
        readers.add(defaultRW);
        providerRegistry.getProviders(BodyReader.class)
                .map(Provider::get)
                .forEach(readers::add);
        readers.sort(Prioritized.COMPARATOR);

        this.writers = new ArrayList<>();
        writers.add(defaultRW);
        providerRegistry.getProviders(BodyWriter.class)
                .map(Provider::get)
                .forEach(writers::add);
        writers.sort(Prioritized.COMPARATOR);
    }

    /**
     * Read the body data from the request.
     *
     * @param request  the request
     * @param typeInfo the type info for the target parameter
     * @return the asynchronous result of reading the body data into the desired type
     */
    public <T> Flo<?, T> read(Request request, TypeInfo typeInfo) {
        for (BodyReader reader : readers) {
            if (reader.canRead(request, typeInfo)) {
                return reader.read(request, typeInfo);
            }
        }
        throw new UnsupportedOperationException("unsupported request body type: " + typeInfo);
    }

    /**
     * Create a {@link Response} for the given request and body data.
     *
     * @param request the request
     * @param data    the body response
     * @return the asynchronous response to the request
     */
    public Flo<?, Response> write(Request request, Object data) {
        if (data == null) {
            return Flo.of(request.createResponse().body(ResponseBody.empty()));
        } else if (data instanceof Flo<?, ?> flo) {
            return flo.chain((o) -> write(request, o));
        } else if (data instanceof CompletableFuture<?> future) {
            return Flo.of(future)
                    .mapFuture(Function.identity())
                    .chain(o -> write(request, o))
                    .cast(Response.class);
        } else if (data instanceof Response response) {
            return Flo.of(response);
        } else if (data instanceof ResponseBody body) {
            return Flo.of(request.createResponse().body(body));
        } else if (data instanceof File file) {
            return Flo.of(request.createResponse().body(ResponseBody.sendFile(file)));
        } else if (data instanceof R r) {
            return write(request, r.body())
                    .map(r::applyTo);
        } else {
            Response response = request.createResponse();
            for (BodyWriter writer : writers) {
                if (writer.canWrite(response, data)) {
                    return Flo.of(response.body(writer.write(response, data)));
                }
            }
            throw new UnsupportedOperationException("unsupported response type: " + response.getClass());
        }
    }
}
