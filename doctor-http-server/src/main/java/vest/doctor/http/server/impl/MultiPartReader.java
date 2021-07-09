package vest.doctor.http.server.impl;

import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.FileUpload;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import vest.doctor.http.server.HttpException;
import vest.doctor.http.server.Part;
import vest.doctor.http.server.RxRequest;
import vest.doctor.pipeline.Pipeline;

import java.util.Objects;

final class MultiPartReader {

    private final HttpPostRequestDecoder decoder;

    public MultiPartReader(HttpRequest request) {
        this.decoder = new HttpPostRequestDecoder(request);
    }

    public Pipeline<Part> read(RxRequest request) {
        boolean valid = decoder.isMultipart();
        if (!valid) {
            throw new HttpException("invalid multipart request data");
        }
        return request.body()
                .map(this::next)
                .filter(Objects::nonNull);
    }

    private Part next(HttpContent content) {
        try {
            decoder.offer(content);
            InterfaceHttpData next;
            while ((next = decoder.next()) != null) {
                String type = next.getHttpDataType().name();
                String name = next.getName();
                switch (next.getHttpDataType()) {
                    case Attribute, InternalAttribute -> {
                        Attribute attribute = (Attribute) next;
                        return new PartImpl(type, name, attribute.content().retainedDuplicate(), false);
                    }
                    case FileUpload -> {
                        FileUpload fileUpload = (FileUpload) next;
                        return new PartImpl(type, name, fileUpload.content().retainedDuplicate(), false);
                    }
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException end) {
            return new PartImpl("", "", Unpooled.EMPTY_BUFFER, true);
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
}
