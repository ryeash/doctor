package vest.doctor.sleipnir.http;

import vest.doctor.sleipnir.BufferUtils;
import vest.doctor.sleipnir.ByteBufferReadableByteChannel;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

public final class FullResponse implements HttpData {
    private StatusLine statusLine = new StatusLine(ProtocolVersion.HTTP1_1, Status.OK);
    private final Headers headers = new Headers();
    private ReadableByteChannel body = BufferUtils.EMPTY_CHANNEL;

    @Override
    public ByteBuffer serialize() {
        throw new UnsupportedOperationException();
    }

    public FullResponse status(Status status) {
        this.statusLine = new StatusLine(ProtocolVersion.HTTP1_1, status);
        return this;
    }

    public StatusLine statusLine() {
        return statusLine;
    }

    public FullResponse headers(Consumer<Headers> action){
        action.accept(headers);
        return this;
    }
    public Headers headers() {
        return headers;
    }

    public FullResponse body(String str) {
        return body(str, StandardCharsets.UTF_8);
    }

    public FullResponse body(String str, Charset charset) {
        return body(ByteBuffer.wrap(str.getBytes(charset)));
    }

    public FullResponse body(byte[] bytes) {
        return body(ByteBuffer.wrap(bytes));
    }

    public FullResponse body(ByteBuffer byteBuffer) {
        return body(new ByteBufferReadableByteChannel(byteBuffer));
    }

    public FullResponse body(ReadableByteChannel body) {
        this.body = body;
        return this;
    }

    public ReadableByteChannel body() {
        return body;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (FullResponse) obj;
        return Objects.equals(this.statusLine, that.statusLine) &&
                Objects.equals(this.headers, that.headers) &&
                Objects.equals(this.body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(statusLine, headers, body);
    }

    @Override
    public String toString() {
        return "FullResponse[" +
                "statusLine=" + statusLine + ", " +
                "headers=" + headers + ", " +
                "body=" + body + ']';
    }

}
