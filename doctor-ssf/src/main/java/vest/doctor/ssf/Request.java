package vest.doctor.ssf;

import vest.doctor.rx.FlowBuilder;

import java.net.URI;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Flow;

public interface Request extends HeaderContainer, AttributeContainer {

    ExecutorService pool();

    String schemaVersion();

    String method();

    URI uri();

    Flow.Publisher<HttpData> bodyFlow();

    default Flow.Publisher<Response> respond() {
        return FlowBuilder.of(Response.ok(), pool());
    }
}
