package vest.doctor.reactor.http;

import io.netty.handler.codec.http.multipart.HttpData;
import reactor.core.publisher.Flux;

/**
 * Used to wire multipart data uploaded into endpoints via @Body attribute.
 */
public interface MultiPartData {

    /**
     * Get the multipart data received from the client.
     */
    Flux<HttpData> data();
}
