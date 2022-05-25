package vest.doctor.http.server.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class PathSpec implements Comparable<PathSpec> {
    private static final Pattern PATH_PARAM_PATTERN = Pattern.compile("\\{.*?}");
    private static final Pattern SPLAT_PARAM_PATTERN = Pattern.compile("/\\*");
    private static final String DEFAULT_PARAM_REGEX = "[^/]+?";

    private final String path;
    private final List<String> paramNames;
    private final Pattern pattern;

    public PathSpec(String path, boolean caseInsensitiveMatch) {
        if (path.isEmpty()) {
            throw new IllegalArgumentException("the route path may not be empty");
        }

        this.path = Objects.requireNonNull(path, "route path uri may not be null");
        this.paramNames = new ArrayList<>(3);

        // pre-process to handle '*'
        String temp = SPLAT_PARAM_PATTERN.matcher(path).replaceAll("/{*:.*}");

        // build the regex pattern uri and extract parameter names
        StringBuilder builder = new StringBuilder("^");
        int i = 0;
        Matcher matcher = PATH_PARAM_PATTERN.matcher(temp);
        while (matcher.find()) {
            builder.append(temp, i, matcher.start());
            String section = matcher.group().trim();
            String[] nameAndRegex = splitNameAndRegex(section);
            String name = nameAndRegex[0].trim();
            if (name.isEmpty()) {
                throw new IllegalArgumentException("the name for a path parameter must not be empty: " + path);
            }
            String paramRegex = nameAndRegex[1].trim();
            paramNames.add(name);
            builder.append('(').append(paramRegex).append(')');
            i = matcher.end();
        }
        builder.append(temp, i, temp.length());
        this.pattern = Pattern.compile(builder.toString(), caseInsensitiveMatch ? Pattern.CASE_INSENSITIVE : 0);
    }

    public String getPath() {
        return path;
    }

    public Pattern getPattern() {
        return pattern;
    }

    /**
     * Match the request uri to this patch spec, returning a non-null map if
     * there is a match.
     *
     * @param requestUri the request uri to match
     * @return a non-null map if the uri matched this spec, else null
     */
    public Map<String, String> matchAndCollect(String requestUri) {
        Matcher matcher = pattern.matcher(requestUri);
        if (!matcher.matches()) {
            return null;
        }
        if (paramNames.isEmpty()) {
            return Collections.emptyMap();
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
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PathSpec pathSpec = (PathSpec) o;
        return Objects.equals(path, pathSpec.path);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path);
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
        return switch (c) {
            case '{' -> 10000;
            case '*' -> 100000;
            // default to sorting alphabetically
            default -> c;
        };
    }

    private static String[] splitNameAndRegex(String section) {
        String trimmed = section.substring(1, section.length() - 1);
        int colon = trimmed.indexOf(':');
        if (colon >= 0) {
            return new String[]{trimmed.substring(0, colon), trimmed.substring(colon + 1)};
        } else {
            return new String[]{trimmed, DEFAULT_PARAM_REGEX};
        }
    }
}