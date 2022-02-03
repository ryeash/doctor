package vest.doctor.reactor.http;

import org.reactivestreams.Publisher;

import java.util.Set;

/**
 * An HTTP request context holding the {@link HttpRequest} and {@link HttpResponse} and context attributes.
 */
public interface RequestContext extends Publisher<HttpResponse> {

    /**
     * Get the {@link HttpRequest}.
     */
    HttpRequest request();

    /**
     * Get the {@link HttpResponse}.
     */
    HttpResponse response();

    /**
     * Set an attribute on this request context. Attributes are preserved for the lifetime of the
     * request.
     *
     * @param name  the name of the attribute
     * @param value the value of the attribute, if null the attribute is removed from the context
     */
    void attribute(String name, Object value);

    /**
     * Get an attribute value.
     *
     * @param name the name of the attribute
     * @return the attribute value, or null if not present
     */
    <T> T attribute(String name);

    /**
     * Get an attribute value, returning a default if not present
     *
     * @param name   the attribute name
     * @param orElse the fallback value if the attribute is not present
     * @return the attribute value
     */
    <T> T attributeOrElse(String name, T orElse);

    /**
     * Get the names of all attributes attached to this context.
     */
    Set<String> attributeNames();
}
