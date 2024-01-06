package vest.doctor.ssf.impl;

import vest.doctor.ssf.Response;
import vest.doctor.ssf.Status;

import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;

public final class ResponseImpl extends BaseMessage implements Response {
    private Status status = Status.OK;
    private ReadableByteChannel body;

    public ResponseImpl() {
        contentType("text/plain");
        server("jumpy");
        date(Utils.getServerTime());
        body(new byte[0]);
    }

    @Override
    public Status status() {
        return status;
    }

    @Override
    public void status(Status status) {
        this.status = status;
    }

    @Override
    public ReadableByteChannel body() {
        return body;
    }

    @Override
    public void body(ReadableByteChannel data) {
        this.body = data;
    }

    @Override
    public void body(String response) {
        body(response.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void body(byte[] body) {
        this.body = new ByteBufReadableChannel(ByteBuffer.wrap(body));
        contentLength(body.length);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(status);
        sb.append("\n   Headers:");
        eachHeader((k, v) -> sb.append("\n      ").append(k).append(" : ").append(v));
        if (body != null) {
            sb.append("\n   Body: ").append(body);
        }
        return sb.toString();
    }

    public Response ok(String responseBody) {
        status(Status.OK);
        body(responseBody);
        return this;
    }

    public Response error(Exception e) {
        return error(null, e);
    }

    public Response error(String additionalInfo, Exception e) {
        StringBuilder sb = new StringBuilder();
        if (additionalInfo != null) {
            sb.append(additionalInfo).append("\n");
        }
        sb.append(e.getClass().getCanonicalName()).append(": ");
        sb.append(e.getLocalizedMessage());
        for (StackTraceElement ste : e.getStackTrace()) {
            sb.append("\n   ").append(ste.toString());
        }
        return error(sb.toString());
    }

    public Response error(String message) {
        status(Status.INTERNAL_SERVER_ERROR);
        body(message);
        return this;
    }

    public Response notFound(String message) {
        status(Status.NOT_FOUND);
        body(message.getBytes());
        return this;
    }
}
