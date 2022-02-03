package vest.doctor.reactor.http;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.DefaultLastHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * HTTP response body data.
 */
public interface ResponseBody {

    /**
     * Create the data flow to be sent to the client.
     */
    Publisher<HttpContent> content();

    /**
     * Create a response body of a single {@link ByteBuf}.
     */
    static ResponseBody of(ByteBuf buf) {
        return new DefaultResponseBody(buf);
    }

    /**
     * Create a response body of a byte array. The entire contents of the byte array will be sent to the
     * client.
     */
    static ResponseBody of(byte[] bytes) {
        return of(Unpooled.wrappedBuffer(bytes));
    }

    /**
     * Create a response body from a string. The string will be encoded using {@link StandardCharsets#UTF_8}.
     */
    static ResponseBody of(String str) {
        return of(str, StandardCharsets.UTF_8);
    }

    /**
     * Create a response body from a string.
     */
    static ResponseBody of(String str, Charset charset) {
        return new DefaultResponseBody(Unpooled.wrappedBuffer(str.getBytes(charset)));
    }

    /**
     * Create a response body from a flow of {@link HttpContent}.
     */
    static ResponseBody ofContent(Publisher<HttpContent> publisher) {
        return new PublishingResponseBody(publisher);
    }

    /**
     * Create a response body from a flow of {@link ByteBuf ByteBufs}. The buffers will be converted into
     * {@link HttpContent} and a {@link LastHttpContent} will be appended to the end of the flow.
     */
    static ResponseBody ofBuffers(Publisher<ByteBuf> publisher) {
        Flux<HttpContent> data = Flux.concat(Flux.from(publisher).map(DefaultHttpContent::new), Flux.just(LastHttpContent.EMPTY_LAST_CONTENT));
        return new PublishingResponseBody(data);
    }

    /**
     * Create an empty response body.
     */
    static ResponseBody empty() {
        return () -> Mono.just(LastHttpContent.EMPTY_LAST_CONTENT);
    }

    record DefaultResponseBody(ByteBuf buf) implements ResponseBody {
        @Override
        public Publisher<HttpContent> content() {
            return Mono.just(buf).map(DefaultLastHttpContent::new);
        }
    }

    record PublishingResponseBody(Publisher<HttpContent> publisher) implements ResponseBody {
        @Override
        public Publisher<HttpContent> content() {
            return publisher;
        }
    }
}
