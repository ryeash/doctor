package vest.doctor.http.server;

import io.netty.channel.ChannelHandlerContext;
import vest.doctor.http.server.impl.Router;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;

public interface RequestContext {

    /**
     * Get the {@link Request}.
     */
    Request request();

    /**
     * Get the {@link Response}.
     */
    Response response();

    /**
     * Get the underlying channel context (socket) that the request was received on.
     */
    ChannelHandlerContext channelContext();

    /**
     * Get the event {@link ExecutorService} that can be used to run background tasks.
     */
    ExecutorService pool();

    /**
     * A "mapping" function that ignores the input and just returns the response object from this
     * request. Useful when doing things like:
     * <pre>
     * ctx -> Rx.from(ctx.request().body().ignored())
     *          .map(ctx::response)
     *          .observe(response -> response.status(HttpResponseStatus.NOT_FOUND).body(ResponseBody.empty()));
     * </pre>
     * @param ignored ignored always
     * @return the {@link Response}
     */
    @SuppressWarnings("unused")
    default Response response(Object ignored) {
        return response();
    }

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

    /**
     * If this request is flowing through the {@link Router} get the path parameter for the given name.
     *
     * @param name the path parameter name
     * @return the value of the path parameter, or null if not found
     */
    default String pathParam(String name) {
        Map<String, String> pathParams = attribute(Router.PATH_PARAMS);
        return pathParams != null ? pathParams.get(name) : null;
    }
}
