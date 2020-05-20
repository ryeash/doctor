package vest.doctor.netty;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.HttpHeaders;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

public interface RequestBody {
    CompletableFuture<ByteBuf> completionFuture();

    InputStream inputStream();

    <T> CompletableFuture<T> asyncRead(BiFunction<ByteBuf, Boolean, T> reader);

    Optional<HttpHeaders> trailingHeaders();

    default CompletableFuture<String> asString() {
        StringBuilder sb = new StringBuilder();
        return asyncRead((buf, finished) -> {
            while (buf.readableBytes() > 0) {
                CharSequence charSequence = buf.readCharSequence(buf.readableBytes(), StandardCharsets.UTF_8);
                sb.append(charSequence);
            }
            if (finished) {
                return sb.toString();
            } else {
                return null;
            }
        });
    }

    default CompletableFuture<Boolean> ignored() {
        return asyncRead((buf, finished) -> {
            buf.readerIndex(buf.writerIndex());
            return finished ? true : null;
        });
    }
}
