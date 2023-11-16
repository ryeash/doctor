package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.function.Function;

public final class BodyUtils {
    private BodyUtils() {
    }

    public static String toString(RequestContext ctx) {
        return readAndRelease(ctx, buf -> buf.toString(StandardCharsets.UTF_8));
    }

    public static byte[] toBytes(RequestContext ctx) {
        return readAndRelease(ctx, buf -> {
            byte[] bytes = new byte[buf.readableBytes()];
            buf.readBytes(bytes);
            return bytes;
        });
    }

    public static <T> T readInputStream(RequestContext ctx, Function<InputStream, T> function) {
        try (ByteBufInputStream bis = new ByteBufInputStream(ctx.request().body(), true)) {
            return function.apply(bis);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static <T> T readAndRelease(RequestContext ctx, Function<ByteBuf, T> function) {
        ByteBuf buf = ctx.request().body();
        try {
            return function.apply(buf);
        } finally {
            buf.release();
        }
    }
}
