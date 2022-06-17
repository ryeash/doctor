package vest.doctor.http.server;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class GrumpyWebsocket extends AbstractWebsocket {

    @Override
    public List<String> paths() {
        return List.of("/grumpy");
    }

    @Override
    protected void onTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        sendText(ctx, "go away " + frame.text())
                .whenComplete((v, error) -> close(ctx));
    }

    @Override
    protected void onBinaryMessage(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        sendText(ctx, "go away " + frame.content().toString(StandardCharsets.UTF_8))
                .whenComplete((v, error) -> close(ctx));
    }
}
