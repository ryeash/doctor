package vest.doctor.netty;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;

import java.util.List;
import java.util.Map;

/**
 * Interface defining the contract for a websocket endpoint.
 */
public interface Websocket {

    /**
     * List of paths to bind this websocket to.
     */
    List<String> paths();

    /**
     * Called when the HttpServer accepts a new websocket connection. Defaults to a no-op.
     *
     * @param ctx    The client connection context
     * @param path   The path that the websocket client connected to
     * @param params The path params extracted from the request path
     */
    void connect(ChannelHandlerContext ctx, String path, Map<String, String> params);

    /**
     * Called when a message is received from the client. If the implementation throws
     * an exception, the channel will be closed automatically.
     *
     * @param ctx   The connection context that received the message.
     * @param frame The websocket message frame
     */
    void onMessage(ChannelHandlerContext ctx, WebSocketFrame frame) throws Exception;

    /**
     * Send a close frame with the given close status and message
     * and perform the close handshake with the remote.
     *
     * @param ctx     The websocket channel context to close
     * @param status  The close status to send
     * @param message The message to send
     */
    void close(ChannelHandlerContext ctx, int status, String message);

    /**
     * Handshakes the websocket connection.
     *
     * @param ctx     The context to handshake
     * @param request The http request that initiated the websocket handshake
     * @param path    The request path
     */
    void handshake(ChannelHandlerContext ctx, HttpRequest request, String path, Map<String, String> params);
}
