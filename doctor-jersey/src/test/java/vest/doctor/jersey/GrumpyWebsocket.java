package vest.doctor.jersey;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.inject.Singleton;

import java.nio.charset.StandardCharsets;

@Singleton
public class GrumpyWebsocket extends AbstractWebsocket {

    @Override
    public String path() {
        return "/grumpy";
    }

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
