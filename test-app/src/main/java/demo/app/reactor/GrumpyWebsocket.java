package demo.app.reactor;

import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import reactor.core.scheduler.Scheduler;
import vest.doctor.reactor.http.WebsocketSession;
import vest.doctor.reactor.http.impl.AbstractWebsocket;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Singleton
public class GrumpyWebsocket extends AbstractWebsocket {

    @Inject
    protected GrumpyWebsocket(@Named("websocketScheduler") Scheduler scheduler) {
        super(scheduler);
    }

    @Override
    public List<String> path() {
        return List.of("/grumpy");
    }

    @Override
    protected void onTextMessage(WebsocketSession session, TextWebSocketFrame frame) {
        String text = frame.text();
        scheduler.schedule(() -> {
            session.sendText("go away " + text);
            session.sendClose(1000, "all done");
        }, 500, TimeUnit.MILLISECONDS);
    }

    @Override
    protected void onBinaryMessage(WebsocketSession session, BinaryWebSocketFrame frame) {
        throw new UnsupportedOperationException();
    }
}
