package demo.app.reactor;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.inject.Singleton;
import vest.doctor.http.server.impl.AbstractWebsocket;

import java.util.List;

@Singleton
public class GrumpyWebsocket extends AbstractWebsocket {

    @Override
    public List<String> paths() {
        return List.of("/grumpy");
    }

    @Override
    protected void onTextMessage(ChannelHandlerContext ctx, TextWebSocketFrame frame) {
        String text = frame.text();
        sendText(ctx, "go away " + text).whenComplete((v, error) -> {
            close(ctx, 1000, "all done");
        });
    }

    @Override
    protected void onBinaryMessage(ChannelHandlerContext ctx, BinaryWebSocketFrame frame) {
        throw new UnsupportedOperationException();
    }
}
