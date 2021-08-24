package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import vest.doctor.http.server.HttpException;
import vest.doctor.http.server.MultiPartData;
import vest.doctor.http.server.RequestBody;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

class MultiPartDataImpl implements MultiPartData {

    private final RequestBody body;
    private final HttpPostRequestDecoder decoder;
    private final boolean valid;
    private Consumer<Part> consumer;

    public MultiPartDataImpl(HttpRequest request, RequestBody body) {
        this.body = body;
        this.decoder = new HttpPostRequestDecoder(request);
        this.valid = decoder.isMultipart();
        if (!valid) {
            decoder.destroy();
        }
    }

    @Override
    public boolean valid() {
        return valid;
    }

    @Override
    public CompletableFuture<Boolean> receive(Consumer<Part> consumer) {
        if (this.consumer != null) {
            throw new IllegalStateException("a consumer has already been attached");
        }
        this.consumer = consumer;

        if (valid) {
            return body.asyncRead(this::nextData);
        } else {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new HttpException(HttpResponseStatus.BAD_REQUEST, "expecting a multipart request"));
            return future;
        }
    }

    private Boolean nextData(ByteBuf buf, boolean finished) {
        try {
            decoder.offer(new DefaultHttpContent(buf));
            if (finished) {
                decoder.offer(LastHttpContent.EMPTY_LAST_CONTENT);
            }
            InterfaceHttpData next;
            while ((next = decoder.next()) != null) {
                String type = next.getHttpDataType().name();
                String name = next.getName();
                switch (next.getHttpDataType()) {
                    case Attribute, InternalAttribute -> {
                        Attribute attribute = (Attribute) next;
                        consumer.accept(new PartImpl(type, name, attribute.content().retainedDuplicate(), false));
                    }
                    case FileUpload -> {
                        FileUpload fileUpload = (FileUpload) next;
                        consumer.accept(new PartImpl(type, name, fileUpload.content().retainedDuplicate(), false));
                    }
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException end) {
            consumer.accept(new PartImpl("", "", Unpooled.EMPTY_BUFFER, true));
            return true;
        } catch (Throwable t) {
            decoder.destroy();
            throw t;
        } finally {
            if (finished) {
                decoder.destroy();
            }
        }
        return finished ? true : null;
    }

    @Override
    public String toString() {
        return "MultiPartData{consuming=" + (consumer != null) + ", " + '}';
    }

    private static final class PartImpl implements Part {
        private final String type;
        private final String name;
        private final ByteBuf data;
        private final boolean last;

        public PartImpl(String type, String name, ByteBuf data, boolean last) {
            this.type = type;
            this.name = name;
            this.data = data;
            this.last = last;
        }

        public String getType() {
            return type;
        }

        public String getName() {
            return name;
        }

        public ByteBuf getData() {
            return data;
        }

        public boolean isLast() {
            return last;
        }

        @Override
        public String toString() {
            return "Part{" +
                    "type='" + type + '\'' +
                    ", name='" + name + '\'' +
                    ", data=" + data.toString(StandardCharsets.UTF_8) +
                    '}';
        }
    }
}
