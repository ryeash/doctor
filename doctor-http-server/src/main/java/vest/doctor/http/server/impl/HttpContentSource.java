package vest.doctor.http.server.impl;

import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.LastHttpContent;
import vest.doctor.workflow.AbstractSource;
import vest.doctor.workflow.WorkflowState;

final class HttpContentSource extends AbstractSource<HttpContent> {

    @Override
    public void cancel() {
        stateChange(WorkflowState.SUBSCRIBED, WorkflowState.CANCELLED);
    }

    @Override
    public void onNext(HttpContent value) {
        super.onNext(value);
        if (requested.get() > 0) {
            requested.decrementAndGet();
            try {
                subscriber.onNext(value);
            } catch (Throwable t) {
                onError(t);
            }
            if (value instanceof LastHttpContent) {
                subscriber.onComplete();
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void onComplete() {
        throw new UnsupportedOperationException();
    }
}
