package vest.doctor.sleipnir.ws;

import vest.doctor.sleipnir.BufferUtils;
import vest.doctor.sleipnir.http.HttpData;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class Frame implements HttpData {

    public static Frame binary(ByteBuffer data) {
        FrameHeader header = new FrameHeader();
        header.setFinished(true);
        header.setMask(false);
        header.setOpCode(FrameHeader.OpCode.BINARY);
        return new Frame(header, data);
    }

    public static Frame text(String text) {
        FrameHeader header = new FrameHeader();
        header.setFinished(true);
        header.setMask(false);
        header.setOpCode(FrameHeader.OpCode.TEXT);
        return new Frame(header, ByteBuffer.wrap(text.getBytes(StandardCharsets.UTF_8)));
    }

    public static Frame close(CloseCode closeCode, String reason) {
        FrameHeader header = new FrameHeader();
        header.setFinished(true);
        header.setMask(false);
        header.setOpCode(FrameHeader.OpCode.CLOSE);
        byte[] reasonBytes = reason.getBytes(StandardCharsets.UTF_8);
        ByteBuffer payload = ByteBuffer.allocate(2 + reasonBytes.length);
        payload.putShort(closeCode.code());
        payload.put(reasonBytes);
        payload.flip();
        return new Frame(header, payload);
    }

    private FrameHeader header;
    private Payload payload;

    public Frame(FrameHeader header, ByteBuffer payload) {
        this.header = header;
        this.payload = new Payload(payload, true);
        this.header.setPayloadSize(payload.remaining());
    }

    public FrameHeader getHeader() {
        return header;
    }

    public void setHeader(FrameHeader header) {
        this.header = header;
    }

    public Payload getPayload() {
        return payload;
    }

    public void setPayload(Payload payload) {
        this.payload = payload;
    }

    @Override
    public ByteBuffer serialize() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "Frame{" +
                "header=" + header +
                ", payload=" + (header.getOpCode() == FrameHeader.OpCode.TEXT ? BufferUtils.toString(payload.getData()) : payload) +
                '}';
    }
}
