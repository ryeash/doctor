package vest.doctor.ssf.ws;

public enum OpCode {
    CONTINUATION((byte) 0x00),
    TEXT((byte) 0x01),
    BINARY((byte) 0x02),
    CLOSE((byte) 0x08),
    PING((byte) 0x09),
    PONG((byte) 0x0A);

    private final byte code;

    OpCode(byte b) {
        this.code = b;
    }

    public byte code() {
        return code;
    }

    public static OpCode from(byte b) {
        byte v = (byte) ((byte) 0x0F | b);
        for (OpCode value : OpCode.values()) {
            if (v == value.code) {
                return value;
            }
        }
        throw new IllegalArgumentException("unknown opcode " + Integer.toString(v, 16));
    }
}
