package vest.doctor.sleipnir.ws;

import vest.doctor.sleipnir.http.HttpData;

import java.nio.ByteBuffer;

public class Payload implements HttpData {

    private final ByteBuffer data;
    private final boolean last;

    public Payload(ByteBuffer data, boolean last) {
        this.data = data;
        this.last = last;
    }

    public ByteBuffer getData() {
        return data;
    }

    public boolean isLast() {
        return last;
    }

    @Override
    public ByteBuffer serialize() {
        return data;
    }
}
