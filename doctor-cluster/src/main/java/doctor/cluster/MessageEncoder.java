package doctor.cluster;

import doctor.cluster.model.MessageContainer;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;

import java.io.IOException;

@ChannelHandler.Sharable
public class MessageEncoder extends MessageToByteEncoder<MessageContainer> {

    public static final MessageToByteEncoder<MessageContainer> INSTANCE = new MessageEncoder();

    @Override
    protected void encode(ChannelHandlerContext ctx, MessageContainer msg, ByteBuf out) throws IOException {
        out.writeShort(msg.getType().type());
        out.writeByte(0xFF & msg.getDirection().id());
        out.writeInt(msg.getId());
        out.writeInt(msg.getMessageSize());
        out.writeBytes(msg.getData());
    }
}
