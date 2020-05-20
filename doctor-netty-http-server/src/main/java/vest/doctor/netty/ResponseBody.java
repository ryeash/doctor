package vest.doctor.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import vest.doctor.netty.impl.DefaultResponseBody;
import vest.doctor.netty.impl.EmptyBody;
import vest.doctor.netty.impl.FileResponseBody;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public interface ResponseBody {

    HttpContent toContent(Request request, Response response);

    static ResponseBody of(String str) {
        return of(str, StandardCharsets.UTF_8);
    }

    static ResponseBody of(String str, Charset charset) {
        if (str == null || str.isEmpty()) {
            return EmptyBody.INSTANCE;
        } else {
            return new DefaultResponseBody(Unpooled.wrappedBuffer(str.getBytes(charset)));
        }
    }

    static ResponseBody of(byte[] content) {
        if (content == null) {
            return EmptyBody.INSTANCE;
        } else {
            return new DefaultResponseBody(Unpooled.wrappedBuffer(content));
        }
    }

    static ResponseBody of(HttpContent content) {
        return (req, res) -> content;
    }

    static ResponseBody of(String rootDirectory, String filePath) {
        return new FileResponseBody(rootDirectory, filePath);
    }

    static ResponseBody of(ByteBuf content) {
        return new DefaultResponseBody(content);
    }

    static ResponseBody empty() {
        return EmptyBody.INSTANCE;
    }
}
