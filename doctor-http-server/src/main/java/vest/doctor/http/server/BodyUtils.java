package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufHolder;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import vest.doctor.pipeline.Pipeline;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * A handle to the asynchronously received request body data.
 */
public final class BodyUtils {
    private BodyUtils() {
    }

    public static Pipeline<ByteBuf> aggregate(Pipeline<HttpContent> data) {
        return data
                .map(ByteBufHolder::content)
                .collect(Collectors.toList())
                .map(list -> Unpooled.compositeBuffer(list.size()).addComponents(true, list));
    }

    public static Pipeline<String> asString(Pipeline<HttpContent> data) {
        return aggregate(data)
                .map(content -> content.toString(StandardCharsets.UTF_8));
    }

    public static Pipeline<InputStream> asInputStream(Pipeline<HttpContent> data) {
        return aggregate(data)
                .map((Function<ByteBuf, InputStream>) ByteBufInputStream::new);
    }

    public static Pipeline<Void> ignored(Pipeline<HttpContent> data) {
        return data.map(c -> {
            c.release();
            return null;
        });
    }
}
