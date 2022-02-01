package vest.doctor.reactor.http.impl;

import io.netty.handler.codec.http.multipart.HttpData;
import reactor.core.publisher.Flux;
import vest.doctor.reactor.http.MultiPartData;

public record MultiPartDataImpl(Flux<HttpData> data) implements MultiPartData {
}
