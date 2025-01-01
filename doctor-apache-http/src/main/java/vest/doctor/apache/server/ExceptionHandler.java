package vest.doctor.apache.server;

import org.apache.hc.core5.http.nio.support.AsyncResponseBuilder;

@FunctionalInterface
public interface ExceptionHandler extends Comparable<ExceptionHandler> {
    AsyncResponseBuilder handle(RequestContext ctx, Throwable error);

    @Override
    default int compareTo(ExceptionHandler o) {
        return 0;
    }
}
