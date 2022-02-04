package vest.doctor.reactor.http;

import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.cookie.Cookie;
import io.netty.handler.codec.http.cookie.DefaultCookie;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.http.server.HttpServerResponse;

import java.util.Map;
import java.util.Set;

/**
 * An HTTP response object.
 */
public interface HttpResponse {

    /**
     * Get the underlying {@link HttpServerResponse} object.
     */
    HttpServerResponse unwrap();

    /**
     * Set the response status.
     *
     * @see HttpResponseStatus
     */
    default HttpResponse status(int status) {
        return status(HttpResponseStatus.valueOf(status));
    }

    /**
     * Set the response status.
     *
     * @see HttpResponseStatus
     */
    HttpResponse status(HttpResponseStatus status);

    /**
     * Set a response header.
     *
     * @param name  the name of the header
     * @param value the value of the header
     * @return this object
     * @see io.netty.handler.codec.http.HttpHeaderNames
     * @see io.netty.handler.codec.http.HttpHeaderValues
     */
    HttpResponse header(CharSequence name, CharSequence value);

    /**
     * Set a response header with multiple values.
     *
     * @param name  the name of the header
     * @param value the values for the header
     * @return this object
     * @see io.netty.handler.codec.http.HttpHeaderNames
     * @see io.netty.handler.codec.http.HttpHeaderValues
     */
    HttpResponse header(CharSequence name, Iterable<? extends CharSequence> value);

    /**
     * Get the response {@link HttpHeaders header} map.
     */
    HttpHeaders headers();

    /**
     * Set the {@link ResponseBody response body}.
     */
    HttpResponse body(ResponseBody body);

    /**
     * Set a cookie (via "Set-Cookie" header) in the response headers.
     *
     * @param cookie the cookie value
     * @return this object
     */
    HttpResponse cookie(Cookie cookie);

    /**
     * Set a cookie (via "Set-Cookie" header) in the response headers.
     *
     * @param name  the cookie name
     * @param value the cookie value
     * @return this object
     */
    default HttpResponse cookie(String name, String value) {
        return cookie(new DefaultCookie(name, value));
    }

    /**
     * Get all the response cookies.
     */
    Map<CharSequence, Set<Cookie>> cookies();

    /**
     * Complete this response and send it to the client.
     */
    Publisher<Void> send();

    /**
     * Get a publisher for this response.
     */
    default Publisher<HttpResponse> publish() {
        return Mono.just(this);
    }
}
