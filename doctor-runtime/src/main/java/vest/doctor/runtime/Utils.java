package vest.doctor.runtime;

import java.util.ArrayList;
import java.util.List;

public final class Utils {

    private Utils() {
    }

    public static List<String> split(String str, char delimiter) {
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
}
