package doctor.cluster;

import doctor.cluster.model.Direction;
import doctor.cluster.model.MessageContainer;
import doctor.cluster.model.MessageType;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;

import java.util.List;

@ChannelHandler.Sharable
public class MessageDecoder extends ByteToMessageDecoder {

    public static final ByteToMessageDecoder INSTANCE = new MessageDecoder();

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) {
        while (in.readableBytes() > 0) {
            MessageContainer msg = new MessageContainer();
            msg.setType(MessageType.from(in.readShort()));
            msg.setDirection(Direction.from(in.readByte()));
            msg.setId(in.readInt());
            msg.setMessageSize(in.readInt());
            byte[] data = new byte[msg.getMessageSize()];
            in.readBytes(data);
            msg.setData(data);
            out.add(msg);
        }
    }
}
