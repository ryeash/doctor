package vest.doctor.reactor.http.impl;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AttributeKey;
import io.netty.util.ReferenceCountUtil;
import vest.doctor.reactor.http.HttpRequest;

import java.util.concurrent.atomic.AtomicLong;

// TODO: inexplicably, there's no way to relay an error up to the Reactor flux unless you're doing a blocking operation (e.g. blockLast).
// i.e. event driven reads will not ever fail for content too large, but blocking reads will
// oh well, half a solution is better than no solution?
@Sharable
class HttpMaxContentEnforcer extends ChannelInboundHandlerAdapter {

    private static final AttributeKey<AtomicLong> BODY_BYTES_READ = AttributeKey.newInstance("vest.doctor.reactor.http.impl.bodyBytesRead");

    private final long maxContentLength;

    HttpMaxContentEnforcer(long maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof HttpRequest) {
            ctx.channel().attr(BODY_BYTES_READ).set(new AtomicLong(0L));
        }
        if (msg instanceof HttpContent content) {
            if (ctx.channel().attr(BODY_BYTES_READ).get() == null) {
                ctx.channel().attr(BODY_BYTES_READ).set(new AtomicLong(0));
            }
            long read = ctx.channel().attr(BODY_BYTES_READ).get().addAndGet(content.content().readableBytes());
            if (read > maxContentLength) {
                ReferenceCountUtil.release(content);
                throw new HttpException(HttpResponseStatus.REQUEST_ENTITY_TOO_LARGE);
            }
        }
        super.channelRead(ctx, msg);
    }


}
