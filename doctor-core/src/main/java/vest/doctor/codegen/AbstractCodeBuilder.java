package vest.doctor.codegen;

import javax.lang.model.element.TypeElement;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractCodeBuilder<S extends AbstractCodeBuilder<?>> {
    private static final Pattern MACRO = Pattern.compile("\\$?\\{\\{([^}]+)}}", Pattern.MULTILINE);

    private final List<String> lines;
    private final Map<String, Object> vars;

    protected AbstractCodeBuilder(AbstractCodeBuilder<?> parentBuilder) {
        this.vars = new HashMap<>(parentBuilder.vars);
        this.lines = new LinkedList<>();
    }

    protected AbstractCodeBuilder() {
        this.vars = new HashMap<>();
        bind("providerRegistry", Constants.PROVIDER_REGISTRY);
        bind("shutdownContainer", Constants.SHUTDOWN_CONTAINER_NAME);
        this.lines = new LinkedList<>();
    }

    @SuppressWarnings("unchecked")
    public S line(Object... segments) {
        if (segments != null && segments.length > 0) {
            lines.add(join(segments));
        }
        return (S) this;
    }

    @SuppressWarnings("unchecked")
    public S bind(String name, Object... value) {
        vars.put(name, join(value));
        return (S) this;
    }

    public final Stream<String> allLines() {
        return lines.stream()
                .map(this::fill);
    }

    protected String join(Object... segments) {
        return Stream.of(segments).map(AbstractCodeBuilder::toString).collect(Collectors.joining("")).trim();
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

    protected String fill(String value) {
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
    }

}
