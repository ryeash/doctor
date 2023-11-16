package vest.doctor.ssf.impl;

import java.nio.ByteBuffer;
import java.util.LinkedList;
import java.util.List;

class ByteArrayCollector {
    private int total = 0;
    private final List<byte[]> chunks = new LinkedList<>();

    public void append(ByteBuffer buf) {
        total += buf.remaining();
        byte[] chunk = new byte[buf.remaining()];
        buf.get(chunk);
        chunks.add(chunk);
    }

    public void append(byte[] bytes) {
        total += bytes.length;
        chunks.add(bytes);
    }

    public int size() {
        return total;
    }

    public void clear() {
        total = 0;
        chunks.clear();
    }

    public byte[] aggregate() {
        byte[] agg = new byte[total];
        int read = 0;
        for (byte[] chunk : chunks) {
            System.arraycopy(chunk, 0, agg, read, chunk.length);
            read += chunk.length;
        }
        return agg;
    }
}
