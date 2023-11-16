package vest.doctor.ssf.impl;

import vest.doctor.ssf.Response;
import vest.doctor.ssf.Status;

public final class ResponseImpl extends Headers implements Response {
    private Status status = Status.OK;
    private byte[] body;

     ResponseImpl() {
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
    public byte[] body() {
        return body;
    }

    @Override
    public void body(String response) {
        body(response.getBytes());
    }

    @Override
    public void body(byte[] response) {
        this.body = response;
        contentLength(body().length);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(status);
        sb.append("\n   Headers:");
        eachHeader((k, v) -> sb.append("\n      ").append(k).append(" : ").append(v));
        if (body != null && body.length > 0) {
            sb.append("\n   Body: ").append(new String(body));
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
