package vest.doctor.netty;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.buffer.Unpooled;
import vest.doctor.Prioritized;
import vest.doctor.ProviderRegistry;

import javax.inject.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public final class BodyInterchange {
    private final List<BodyReader> readers;
    private final List<BodyWriter> writers;

    private final boolean tracingEnabled;

    public BodyInterchange(ProviderRegistry providerRegistry) {
        this.tracingEnabled = new NettyConfiguration(providerRegistry.configuration()).debugRequestRouting();

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

    public <T> T read(RequestContext ctx, TypeInfo typeInfo) {
        for (BodyReader reader : readers) {
            if (reader.handles(ctx, typeInfo)) {
                Utils.addTraceInfo(tracingEnabled, ctx, "BODY READER {}", reader);
                return reader.read(ctx, typeInfo);
            }
        }
        throw new UnsupportedOperationException("unsupported request body type: " + typeInfo);
    }

    public void write(RequestContext ctx, Object response) {
        if (response == null) {
            ctx.complete();
        } else if (response instanceof CompletableFuture) {
            write(ctx, (CompletableFuture<?>) response);
        } else if (response instanceof R) {
            write(ctx, (R) response);
        } else {
            for (BodyWriter writer : writers) {
                if (writer.handles(ctx, response)) {
                    Utils.addTraceInfo(tracingEnabled, ctx, "BODY WRITER {}", writer);
                    writer.write(ctx, response);
                    ctx.complete();
                    return;
                }
            }
            throw new UnsupportedOperationException("unsupported response type: " + response.getClass());
        }
    }

    public void write(RequestContext ctx, CompletableFuture<?> response) {
        if (response == null) {
            ctx.responseBody(Unpooled.EMPTY_BUFFER);
            return;
        }
        response.whenComplete((value, error) -> {
            if (error != null) {
                ctx.complete(error);
            } else {
                write(ctx, value);
            }
        });
    }

    public void write(RequestContext ctx, R response) {
        if (response == null) {
            ctx.responseBody(Unpooled.EMPTY_BUFFER);
            return;
        }
        ctx.responseStatus(response.status());
        response.headers().forEach(ctx.responseHeaders()::set);
        write(ctx, response.body());
    }
}
