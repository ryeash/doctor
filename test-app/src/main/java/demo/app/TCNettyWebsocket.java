package demo.app;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.inject.Singleton;
import vest.doctor.netty.common.AbstractWebsocket;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Singleton
public class TCNettyWebsocket extends AbstractWebsocket {

    @Override
    public List<String> paths() {
        return List.of("/grumpy");
    }

    @Override
    public void connect(ChannelHandlerContext ctx, String path, Map<String, String> params) {
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
