package vest.doctor;

import java.util.Comparator;

/**
 * Marker interface to indicate that implementations are to be sorted according to their priority relative to other
 * like-typed objects. Lower priority sorts first.
 */
public interface Prioritized {

    /**
     * The default priority for a prioritized type.
     */
    int DEFAULT_PRIORITY = 1000;

    /**
     * The highest priority, i.e. will always sort first.
     */
    int HIGHEST_PRIORITY = Integer.MIN_VALUE;

    /**
     * The lowest priority, i.e. will always sort last.
     */
    int LOWEST_PRIORITY = Integer.MAX_VALUE;

    /**
     * Default comparator for prioritized types. Sorts in ascending order, e.g. [1, 2, 3].
     */
    Comparator<Prioritized> COMPARATOR = Comparator.comparingInt(Prioritized::priority);

    /**
     * Comparator used to compare objects that may or may not be prioritized.
     */
    Comparator<Object> COMPARATOR_ANY = (a, b) -> {
        if (a instanceof Prioritized aP && b instanceof Prioritized bP) {
            return Integer.compare(aP.priority(), bP.priority());
        } else if (a instanceof Prioritized aP) {
            return Integer.compare(aP.priority(), DEFAULT_PRIORITY);
        } else if (b instanceof Prioritized bP) {
            return Integer.compare(DEFAULT_PRIORITY, bP.priority());
        } else {
            return 0;
        }
    };

    /**
     * The priority for this object. By default, lower values will sort before higher values.
     *
     * @default {@link #DEFAULT_PRIORITY}
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }
}
