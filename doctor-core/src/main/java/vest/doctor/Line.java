package vest.doctor;

import javax.lang.model.element.TypeElement;
import java.util.Arrays;
import java.util.Iterator;

public final class Line {
    private Line() {
    }

    public static String line(String template, Object... vals) {
        if (vals == null || vals.length == 0) {
            return template;
        }
        StringBuilder sb = new StringBuilder();
        Iterator<Object> iterator = Arrays.asList(vals).iterator();
        int i = 0;
        int next;
        while (iterator.hasNext() && (next = template.indexOf("{}", i)) >= 0) {
            String replaceValue = toString(iterator.next());
            sb.append(template, i, next);
            sb.append(replaceValue);
            i = next + 2;
        }
        sb.append(template.substring(i));
        return sb.toString();
    }

    private static String toString(Object o) {
        if (o instanceof Class) {
            return ((Class<?>) o).getSimpleName();
        } else if (o instanceof TypeElement) {
            return ((TypeElement) o).getQualifiedName().toString();
        } else {
            return String.valueOf(o);
        }
    }
}
