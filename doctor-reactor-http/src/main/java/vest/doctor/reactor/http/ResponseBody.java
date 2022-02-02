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

    static ResponseBody of(byte[] bytes) {
        return of(Unpooled.wrappedBuffer(bytes));
    }

    static ResponseBody of(String str) {
        return of(str, StandardCharsets.UTF_8);
    }

    static ResponseBody of(String str, Charset charset) {
        return new DefaultResponseBody(Unpooled.wrappedBuffer(str.getBytes(charset)));
    }

    static ResponseBody ofContent(Publisher<HttpContent> publisher) {
        return new PublishingResponseBody(publisher);
    }

    static ResponseBody ofBuffers(Publisher<ByteBuf> publisher) {
        Flux<HttpContent> data = Flux.concat(Flux.from(publisher).map(DefaultHttpContent::new), Flux.just(LastHttpContent.EMPTY_LAST_CONTENT));
        return new PublishingResponseBody(data);
    }

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
