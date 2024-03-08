package vest.doctor.sleipnir.http;

import vest.doctor.sleipnir.BaseProcessor;
import vest.doctor.sleipnir.BufferUtils;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Flow;

public class RequestDecoder extends BaseProcessor<ByteBuffer, HttpData> {

    private enum State {
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
    public void onSubscribe(Flow.Subscription subscription) {
        super.onSubscribe(subscription);
        subscription.request(Long.MAX_VALUE);
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
        } catch (Throwable e) {
            state = State.CORRUPT;
            subscriber().onError(e);
            throw e;
        }
    }

    private State parseRequestLine(ByteBuffer buf) {
        boolean completeLine = readLineBytes(buf);
        if (completeLine) {
            lineBuffer.flip();
            String methodStr = toString(lineBuffer, HttpData.SPACE);
            lineBuffer.get();
            String uriStr = toString(lineBuffer, HttpData.SPACE);
            lineBuffer.get();
            ProtocolVersion protocolVersion = ProtocolVersion.valueOf(BufferUtils.toString(lineBuffer).trim());
            if (methodStr.isEmpty()) {
                throw new HttpException(Status.BAD_REQUEST, "");
            }
            URI uri = URI.create(uriStr);
            subscriber().onNext(new RequestLine(methodStr, uri, protocolVersion));
            lineBuffer.clear();
            return State.HEADERS;
        } else {
            return State.REQUEST_LINE;
        }
    }

    private State parseHeaders(ByteBuffer buf) {
        while (true) {
            boolean completeLine = readLineBytes(buf);
            if (completeLine) {
                lineBuffer.flip();
                if (lineBuffer.remaining() == 2) {
                    return bodyReadMode();
                }
                String name = toString(lineBuffer, (byte) ':').trim();
                lineBuffer.get();
                String value = BufferUtils.toString(lineBuffer).trim();
                lineBuffer.clear();
                Header header = new Header(name, value);
                determineBodyReadType(header);
                subscriber().onNext(header);
            } else {
                return State.HEADERS;
            }
        }
    }

    private void determineBodyReadType(Header header) {
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
            subscriber().onNext(Body.LAST_EMPTY);
            return State.DONE;
        } else {
            return State.BODY_FIXED;
        }
    }

    private State parseFixedLengthBody(ByteBuffer buf) {
        if (leftToRead <= 0) {
            subscriber().onNext(Body.LAST_EMPTY);
            return State.DONE;
        }
        int toRead = Math.min(buf.remaining(), leftToRead);
        ByteBuffer data = ByteBuffer.allocate(toRead);
        BufferUtils.transfer(buf, data);
        data.flip();
        subscriber().onNext(new Body(data, false));
        leftToRead -= toRead;
        if (leftToRead <= 0) {
            subscriber().onNext(Body.LAST_EMPTY);
            return State.DONE;
        } else {
            return State.BODY_FIXED;
        }
    }

    private State parseChunkSize(ByteBuffer buf) {
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

    private State parseChunk(ByteBuffer buf) {
        int toRead = Math.min(buf.remaining(), leftToRead);
        if ((totalRead + toRead) > maxBodyLength) {
            state = State.CORRUPT;
            throw new HttpException(Status.REQUEST_ENTITY_TOO_LARGE, "request body content length was too large");
        }
        ByteBuffer data = ByteBuffer.allocate(toRead);
        BufferUtils.transfer(buf, data);
        data.flip();
        subscriber().onNext(new Body(data, false));
        leftToRead -= toRead;
        if (leftToRead <= 0) {
            return State.READ_CHUNK_EOL;
        }
        return State.READ_CHUNK;
    }

    private State parseChunkEOL(ByteBuffer buf) {
        boolean completeLine = readLineBytes(buf);
        return completeLine ? State.READ_CHUNK_SIZE : State.READ_CHUNK_EOL;
    }

    // read the final \r\n at the end of chunked content
    private State parseChunkFooter(ByteBuffer buf) {
        boolean completeLine = readLineBytes(buf);
        if (completeLine) {
            subscriber().onNext(Body.LAST_EMPTY);
            return State.DONE;
        }
        return state;
    }

    // fills the lineBuffer,
    // returns true if it actually read a line,
    // false if the buffer ran out before the \r\n was reached
    private boolean readLineBytes(ByteBuffer buffer) {
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

    private String lineBufferToString() {
        String s = BufferUtils.toString(lineBuffer).trim();
        lineBuffer.clear();
        return s;
    }

    private void reset() {
        state = State.REQUEST_LINE;
        // keep the line buffer at its current size
        lineBuffer.clear();
        totalRead = 0;
        leftToRead = 0;
    }

    private String toString(ByteBuffer buf, byte stop) {
        int pos = BufferUtils.indexOf(buf, stop);
        if (pos < 0) {
            throw new HttpException(Status.BAD_REQUEST, "missing expected character [" + (char) stop + "] " + BufferUtils.toString(buf));
        }
        if (pos == 0) {
            return "";
        }
        byte[] b = new byte[pos - buf.position()];
        buf.get(b);
        return new String(b, StandardCharsets.UTF_8);
    }
}
