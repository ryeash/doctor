package vest.doctor.netty;

import vest.doctor.Prioritized;

public interface ExceptionHandler extends Prioritized {

    Class<? extends Throwable> type();

    Response handle(Request request, Throwable error);
}
