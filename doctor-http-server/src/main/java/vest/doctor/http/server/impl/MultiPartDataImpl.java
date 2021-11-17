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
import vest.doctor.flow.Emitter;
import vest.doctor.flow.Flo;
import vest.doctor.http.server.HttpException;
import vest.doctor.http.server.MultiPartData;
import vest.doctor.http.server.RequestBody;

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

    public Flo<?, Part> parts() {
        if (valid) {
            return body.flow()
                    .step(Part.class, (c, sub, emit) -> {
                        nextData(c, emit);
                    })
                    .takeWhile(p -> !p.last(), true);
        } else {
            return body.flow()
                    .map(content -> {
                        throw new HttpException(HttpResponseStatus.BAD_REQUEST, "expecting a multipart request");
                    });
        }
    }

    private void nextData(HttpContent content, Emitter<Part> emitter) {
        try {
            decoder.offer(content);
            InterfaceHttpData next;
            while ((next = decoder.next()) != null) {
                String type = next.getHttpDataType().name();
                String name = next.getName();
                switch (next.getHttpDataType()) {
                    case Attribute, InternalAttribute -> {
                        Attribute attribute = (Attribute) next;
                        emitter.emit(new PartImpl(type, name, attribute.content().retainedDuplicate(), false));
                    }
                    case FileUpload -> {
                        FileUpload fileUpload = (FileUpload) next;
                        emitter.emit(new PartImpl(type, name, fileUpload.content().retainedDuplicate(), false));
                    }
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException end) {
            emitter.emit(new PartImpl("", "", Unpooled.EMPTY_BUFFER, true));
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
