package demo.app;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import vest.doctor.netty.Path;
import vest.doctor.netty.Websocket;

import javax.inject.Singleton;

@Singleton
@Path("/grumpy")
public class TCNettyWebsocket extends Websocket {
    @Override
    public void onMessage(ChannelHandlerContext ctx, WebSocketFrame frame) {
        sendText(ctx, "go away " + getText(frame))
                .thenRun(() -> close(ctx));
    }
}
