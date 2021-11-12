package vest.doctor.flow;

import java.util.Iterator;

public final class StandardSources {
    private StandardSources() {
    }

    public static class AdhocSource<I> extends AbstractSource<I> {

        @Override
        public void onNext(I item) {
            checkSubscribed();
            if (getAndDecrementRequested() > 0) {
                if (subscriber != null) {
                    try {
                        subscriber.onNext(item);
                    } catch (Throwable t) {
                        onError(t);
                    }
                }
            } else {
                throw new IllegalStateException("no items have been requested");
            }
        }

        @Override
        public void startSubscription() {
            // no-op
        }
    }

    public static class IterableSource<I> extends AbstractSource<I> {

        private final Iterator<I> iterator;

        public IterableSource(Iterable<I> iterable) {
            this.iterator = iterable.iterator();
        }

        @Override
        public void onNext(I item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startSubscription() {
            while (subscriber != null
                    && state.get() == FlowState.SUBSCRIBED
                    && iterator.hasNext()
                    && getAndDecrementRequested() > 0) {
                try {
                    subscriber.onNext(iterator.next());
                } catch (Throwable t) {
                    onError(t);
                }
            }
            if (!iterator.hasNext() && state.get() == FlowState.SUBSCRIBED) {
                onComplete();
            }
        }
    }

    public static class OneItemSource<I> extends AbstractSource<I> {

        private final I item;

        public OneItemSource(I item) {
            this.item = item;
        }

        @Override
        public void onNext(I item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startSubscription() {
            if (getAndDecrementRequested() > 0) {
                publishDownstream(item);
                onComplete();
            } else {
                throw new IllegalStateException("no items have been requested");
            }
        }
    }

    public static class ErrorSource<I> extends AbstractSource<I> {

        private final Throwable error;

        public ErrorSource(Throwable error) {
            this.error = error;
        }

        @Override
        public void onNext(I item) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void startSubscription() {
            if (subscriber != null) {
                subscriber.onError(error);
            }
        }
    }
}
