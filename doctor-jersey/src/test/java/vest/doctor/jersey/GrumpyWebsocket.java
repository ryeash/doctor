package vest.doctor.jersey;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.inject.Singleton;
import vest.doctor.http.server.impl.AbstractWebsocket;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Singleton
public class GrumpyWebsocket extends AbstractWebsocket {

    @Override
    public List<String> paths() {
        return List.of("/grumpy");
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
