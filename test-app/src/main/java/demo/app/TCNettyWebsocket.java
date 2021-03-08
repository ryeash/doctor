package demo.app;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.inject.Singleton;
import vest.doctor.http.server.impl.AbstractWebsocket;
import vest.doctor.http.server.rest.Path;

import java.nio.charset.StandardCharsets;

@Singleton
@Path("/grumpy")
public class TCNettyWebsocket extends AbstractWebsocket {

    @Override
    public void connect(ChannelHandlerContext ctx, String path) {
        System.out.println("NEW WEBSOCKET CONNECTION ACCEPTED");
    }

    @Override
    protected void onTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        sendText(ctx, "go away " + frame.text())
                .thenRun(() -> close(ctx));
    }

    @Override
    protected void onBinaryMessage(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        sendText(ctx, "go away " + new String(toByteArray(frame.content()), StandardCharsets.UTF_8))
                .thenRun(() -> close(ctx));
    }
}
