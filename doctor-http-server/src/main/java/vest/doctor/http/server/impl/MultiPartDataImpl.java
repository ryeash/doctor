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
import vest.doctor.http.server.MultiPartData;
import vest.doctor.http.server.RequestBody;
import vest.doctor.reactive.Rx;

import java.util.concurrent.Flow;
import java.util.function.Consumer;

class MultiPartDataImpl implements MultiPartData {

    private final RequestBody body;
    private final HttpPostRequestDecoder decoder;
    private final boolean valid;

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
    public Flow.Publisher<Part> parts() {
        if (valid()) {
            return Rx.from(body.flow())
                    .<Part>onNext((content, subscription, subscriber) -> nextData(content, subscriber::onNext))
                    .onNext((part, subscription, subscriber) -> {
                        subscriber.onNext(part);
                        if (part.last()) {
                            subscriber.onComplete();
                        }
                    });
        } else {
            return Rx.from(body.ignored())
                    .map(ignored -> {
                        throw new HttpException(HttpResponseStatus.BAD_REQUEST, "expecting a multipart request");
                    });
        }
    }

    private void nextData(HttpContent content, Consumer<? super Part> emitter) {
        try {
            decoder.offer(content);
            InterfaceHttpData next;
            while ((next = decoder.next()) != null) {
                String type = next.getHttpDataType().name();
                String name = next.getName();
                switch (next.getHttpDataType()) {
                    case Attribute, InternalAttribute -> {
                        Attribute attribute = (Attribute) next;
                        emitter.accept(new PartImpl(type, name, attribute.content().retainedDuplicate(), false));
                    }
                    case FileUpload -> {
                        FileUpload fileUpload = (FileUpload) next;
                        emitter.accept(new PartImpl(type, name, fileUpload.content().retain(), false));
                    }
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException end) {
            emitter.accept(new PartImpl("", "", Unpooled.EMPTY_BUFFER, true));
        } catch (Throwable t) {
            decoder.destroy();
            throw t;
        } finally {
            if (content instanceof LastHttpContent) {
                decoder.destroy();
            }
        }
    }

    @Override
    public String toString() {
        return "MultiPartData{valid=" + valid + '}';
    }

    private record PartImpl(String type, String name, ByteBuf data, boolean last) implements Part {
    }
}
