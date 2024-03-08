package vest.doctor.sleipnir.ws;

import vest.doctor.sleipnir.BaseProcessor;
import vest.doctor.sleipnir.BufferUtils;
import vest.doctor.sleipnir.http.HttpData;
import vest.doctor.sleipnir.http.HttpException;
import vest.doctor.sleipnir.http.Status;

import java.nio.ByteBuffer;
import java.util.concurrent.Flow;

public class WebsocketDecoder extends BaseProcessor<ByteBuffer, HttpData> {

    public enum State {
        WS_OPCODE,
        WS_PAYLOAD_INITIAL,
        WS_PAYLOAD_SHORT,
        WS_PAYLOAD_LONG,
        WS_MASK_KEY,
        WS_PAYLOAD_DATA,
        CORRUPT
    }

    private final ByteBuffer lineBuffer;
    private final long maxBodyLength;

    private State state = State.WS_OPCODE;
    private FrameHeader frameHeader;
    private long payloadLeft = 0;
    private int demaskingCounter = 0;

    public WebsocketDecoder(long maxBodyLength) {
        this.lineBuffer = ByteBuffer.allocate(8);
        this.maxBodyLength = maxBodyLength;
    }

    @Override
    public void onSubscribe(Flow.Subscription subscription) {
        // nope
    }

    @Override
    public void onNext(ByteBuffer buf) {
        try {
            while (buf.hasRemaining()) {
                switch (state) {
                    case WS_OPCODE:
                        state = opcode(buf);
                        break;
                    case WS_PAYLOAD_INITIAL:
                        state = payloadInitial(buf);
                        break;
                    case WS_PAYLOAD_SHORT:
                        state = payloadShort(buf);
                        break;
                    case WS_PAYLOAD_LONG:
                        state = payloadLong(buf);
                        break;
                    case WS_MASK_KEY:
                        state = maskKey(buf);
                        break;
                    case WS_PAYLOAD_DATA:
                        state = payloadData(buf);
                        break;
                    case CORRUPT:
                        return;
                    default:
                        throw new HttpException(Status.INTERNAL_SERVER_ERROR, "Unhandled parse state: " + state);
                }
            }
        } catch (Throwable e) {
            state = State.CORRUPT;
            subscriber().onError(e);
            throw e;
        }
    }

    public State opcode(ByteBuffer buf) {
        if (buf.hasRemaining()) {
            frameHeader = new FrameHeader();
            byte first = buf.get();
            frameHeader.setFinished(((byte) 0b10000000 & first) != 0);
            frameHeader.setOpCode(FrameHeader.OpCode.from(first));
            return State.WS_PAYLOAD_INITIAL;
        } else {
            return State.WS_OPCODE;
        }
    }

    public State payloadInitial(ByteBuffer buf) {
        if (buf.hasRemaining()) {
            byte second = buf.get();
            frameHeader.setMask((0b10000000 & second) != 0);

            byte initialPayloadLength = (byte) (0b01111111 & second);
            if (initialPayloadLength < 126) {
                frameHeader.setPayloadSize(initialPayloadLength);
                payloadLeft = frameHeader.getPayloadSize();
                lineBuffer.clear();
                return maskKey(buf);
            } else if (initialPayloadLength == 126) {
                lineBuffer.clear();
                return State.WS_PAYLOAD_SHORT;
            } else {
                lineBuffer.clear();
                return State.WS_PAYLOAD_LONG;
            }
        } else {
            return State.WS_OPCODE;
        }
    }

    public State payloadShort(ByteBuffer buf) {
        while (buf.hasRemaining() && lineBuffer.position() < 2) {
            lineBuffer.put(buf.get());
        }
        if (lineBuffer.position() >= 2) {
            lineBuffer.flip();
            frameHeader.setPayloadSize(lineBuffer.getShort());
            payloadLeft = frameHeader.getPayloadSize();
            lineBuffer.clear();
            return State.WS_MASK_KEY;
        } else {
            return State.WS_PAYLOAD_SHORT;
        }
    }

    public State payloadLong(ByteBuffer buf) {
        while (buf.hasRemaining() && lineBuffer.position() < 8) {
            lineBuffer.put(buf.get());
        }
        if (lineBuffer.position() >= 8) {
            lineBuffer.flip();
            frameHeader.setPayloadSize(lineBuffer.getLong());
            payloadLeft = frameHeader.getPayloadSize();
            lineBuffer.clear();
            return State.WS_MASK_KEY;
        } else {
            return State.WS_PAYLOAD_SHORT;
        }
    }

    public State maskKey(ByteBuffer buf) {
        if (!frameHeader.isMask()) {
            subscriber().onNext(frameHeader);
            return State.WS_PAYLOAD_DATA;
        }
        while (buf.hasRemaining() && lineBuffer.position() < 4) {
            lineBuffer.put(buf.get());
        }
        if (lineBuffer.position() >= 4) {
            lineBuffer.flip();
            byte[] key = new byte[4];
            lineBuffer.get(key);
            frameHeader.setMaskingKey(key);
            subscriber().onNext(frameHeader);
            return State.WS_PAYLOAD_DATA;
        } else {
            return State.WS_MASK_KEY;
        }
    }

    private State payloadData(ByteBuffer buf) {
        if (payloadLeft > maxBodyLength) {
            throw new IllegalStateException("exceeded maximum frame payload length: " + payloadLeft + " max:" + maxBodyLength);
        }

        int toRead = (int) Math.min(payloadLeft, buf.remaining());
        payloadLeft -= toRead;
        ByteBuffer decoded = ByteBuffer.allocate(toRead);
        if (frameHeader.isMask()) {
            for (; decoded.hasRemaining(); demaskingCounter++) {
                decoded.put((byte) (buf.get() ^ frameHeader.getMaskingKey()[demaskingCounter & 0x3]));
            }
        } else {
            BufferUtils.transfer(buf, decoded);
        }
        decoded.flip();
        subscriber().onNext(new Payload(decoded, payloadLeft <= 0));

        if (payloadLeft <= 0) {
            reset();
            return State.WS_OPCODE;
        } else {
            return State.WS_PAYLOAD_DATA;
        }
    }

    private void reset() {
        lineBuffer.clear();
        frameHeader = null;
        payloadLeft = 0;
        demaskingCounter = 0;
    }
}
