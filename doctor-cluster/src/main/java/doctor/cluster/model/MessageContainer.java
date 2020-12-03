package doctor.cluster.model;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class MessageContainer {

    public static final byte[] EMPTY_BYTES = new byte[0];

    public static MessageContainer errorMessage(String message) {
        MessageContainer msg = new MessageContainer();
        msg.setType(MessageType.ERROR);
        msg.setDirection(Direction.RESPONSE);
        msg.setId(-1);
        msg.setData(message.getBytes(StandardCharsets.UTF_8));
        msg.setMessageSize(message.getBytes().length);
        return msg;
    }

    private MessageType type; // 2
    private Direction direction; // 1
    private int id; // 4
    private int messageSize; // 4
    private byte[] data; // N

    public MessageType getType() {
        return type;
    }

    public void setType(MessageType type) {
        this.type = type;
    }

    public Direction getDirection() {
        return direction;
    }

    public void setDirection(Direction direction) {
        this.direction = direction;
    }

    public int getMessageSize() {
        return messageSize;
    }

    public void setMessageSize(int messageSize) {
        this.messageSize = messageSize;
    }

    public byte[] getData() {
        return data;
    }

    public void setData(byte[] data) {
        this.data = Objects.requireNonNull(data);
        setMessageSize(data.length);
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
