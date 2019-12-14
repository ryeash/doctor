package vest.doctor;

import java.util.Comparator;

public interface Prioritized {

    Comparator<Prioritized> COMPARATOR = Comparator.comparingInt(Prioritized::priority);

    default int priority() {
        return 1000;
    }
}
