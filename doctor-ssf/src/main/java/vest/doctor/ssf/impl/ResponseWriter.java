package vest.doctor.ssf.impl;


import vest.doctor.ssf.RequestContext;
import vest.doctor.ssf.Response;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static vest.doctor.ssf.impl.Utils.CLOSED;
import static vest.doctor.ssf.impl.Utils.CR;
import static vest.doctor.ssf.impl.Utils.LF;

public class ResponseWriter {
    public static byte[] HTTP11 = "HTTP/1.1".getBytes(Utils.UTF_8);
    public static byte SP = ' ';
    public static byte CLN = ':';
    public static byte[] CR_LF = new byte[]{CR, LF};

    private final Configuration conf;
    private final boolean keepAlive;
    private final ByteBuffer[] outBufs;

    public ResponseWriter(Configuration conf, RequestContext ctx) {
        this.conf = conf;
        Response response = ctx.response();
        this.keepAlive = !CLOSED.equalsIgnoreCase(ctx.request().connection())
                && !CLOSED.equalsIgnoreCase(ctx.response().connection());
        outBufs = toBuf(response);
    }

    public boolean keepAlive() {
        return keepAlive;
    }

    public ByteBuffer[] writeBuffers() {
        return outBufs;
    }

    public boolean hasRemaining() {
        for (ByteBuffer outBuf : outBufs) {
            if (outBuf.hasRemaining()) {
                return true;
            }
        }
        return false;
    }

    private ByteBuffer[] toBuf(Response response) {
        List<ByteBuffer> buffers = new ArrayList<>(response.numHeaders() + 3);
        // status line
        buffers.add(conf.allocateBuffer(HTTP11.length + response.status().bytes().length + 3)
                .put(HTTP11)
                .put(SP)
                .put(response.status().bytes())
                .put(CR_LF)
                .flip());
        // headers
        response.eachHeader((key, value) -> buffers.add(headerBuf(key, value)));
        buffers.add(conf.allocateBuffer(2).put(CR_LF).flip());
        // body
        if (response.body() != null) {
            buffers.add(ByteBuffer.wrap(response.body()));
        }
        return buffers.toArray(ByteBuffer[]::new);
    }

    private ByteBuffer headerBuf(String key, String value) {
        byte[] keyBytes = key.getBytes(StandardCharsets.UTF_8);
        byte[] valueBytes = value.getBytes(StandardCharsets.UTF_8);
        return conf.allocateBuffer(keyBytes.length + valueBytes.length + 3)
                .put(keyBytes)
                .put(CLN)
                .put(valueBytes)
                .put(CR_LF)
                .flip();
    }
}
