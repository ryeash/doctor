package vest.doctor.codegen;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Bindings {
    private static final Pattern MACRO = Pattern.compile("\\$?\\{([^\\s}]+)}", Pattern.MULTILINE);

    public static Bindings create() {
        return new Bindings();
    }

    private final Map<String, Object> vars = new HashMap<>();

    private Bindings() {
    }

    public Bindings var(String name, Object o) {
        vars.put(name, o);
        return this;
    }

    public String fill(String value) {
        try {
            StringBuilder sb = new StringBuilder();
            Matcher matcher = MACRO.matcher(value);
            int pos = 0;
            while (matcher.find()) {
                if (pos != matcher.start()) {
                    sb.append(value, pos, matcher.start());
                }
                if (!matcher.group().startsWith("$")) {
                    String name = matcher.group(1);
                    Object val = vars.get(name);
                    if (val == null) {
                        throw new IllegalArgumentException("missing binding for " + name + ": " + value);
                    }
                    sb.append(fill(String.valueOf(val)));
                } else {
                    sb.append(matcher.group());
                }
                pos = matcher.end();
            }
            sb.append(value, pos, value.length());
            return sb.toString();
        } catch (Throwable t) {
            throw new RuntimeException("error: " + value, t);
        }
    }
}
