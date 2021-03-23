package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import vest.doctor.http.server.HttpException;
import vest.doctor.http.server.MultiPartData;
import vest.doctor.http.server.RequestBody;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedList;
import java.util.concurrent.CompletableFuture;

class MultiPartDataImpl implements MultiPartData {

    private final HttpPostRequestDecoder decoder;
    private final Collection<Part> parts = new LinkedList<>();
    private final CompletableFuture<Iterable<Part>> future;

    public MultiPartDataImpl(HttpRequest request, RequestBody body) {
        HttpPostRequestDecoder.isMultipart(request);
        this.decoder = new HttpPostRequestDecoder(request);
        if (decoder.isMultipart()) {
            this.future = body.asyncRead(this::nextData);
        } else {
            this.future = new CompletableFuture<>();
            this.future.completeExceptionally(new HttpException(400, "expecting a multipart request"));
        }
    }

    @Override
    public boolean valid() {
        return decoder.isMultipart();
    }

    @Override
    public CompletableFuture<Iterable<Part>> future() {
        return future;
    }

    private Iterable<Part> nextData(ByteBuf buf, boolean finished) {
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
                        parts.add(new PartImpl(type, name, attribute.content().retainedDuplicate()));
                    }
                    case FileUpload -> {
                        FileUpload fileUpload = (FileUpload) next;
                        parts.add(new PartImpl(type, name, fileUpload.content().retainedDuplicate()));
                    }
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException end) {
            finished = true;
        } catch (Throwable t) {
            finished = true;
            parts.clear();
            decoder.destroy();
            future.completeExceptionally(t);
            return null;
        } finally {
            if (finished) {
                decoder.destroy();
            }
        }
        return finished ? parts : null;
    }

    @Override
    public String toString() {
        return "MultiPartData{" + parts + '}';
    }

    private static final class PartImpl implements Part {
        private final String type;
        private final String name;
        private final ByteBuf data;

        public PartImpl(String type, String name, ByteBuf data) {
            this.type = type;
            this.name = name;
            this.data = data;
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
