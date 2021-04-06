package vest.doctor.http.server.rest;

import jakarta.inject.Provider;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;
import vest.doctor.TypeInfo;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BodyInterchange {
    private final List<BodyReader> readers;
    private final List<BodyWriter> writers;

    public BodyInterchange(ProviderRegistry providerRegistry) {
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

    public <T> CompletableFuture<T> read(Request request, TypeInfo typeInfo) {
        for (BodyReader reader : readers) {
            if (reader.canRead(request, typeInfo)) {
                return reader.read(request, typeInfo);
            }
        }
        throw new UnsupportedOperationException("unsupported request body type: " + typeInfo);
    }

    public CompletableFuture<Response> write(Request request, Object data) {
        if (data == null) {
            return request.createResponse().body(ResponseBody.empty()).wrapFuture();
        } else if (data instanceof CompletableFuture) {
            return ((CompletableFuture<?>) data).thenCompose(d -> write(request, d));
        } else if (data instanceof Response) {
            return ((Response) data).wrapFuture();
        } else if (data instanceof ResponseBody) {
            return request.createResponse().body((ResponseBody) data).wrapFuture();
        } else if (data instanceof File) {
            return request.createResponse().body(ResponseBody.sendFile((File) data)).wrapFuture();
        } else if (data instanceof R) {
            R r = (R) data;
            return write(request, r.body()).thenApply(r::applyTo);
        } else {
            Response response = request.createResponse();
            for (BodyWriter writer : writers) {
                if (writer.canWrite(response, data)) {
                    return writer.write(response, data)
                            .thenApply(response::body);
                }
            }
            throw new UnsupportedOperationException("unsupported response type: " + response.getClass());
        }
    }
}
