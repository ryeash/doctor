package vest.doctor.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BodyInterchange {
    private final List<BodyReader> readers;
    private final List<BodyWriter> writers;

    public BodyInterchange(ProviderRegistry providerRegistry) {
        DefaultReaderWriter defaultRW = new DefaultReaderWriter();

        JacksonInterchange jacksonInterchange = providerRegistry.getProviderOpt(ObjectMapper.class)
                .map(Provider::get)
                .map(JacksonInterchange::new)
                .orElseGet(() -> new JacksonInterchange(JacksonInterchange.defaultConfig()));

        this.readers = new ArrayList<>();
        readers.add(defaultRW);
        readers.add(jacksonInterchange);
        providerRegistry.getProviders(BodyReader.class)
                .map(Provider::get)
                .forEach(readers::add);
        readers.sort(Prioritized.COMPARATOR);

        this.writers = new ArrayList<>();
        writers.add(defaultRW);
        writers.add(jacksonInterchange);
        providerRegistry.getProviders(BodyWriter.class)
                .map(Provider::get)
                .forEach(writers::add);
        writers.sort(Prioritized.COMPARATOR);
    }

    public <T> CompletableFuture<T> read(Request request, TypeInfo typeInfo) {
        for (BodyReader reader : readers) {
            if (reader.handles(request, typeInfo)) {
                return reader.read(request, typeInfo);
            }
        }
        throw new UnsupportedOperationException("unsupported request body type: " + typeInfo);
    }

    public CompletableFuture<Response> write(Request request, Object data) {
        if (data instanceof CompletableFuture) {
            return ((CompletableFuture<?>) data).thenCompose(d -> write(request, d));
        } else if (data instanceof Response) {
            return CompletableFuture.completedFuture((Response) data);
        } else if (data instanceof ResponseBody) {
            return CompletableFuture.completedFuture(request.createResponse().body((ResponseBody) data));
        } else if (data == null) {
            return CompletableFuture.completedFuture(request.createResponse().body(ResponseBody.empty()));
        } else if (data instanceof R) {
            R r = (R) data;
            return write(request, r.body())
                    .thenApply(response -> {
                        r.headers().forEach(response.headers()::set);
                        response.status(r.status());
                        return response;
                    });
        } else {
            Response response = request.createResponse();
            for (BodyWriter writer : writers) {
                if (writer.handles(response, data)) {
                    return writer.write(response, data)
                            .thenApply(response::body);
                }
            }
            throw new UnsupportedOperationException("unsupported response type: " + response.getClass());
        }
    }
}
