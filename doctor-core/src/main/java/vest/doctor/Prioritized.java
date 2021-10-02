package vest.doctor;

import java.util.Comparator;

/**
 * Marker interface to indicate that implementations are to be sorted according to their priority relative to other
 * like-typed objects. Lower priority sorts first.
 */
public interface Prioritized {

    int DEFAULT_PRIORITY = 1000;
    Comparator<Prioritized> COMPARATOR = Comparator.comparingInt(Prioritized::priority);

    /**
     * The priority for this object. By default, lower values will sort before higher values.
     *
     * @default 1000
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }
}
