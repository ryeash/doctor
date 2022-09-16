package vest.doctor.runtime;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RuntimeUtils {

    private RuntimeUtils() {
    }

    public static List<String> split(String str, char delimiter) {
        if (str == null || str.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> split = new ArrayList<>();
        int i = 0;
        int next;
        while ((next = str.indexOf(delimiter, i)) > 0) {
            split.add(str.substring(i, next));
            i = next + 1;
        }
        split.add(str.substring(i));
        return split;
    }

    public static void close(Object o) throws Exception {
        if (o instanceof AutoCloseable ac) {
            ac.close();
        }
    }
}
