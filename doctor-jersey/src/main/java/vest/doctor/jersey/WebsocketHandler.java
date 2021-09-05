package vest.doctor.jersey;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class WebsocketHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    private static final Logger log = LoggerFactory.getLogger(WebsocketHandler.class);
    private final Websocket websocket;

    public WebsocketHandler(Websocket websocket) {
        super();
        this.websocket = websocket;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame frame) {
        if (websocket == null) {
            ctx.close();
            return;
        }
        try {
            websocket.onMessage(ctx, frame);
        } catch (Throwable t) {
            log.error("websocket handler caused an error; closing channel", t);
            websocket.close(ctx, 1002, t.getMessage());
        }
    }
}