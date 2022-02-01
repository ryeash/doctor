package demo.app;

import io.netty.buffer.ByteBuf;
import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import vest.doctor.reactor.http.Filter;
import vest.doctor.reactor.http.FilterChain;
import vest.doctor.reactor.http.HttpRequest;
import vest.doctor.reactor.http.HttpResponse;
import vest.doctor.reactor.http.RequestContext;
import vest.doctor.reactor.http.ResponseBody;

import java.util.Objects;

@Singleton
public class TestFilter implements Filter {
    @Override
    public Publisher<HttpResponse> filter(RequestContext requestContext, FilterChain chain) throws Exception {
        HttpRequest request = requestContext.request();
        if (Objects.equals(request.queryParam("halt"), "true")) {
            return request.body()
                    .map(ByteBuf::release)
                    .ignoreElements()
                    .then(Mono.just(requestContext.response().status(202).body(ResponseBody.of("halted"))));
        }

        requestContext.attribute("test.attribute", "value");
        return Flux.from(chain.next(requestContext))
                .map(r -> r.header("X-After", "true"));
    }
}
