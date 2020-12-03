package doctor.cluster.model;

public enum MessageType {
    PING(0),
    PONG(1),
    ERROR(2),
    ACK(3), // all requests must have a response, either ACK or NACK (or ERROR)
    NACK(4),
    JOINING(5), // node wishing to join this cluster
    LEAVING(6), // node shutting down and leaving cluster
    DISCOVERY(7), // request to get topology information
    ;

    private final short type;

    MessageType(int type) {
        this.type = (short) type;
    }

    public short type() {
        return type;
    }

    public static MessageType from(short type) {
        for (MessageType value : values()) {
            if (value.type == type) {
                return value;
            }
        }
        throw new IllegalArgumentException("unknown message type: " + type);
    }
}
