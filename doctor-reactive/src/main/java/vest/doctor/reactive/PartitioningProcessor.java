package vest.doctor.reactive;

import java.util.LinkedList;
import java.util.List;

public final class PartitioningProcessor<I> extends AbstractProcessor<I, List<I>> {

    private final int size;
    private List<I> list;

    public PartitioningProcessor(int size) {
        this.size = size;
    }

    @Override
    protected void handleNextItem(I item) {
        synchronized (this) {
            if (list == null) {
                list = new LinkedList<>();
            }
            list.add(item);
            if (list.size() >= size) {
                publishDownstream(new LinkedList<>(list));
                list = null;
            }
        }
    }

    @Override
    public void onComplete() {
        synchronized (this) {
            if (list != null && !list.isEmpty()) {
                publishDownstream(new LinkedList<>(list));
                list = null;
            }
        }
        super.onComplete();
    }
}
