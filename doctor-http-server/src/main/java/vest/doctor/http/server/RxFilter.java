package vest.doctor.http.server;

import vest.doctor.Prioritized;
import vest.doctor.pipeline.Pipeline;

@FunctionalInterface
public interface RxFilter extends Prioritized {

    Pipeline<RxResponse> filter(RxRequest request, Pipeline<RxResponse> pipeline);
}
