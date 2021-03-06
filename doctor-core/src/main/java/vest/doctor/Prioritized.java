package vest.doctor;

import java.util.Comparator;

/**
 * Marker interface to indicate that implementations are to be sorted according to their priority relative to other
 * like types objects. Lower priority sorts first.
 */
public interface Prioritized {

    int DEFAULT_PRIORITY = 1000;
    Comparator<Prioritized> COMPARATOR = Comparator.comparingInt(Prioritized::priority);

    /**
     * The priority for this object.
     *
     * @default 1000
     */
    default int priority() {
        return DEFAULT_PRIORITY;
    }
}
