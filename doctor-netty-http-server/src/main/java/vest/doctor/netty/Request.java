package vest.doctor.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.cookie.Cookie;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public interface Request {
    ChannelHandlerContext channelContext();

    ExecutorService pool();

    HttpRequest unwrap();

    HttpMethod method();

    URI uri();

    String path();

    void rewritePath(String path);

    HttpHeaders headers();

    Cookie cookie(String name);

    Map<String, Cookie> cookies();

    RequestBody body();

    String queryParam(String name);

    List<String> queryParams(String name);

    void attribute(String name, Object attribute);

    <T> T attribute(String name);

    <T> T attribute(Class<T> type);

    Charset requestCharset(Charset defaultCharset);

    Charset responseCharset(Charset defaultCharset);

    Response createResponse();
}
