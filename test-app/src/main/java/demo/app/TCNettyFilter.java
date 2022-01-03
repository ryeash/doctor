package demo.app;

import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import vest.doctor.flow.Flo;
import vest.doctor.http.server.Filter;
import vest.doctor.http.server.FilterChain;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;
import vest.doctor.http.server.ResponseBody;
import vest.doctor.http.server.rest.Path;
import vest.doctor.util.Heartbeat;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Singleton
@Path("/netty/*")
public class TCNettyFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(TCNettyFilter.class);
    private static final AtomicLong totalBytes = new AtomicLong(0);
    private static final Heartbeat heartbeat = new Heartbeat(5);

    @Override
    public Flo<?, Response> filter(Request request, FilterChain chain) throws Exception {
        if (Objects.equals(request.queryParam("halt"), "true")) {
            return request.body()
                    .ignored()
                    .map(request::createResponse)
                    .map(r -> r.status(202)
                            .body(ResponseBody.of("halted")));
        }

        request.body().inspect(flo ->
                flo.observe(c -> {
                    totalBytes.addAndGet(c.content().readableBytes());
                    heartbeat.tick(i -> log.info("Requests {} - totalBytes {}", i, totalBytes));
                }));

        Optional.ofNullable(request.queryParam("attr"))
                .ifPresent(a -> request.attribute("attr", a));

        request.headers().set("X-BEFORE-ROUTE", true);
        request.attribute("filter", true);
        return chain.next(request)
                .observe(r -> r.headers().set("X-AFTER-ROUTE", true));
    }
}
