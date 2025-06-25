package demo.app.service;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.inject.Singleton;
import jakarta.ws.rs.Path;
import vest.doctor.websocket.AbstractWebsocket;

@Path("/grumpy")
@Singleton
public class GrumpyWebsocket extends AbstractWebsocket {

    @Override
    protected void onTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        sendText(ctx, "go away " + frame.text())
                .whenComplete((v, ex) -> close(ctx));
    }

    @Override
    protected void onBinaryMessage(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        sendBinary(ctx, frame.content());
    }
}
