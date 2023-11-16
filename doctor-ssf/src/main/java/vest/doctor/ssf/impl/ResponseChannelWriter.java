package vest.doctor.ssf.impl;


import vest.doctor.ssf.RequestContext;
import vest.doctor.ssf.Response;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;

import static vest.doctor.ssf.impl.Utils.CLOSED;
import static vest.doctor.ssf.impl.Utils.CR;
import static vest.doctor.ssf.impl.Utils.LF;

public class ResponseChannelWriter implements ReadableByteChannel {
    public static byte[] HTTP11 = "HTTP/1.1".getBytes(Utils.UTF_8);
    public static byte SP = ' ';
    public static byte CLN = ':';
    public static byte[] CR_LF = new byte[]{CR, LF};

    private final Configuration conf;
    private final boolean keepAlive;
    private final Response response;
    private final ReadableByteChannel statusAndHeaders;
    private final ReadableByteChannel body;

    public ResponseChannelWriter(Configuration conf, RequestContext ctx) {
        this.conf = conf;
        this.response = ctx.response();
        this.keepAlive = !CLOSED.equalsIgnoreCase(ctx.request().connection())
                && !CLOSED.equalsIgnoreCase(ctx.response().connection());
        
        ByteBuffer[] top = new ByteBuffer[2 + response.headerNames().size()];
        top[0] = conf.allocateBuffer(HTTP11.length + response.status().bytes().length + 3)
                .put(HTTP11)
                .put(SP)
                .put(response.status().bytes())
                .put(CR_LF)
                .flip();
        Iterator<String> headerNames = response.headerNames().iterator();
        for (int i = 1; headerNames.hasNext(); i++) {
            String header = headerNames.next();
            top[i] = headerBuf(header, String.join(",", response.getHeaders(header)));
        }
        top[top.length - 1] = ByteBuffer.wrap(CR_LF);
        this.statusAndHeaders = new ByteBufArrReadableChannel(top);
        this.body = new ByteBufReadableChannel(ByteBuffer.wrap(response.body()));
    }

    public boolean keepAlive() {
        return keepAlive;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int start = dst.position();
        while (dst.hasRemaining()) {
            if (statusAndHeaders.isOpen()) {
                statusAndHeaders.read(dst);
            } else if (body.isOpen()) {
                body.read(dst);
            } else {
                return -1;
            }
        }
        return dst.position() - start;
    }

    @Override
    public boolean isOpen() {
        return statusAndHeaders.isOpen() || body.isOpen();
    }

    @Override
    public void close() throws IOException {
        statusAndHeaders.close();
        body.close();
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
