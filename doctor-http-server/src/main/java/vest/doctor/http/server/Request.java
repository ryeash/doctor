package vest.doctor.http.server;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.cookie.Cookie;

import java.net.URI;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

/**
 * Models an HTTP request.
 */
public interface Request {

    /**
     * Unwrap the backing {@link HttpRequest}.
     */
    HttpRequest unwrap();

    /**
     * Get the request HTTP method.
     */
    HttpMethod method();

    /**
     * Get the request {@link URI}.
     */
    URI uri();

    /**
     * Get the full HTTP request path.
     */
    String path();

    /**
     * Get the request headers.
     */
    HttpHeaders headers();

    /**
     * Get a {@link Cookie}.
     *
     * @param name the name of the cooke
     * @return the cookie, or null if it was not sent in the request
     */
    Cookie cookie(String name);

    /**
     * Get a map of all {@link Cookie}s.
     *
     * @return a map of all cookies sent in the request
     */
    Map<String, Cookie> cookies();

    /**
     * Get the {@link RequestBody}.
     */
    RequestBody body();

    /**
     * Receive a multipart body from the client.
     *
     * @return the multipart body uploaded from the client
     */
    MultiPartData multiPartBody();

    /**
     * Get the value of a query parameter.
     *
     * @param name the name of the query parameter
     * @return the value of the query parameter, or null if it is not set
     */
    String queryParam(String name);

    /**
     * Get all values for a query parameter.
     *
     * @param name the name of the query parameter
     * @return a list of all values set for the query parameter
     */
    List<String> queryParams(String name);

    /**
     * Get the request charset.
     *
     * @param defaultCharset the fallback charset to use if the charset wasn't set
     *                       in the request
     * @return the request charset to use when decoding the request body
     */
    Charset requestCharset(Charset defaultCharset);

    /**
     * Get the response charset that the client expects.
     *
     * @param defaultCharset the fallback charset to use if a charset wasn't requested
     *                       by the client
     * @return the response charset to use when encoding the response body
     */
    Charset responseCharset(Charset defaultCharset);
}
