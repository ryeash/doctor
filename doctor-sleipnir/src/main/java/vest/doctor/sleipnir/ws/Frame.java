package vest.doctor.sleipnir.ws;

import vest.doctor.sleipnir.http.HttpData;

import java.nio.ByteBuffer;

public class Frame implements HttpData {

    private FrameHeader header;
    private Payload payload;

    public Frame(FrameHeader header, ByteBuffer payload) {
        this.header = header;
        this.payload = new Payload(payload, true);
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
}
