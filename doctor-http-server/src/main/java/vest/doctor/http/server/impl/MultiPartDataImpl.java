package vest.doctor.http.server.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
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
            return body.asyncRead(this::nextData).thenApply(o -> true);
        } else {
            CompletableFuture<Boolean> future = new CompletableFuture<>();
            future.completeExceptionally(new HttpException(HttpResponseStatus.BAD_REQUEST, "expecting a multipart request"));
            return future;
        }
    }

    private Object nextData(HttpContent content) {
        try {
            decoder.offer(content);
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
            if (content instanceof LastHttpContent) {
                decoder.destroy();
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "MultiPartData{consuming=" + (consumer != null) + ", " + '}';
    }

    private record PartImpl(String type, String name, ByteBuf data, boolean last) implements Part {
    }
}
