package vest.doctor.http.server;

import io.netty.buffer.ByteBuf;

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

    public static <T> T readAndRelease(RequestContext ctx, Function<ByteBuf, T> function) {
        ByteBuf buf = ctx.request().body();
        try {
            return function.apply(buf);
        } finally {
            ctx.request().body().release();
        }
    }
}
