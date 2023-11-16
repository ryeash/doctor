package vest.doctor.ssf.impl;

import vest.doctor.ssf.Request;
import vest.doctor.ssf.SSFException;
import vest.doctor.ssf.Status;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class RequestParser {

    public enum State {
        REQUEST_LINE,
        HEADERS,
        BODY,
        BODY_FIXED,
        READ_CHUNK_SIZE,
        READ_CHUNK,
        READ_CHUNK_EOL,
        READ_CHUNK_FOOTER,
        DONE,
        CORRUPT
    }

    private static final byte SPACE = ' ';

    private final Configuration conf;

    private RequestImpl request = new RequestImpl();
    private State state = State.REQUEST_LINE;
    private ByteBuffer lineBuffer;

    private final ByteArrayCollector byteArrayCollector = new ByteArrayCollector();
    private int leftToRead = 0;

    public RequestParser(Configuration conf) {
        this.conf = conf;
        this.lineBuffer = conf.allocateBuffer(conf.initialParseBufferSize());
    }

    public State state() {
        return state;
    }

    public Request getRequest() {
        if (state != State.DONE) {
            throw new SSFException(Status.INTERNAL_SERVER_ERROR, "Attempted to get an incomplete request");
        }
        return request;
    }

    public void parse(ByteBuffer buf) {
        try {
            while (buf.hasRemaining()) {
                switch (state) {
                    case REQUEST_LINE:
                        state = parseRequestLine(buf);
                        break;
                    case HEADERS:
                        state = parseHeaders(buf);
                        if (state == State.BODY) {
                            state = determineBodyReadType();
                        }
                        break;
                    case READ_CHUNK_SIZE:
                        state = parseChunkSize(buf);
                        break;
                    case READ_CHUNK:
                        state = parseChunk(buf);
                        break;
                    case READ_CHUNK_EOL:
                        state = parseChunkEOL(buf);
                        break;
                    case READ_CHUNK_FOOTER:
                        state = parseChunkFooter(buf);
                        break;
                    case BODY_FIXED:
                        state = parseFixedLengthBody(buf);
                        break;
                    case DONE:
                    case CORRUPT:
                        return;
                    default:
                        throw new SSFException(Status.INTERNAL_SERVER_ERROR, "Unhandled parse state: " + state);
                }
            }
        } catch (Exception e) {
            state = State.CORRUPT;
            throw e;
        }
    }

    public State parseRequestLine(ByteBuffer buf) {
        boolean completeLine = readLineBytes(buf);
        if (completeLine) {
            lineBuffer.flip();
            String methodStr = toString(lineBuffer, SPACE);
            lineBuffer.get();
            String uriStr = toString(lineBuffer, SPACE);
            lineBuffer.get();
            String protocolVersion = toString(lineBuffer);
            if (methodStr.isEmpty()) {
                state = State.CORRUPT;
            }
            URI uri = URI.create(uriStr);
            request = new RequestImpl(protocolVersion, methodStr, uri);
            lineBuffer.clear();
            return State.HEADERS;
        } else {
            return State.REQUEST_LINE;
        }
    }

    public State parseHeaders(ByteBuffer buf) {
        while (true) {
            boolean completeLine = readLineBytes(buf);
            if (completeLine) {
                lineBuffer.flip();
                if (lineBuffer.remaining() == 2) {
                    return State.BODY;
                }
                String name = toString(lineBuffer, (byte) ':');
                lineBuffer.get();
                String value = toString(lineBuffer);
                lineBuffer.clear();
                request.addHeader(name.trim(), value.trim());
            } else {
                return State.HEADERS;
            }
        }
    }

    public State determineBodyReadType() {
        if (Objects.equals(Utils.CHUNKED, request.transferEncoding())) {
            leftToRead = 0;
            return State.READ_CHUNK_SIZE;
        } else {
            leftToRead = request.contentLength();
            if (leftToRead == 0) {
                return State.DONE;
            }
            if (leftToRead > conf.bodyMaxLength()) {
                state = State.CORRUPT;
                throw new SSFException(Status.REQUEST_ENTITY_TOO_LARGE, "Request body content length was too large");
            }
            return State.BODY_FIXED;
        }
    }

    public State parseFixedLengthBody(ByteBuffer buf) {
        if (leftToRead <= 0) {
            request.body(byteArrayCollector.aggregate());
            return State.DONE;
        }
        int toRead = Math.min(buf.remaining(), leftToRead);
        byte[] temp = new byte[toRead];
        buf.get(temp, 0, toRead);
        byteArrayCollector.append(temp);
        leftToRead -= toRead;
        if (leftToRead <= 0) {
            request.body(byteArrayCollector.aggregate());
            return State.DONE;
        } else {
            return State.BODY_FIXED;
        }
    }

    public State parseChunkSize(ByteBuffer buf) {
        boolean completeLine = readLineBytes(buf);
        if (completeLine) {
            String chunkSizeStr = lineBufferToString();
            leftToRead = Integer.valueOf(chunkSizeStr, 16);
            return (leftToRead > 0) ? State.READ_CHUNK : State.READ_CHUNK_FOOTER;
        }
        return State.READ_CHUNK_SIZE;
    }

    public State parseChunk(ByteBuffer buf) {
        int toRead = Math.min(buf.remaining(), leftToRead);
        if ((byteArrayCollector.size() + toRead) > conf.bodyMaxLength()) {
            state = State.CORRUPT;
            throw new SSFException(Status.REQUEST_ENTITY_TOO_LARGE, "Request body content length was too large");
        }
        byte[] temp = new byte[toRead];
        buf.get(temp, 0, toRead);
        byteArrayCollector.append(temp);
        leftToRead -= toRead;
        if (leftToRead <= 0) {
            return State.READ_CHUNK_EOL;
        }
        return State.READ_CHUNK;
    }

    public State parseChunkEOL(ByteBuffer buf) {
        boolean completeLine = readLineBytes(buf);
        return completeLine ? State.READ_CHUNK_SIZE : State.READ_CHUNK_EOL;
    }

    // read the final \r\n at the end of chunked content
    public State parseChunkFooter(ByteBuffer buf) {
        boolean completeLine = readLineBytes(buf);
        if (completeLine) {
            request.body(byteArrayCollector.aggregate());
            return State.DONE;
        }
        return state;
    }

    // fills the lineBuffer,
    // returns true if it actually read a line,
    // false if the buffer ran out before the \r\n was reached
    public boolean readLineBytes(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            byte b = buffer.get();
            lineBuffer.put(b);
            if (lineBuffer.position() == conf.headerMaxLength()) {
                throw new SSFException(Status.REQUEST_HEADER_FIELDS_TOO_LARGE, "exceeded max line length: " + conf.headerMaxLength());
            }
            if (lineBuffer.remaining() == 0) {
                lineBuffer = conf.allocateBuffer(Math.min(lineBuffer.capacity() + 1024, conf.headerMaxLength()))
                        .put(lineBuffer.flip());
            }
            if (lineBuffer.position() > 1 && lineBuffer.get(lineBuffer.position() - 2) == '\r' && lineBuffer.get(lineBuffer.position() - 1) == '\n') {
                return true;
            }
        }
        return false;
    }

    public String lineBufferToString() {
        String s = toString(lineBuffer).trim();
        lineBuffer.clear();
        return s;
    }

    public void reset() {
        request = new RequestImpl();
        state = State.REQUEST_LINE;
        // keep the line buffer at its current size
        lineBuffer.clear();
        leftToRead = 0;
        byteArrayCollector.clear();
    }

    public int indexOf(ByteBuffer buf, byte b, int from) {
        int pos = buf.position();
        for (int i = from; i < buf.remaining(); i++) {
            if (buf.get(pos + i) == b) {
                return i;
            }
        }
        return -1;
    }

    public String toString(ByteBuffer buf, byte stop) {
        int pos = indexOf(buf, stop, buf.position());
        if (pos < 0) {
            throw new SSFException(Status.BAD_REQUEST, "missing expected character [" + (char) stop + "]");
        }
        if (pos == 0) {
            return "";
        }
        byte[] b = new byte[pos];
        buf.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }

    public String toString(ByteBuffer buf) {
        return StandardCharsets.UTF_8.decode(buf).toString();
    }

    public static int indexOf(ByteBuffer buf, byte[] b) {
        int pos = buf.position();
        for (int i = 0; i < buf.remaining() - b.length; i++) {
            boolean matched = true;
            for (int j = 0; j < b.length; j++) {
                if (buf.get(pos + i + j) != b[j]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return i;
            }
        }
        return -1;
    }
}
