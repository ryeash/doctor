package vest.sleipnir.http;

import vest.sleipnir.BaseProcessor;
import vest.sleipnir.BufferUtils;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class RequestDecoder extends BaseProcessor<ByteBuffer, HttpData> {

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

    private State state = State.REQUEST_LINE;
    private final ByteBuffer lineBuffer;
    private final long maxBodyLength;
    private int totalRead = 0;
    private int leftToRead = 0;

    public RequestDecoder(int maxLineLength, long maxBodyLength) {
        this.lineBuffer = ByteBuffer.allocate(maxLineLength);
        this.maxBodyLength = maxBodyLength;
    }

    @Override
    public void onNext(ByteBuffer item) {
        parse(item);
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
                        break;
                    case BODY:
                        throw new UnsupportedOperationException();

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
                        throw new HttpException(Status.INTERNAL_SERVER_ERROR, "Unhandled parse state: " + state);
                }
            }
            if (state == State.DONE) {
                reset();
            }
        } catch (Exception e) {
            state = State.CORRUPT;
            subscriber().onError(e);
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
            ProtocolVersion protocolVersion = ProtocolVersion.valueOf(toString(lineBuffer).trim());
            if (methodStr.isEmpty()) {
                state = State.CORRUPT;
            }
            URI uri = URI.create(uriStr);
            subscriber().onNext(new HttpData.RequestLine(methodStr, uri, protocolVersion));
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
                    return bodyReadMode();
                }
                String name = toString(lineBuffer, (byte) ':').trim();
                lineBuffer.get();
                String value = toString(lineBuffer).trim();
                lineBuffer.clear();
                HttpData.Header header = new HttpData.Header(name, value);
                determineBodyReadType(header);
                subscriber().onNext(header);
            } else {
                return State.HEADERS;
            }
        }
    }

    public void determineBodyReadType(HttpData.Header header) {
        if (header.matches("Transfer-Encoding", "chunked")) {
            leftToRead = -1;
        } else if (header.name().equalsIgnoreCase("Content-Length")) {
            leftToRead = Integer.parseInt(header.value());
            if (leftToRead > maxBodyLength) {
                throw new HttpException(Status.REQUEST_ENTITY_TOO_LARGE, "request body too large");
            }
        }
    }


    private State bodyReadMode() {
        if (leftToRead < 0) {
            return State.READ_CHUNK_SIZE;
        } else if (leftToRead == 0) {
            subscriber().onNext(HttpData.Body.LAST_EMPTY);
            return State.DONE;
        } else {
            return State.BODY_FIXED;
        }
    }

    public State parseFixedLengthBody(ByteBuffer buf) {
        if (leftToRead <= 0) {
            subscriber().onNext(HttpData.Body.LAST_EMPTY);
            return State.DONE;
        }
        int toRead = Math.min(buf.remaining(), leftToRead);
        ByteBuffer data = ByteBuffer.allocate(toRead);
        BufferUtils.transfer(buf, data);
        data.flip();
        subscriber().onNext(new HttpData.Body(data, false));
        leftToRead -= toRead;
        if (leftToRead <= 0) {
            subscriber().onNext(HttpData.Body.LAST_EMPTY);
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
            totalRead += leftToRead;
            if (totalRead > maxBodyLength) {
                throw new HttpException(Status.REQUEST_ENTITY_TOO_LARGE, "request body too large");
            }
            return (leftToRead > 0) ? State.READ_CHUNK : State.READ_CHUNK_FOOTER;
        }
        return State.READ_CHUNK_SIZE;
    }

    public State parseChunk(ByteBuffer buf) {
        int toRead = Math.min(buf.remaining(), leftToRead);
//        if ((totalRead + toRead) > conf.bodyMaxLength()) {
//            state = State.CORRUPT;
//            throw new SSFException(Status.REQUEST_ENTITY_TOO_LARGE, "Request body content length was too large");
//        }
        ByteBuffer data = ByteBuffer.allocate(toRead);
        BufferUtils.transfer(buf, data);
        data.flip();
        subscriber().onNext(new HttpData.Body(data, false));
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
            subscriber().onNext(HttpData.Body.LAST_EMPTY);
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
            if (lineBuffer.position() == lineBuffer.limit()) {
                throw new HttpException(Status.REQUEST_HEADER_FIELDS_TOO_LARGE, "exceeded max line length: " + lineBuffer.limit());
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
        state = State.REQUEST_LINE;
        // keep the line buffer at its current size
        lineBuffer.clear();
        totalRead = 0;
        leftToRead = 0;
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
            throw new HttpException(Status.BAD_REQUEST, "missing expected character [" + (char) stop + "]");
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
}
