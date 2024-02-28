package vest.doctor.sleipnir.ws;

import vest.doctor.sleipnir.http.HttpData;

import java.nio.ByteBuffer;

public class FrameHeader implements HttpData {

    public enum OpCode {
        CONTINUE((byte) 0x00),
        TEXT((byte) 0x01),
        BINARY((byte) 0x02),
        CLOSE((byte) 0x08),
        PING((byte) 0x09),
        PONG((byte) 0x0A);

        private final byte code;

        OpCode(byte code) {
            this.code = code;
        }

        public static OpCode from(byte b) {
            byte masked = (byte) (0x0F & b);
            for (OpCode value : values()) {
                if (value.code == masked) {
                    return value;
                }
            }
            throw new IllegalStateException("unknown opcode: " + Integer.toString(masked, 16));
        }
    }

    private boolean finished;
    private OpCode opCode;
    private boolean mask;
    private long payloadSize;
    private byte[] maskingKey;

    public boolean isFinished() {
        return finished;
    }

    public FrameHeader setFinished(boolean finished) {
        this.finished = finished;
        return this;
    }

    public OpCode getOpCode() {
        return opCode;
    }

    public FrameHeader setOpCode(OpCode opCode) {
        this.opCode = opCode;
        return this;
    }

    public boolean isMask() {
        return mask;
    }

    public FrameHeader setMask(boolean mask) {
        this.mask = mask;
        return this;
    }

    public long getPayloadSize() {
        return payloadSize;
    }

    public FrameHeader setPayloadSize(long payloadSize) {
        this.payloadSize = payloadSize;
        return this;
    }

    public byte[] getMaskingKey() {
        return maskingKey;
    }

    public void setMaskingKey(byte[] maskingKey) {
        this.maskingKey = maskingKey;
    }

    @Override
    public ByteBuffer serialize() {
        ByteBuffer buf = ByteBuffer.allocate(14);
        byte opCode = getOpCode().code;
        if (finished) {
            opCode |= (byte) 0b10000000;
        }
        buf.put(opCode);
        byte payloadInitial = (byte) (mask ? 0b10000000 : 0);
        if (payloadSize < 126) {
            payloadInitial |= (byte) payloadSize;
            buf.put(payloadInitial);
        } else if (payloadSize < Short.MAX_VALUE) {
            payloadInitial |= (byte) 126;
            buf.put(payloadInitial);
            buf.putShort((short) payloadSize);
        } else {
            payloadInitial |= (byte) 127;
            buf.put(payloadInitial);
            buf.putLong(payloadSize);
        }

        if (mask) {
            buf.put(maskingKey);
        }

        buf.flip();
        return buf;
    }
}
