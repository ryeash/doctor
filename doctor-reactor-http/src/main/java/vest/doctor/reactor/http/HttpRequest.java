package vest.doctor.reactor.http;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.cookie.Cookie;
import reactor.netty.ByteBufFlux;
import reactor.netty.http.server.HttpServerRequest;

import java.net.URI;
import java.util.Map;

/**
 * An HTTP request object.
 */
public interface HttpRequest {

    /**
     * Get the underlying {@link HttpServerRequest}.
     */
    HttpServerRequest unwrap();

    /**
     * The HTTP request method.
     */
    HttpMethod method();

    /**
     * The HTTP request {@link URI}.
     */
    URI uri();

    /**
     * The URI path.
     */
    String path();

    /**
     * The HTTP request {@link HttpHeaders headers}.
     */
    HttpHeaders headers();

    /**
     * Get the value for a header.
     *
     * @param name the name of the header
     * @return the header value, or null if not present
     * @see io.netty.handler.codec.http.HttpHeaderNames
     */
    String header(CharSequence name);

    /**
     * Get a path parameter from the URI path matching.
     * Example: for path template like /root/ws/{type}
     * Matching requests can use <code>request.pathParam("type")</code>
     * to get the matched value
     *
     * @param name the name of the path parameter
     * @return the value of the parameter or null if not present
     */
    String pathParam(String name);

    /**
     * Get a query parameter from the request URI.
     *
     * @param name the name of the query parameter
     * @return the value of the parameter or null if not present
     */
    String queryParam(String name);

    /**
     * Get a {@link Cookie} from the "Cookie" headers.
     *
     * @param name the name of the cookie
     * @return the cookie object or null if not present
     */
    Cookie cookie(String name);

    /**
     * Get a map of all {@link Cookie cookies} sent in the "Cookie" headers.
     *
     * @return a map of all cookies
     */
    Map<CharSequence, Cookie> cookies();

    /**
     * Get the request body data flow.
     */
    ByteBufFlux body();
}
