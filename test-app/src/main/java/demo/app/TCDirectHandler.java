package demo.app;

import jakarta.inject.Singleton;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.http.server.HttpServerRequest;
import reactor.netty.http.server.HttpServerResponse;
import vest.doctor.reactor.http.Handler;

import java.util.List;

@Singleton
public class TCDirectHandler implements Handler {
    @Override
    public List<String> method() {
        return List.of("GET");
    }

    @Override
    public List<String> path() {
        return List.of("/hello");
    }

    @Override
    public Publisher<Void> apply(HttpServerRequest httpServerRequest, HttpServerResponse httpServerResponse) {
        httpServerRequest.isMultipart();
        return Flux.just(httpServerResponse)
                .subscribeOn(Schedulers.boundedElastic())
                .switchMap(r -> {
                    System.out.println(Thread.currentThread().getName() + " THERE IS HOPE");
                    return r.sendString(Mono.just("Hello World!"));
                });
    }
}
