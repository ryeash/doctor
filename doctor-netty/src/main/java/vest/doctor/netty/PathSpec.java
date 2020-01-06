package vest.doctor.netty;

import io.netty.handler.codec.http.HttpMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PathSpec implements Comparable<PathSpec> {
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{.*?}");
    private static final Pattern SPLAT_PARAM_PATTERN = Pattern.compile("/\\*");
    private static final String DEFAULT_PARAM_REGEX = "[^/]+?";

    private final HttpMethod method;
    private final String path;
    private final List<String> paramNames;
    private final Pattern pattern;

    public PathSpec(String method, String path) {
        this.method = HttpMethod.valueOf(method);
        if (path.isEmpty()) {
            throw new IllegalArgumentException("the route path may not be empty");
        }
        Objects.requireNonNull(path, "route path uri may not be null");

        this.path = path;
        this.paramNames = new ArrayList<>(3);

        // build the regex pattern uri and extract parameter names
        // pre-process to handle '*'
        // TODO: add ^ to the beginning of the regex
        String temp = SPLAT_PARAM_PATTERN.matcher(path).replaceAll("/{*:.*}");

        StringBuilder builder = new StringBuilder("^");
        int i = 0;
        Matcher matcher = PATH_PARAM_PATTERN.matcher(temp);
        while (matcher.find()) {
            builder.append(temp, i, matcher.start());
            String section = matcher.group().trim();
            String name = section.substring(1, section.length() - 1);
            String paramRegex = DEFAULT_PARAM_REGEX;
            int index = name.indexOf(':', 0);
            if (index >= 0) {
                paramRegex = name.substring(index + 1);
                name = name.substring(0, index);
            }
            paramNames.add(name);
            builder.append('(').append(paramRegex).append(')');
            i = matcher.end();
        }
        builder.append(temp, i, temp.length());

        this.pattern = Pattern.compile(builder.toString());
    }

    public HttpMethod method() {
        return method;
    }

    public String getPath() {
        return path;
    }

    public int paramCount() {
        return paramNames.size();
    }

    public List<String> getParamNames() {
        return paramNames;
    }

    public Pattern getPattern() {
        return pattern;
    }

    public Map<String, String> matchAndCollect(String requestUri) {
        // short circuit: if no parameters were found in the route path, just do a string compare
        if (paramNames.isEmpty()) {
            return requestUri.equals(path) ? Collections.emptyMap() : null;
        }

        Matcher matcher = pattern.matcher(requestUri);
        if (!matcher.matches()) {
            return null;
        }
        Map<String, String> params = new LinkedHashMap<>(matcher.groupCount(), 1.0F);
        for (int i = 1; i <= matcher.groupCount(); i++) {
            String group = matcher.group(i);
            String name = paramNames.get(i - 1);
            params.put(name, group);
        }
        return params;
    }

    @Override
    public String toString() {
        return path + "  pattern:" + pattern + " " + paramNames;
    }

    @Override
    public int compareTo(PathSpec o) {
        // compare specificity of the path
        for (int i = 0; i < path.length() && i < o.path.length(); i++) {
            int ca = path.charAt(i);
            int cb = o.path.charAt(i);
            if (ca != cb) {
                return Integer.compare(charSortValue(ca), charSortValue(cb));
            }
        }

        // if all else fails, just do a length comparison
        return Integer.compare(path.length(), o.path.length());
    }

    private static int charSortValue(int c) {
        switch (c) {
            case '{':
                return 10000;
            case '*':
                return 100000;
            default:
                // default to sorting alphabetically
                return c;
        }
    }
}