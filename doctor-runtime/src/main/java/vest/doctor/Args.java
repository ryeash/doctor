package vest.doctor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable wrapper around the arguments passed into the main method.
 * Options (named arguments that are followed by a value)
 * and Flags (named arguments that are interpreted as boolean true) are supported.
 * <p>
 * Example:
 * java -jar app.jar -Fg --debug -e "dev"
 * F, g, and debug would all be flags (i.e. {@link  #flag(String)} would return true)
 * e is an option (i.e {@link #option(String)} would return "dev" for "e")
 */
public class Args implements Iterable<String> {

    public static final String FLAG = "-";
    public static final String VERBOSE_FLAG = "--";

    private final List<String> args;
    private final Set<String> flags;
    private final Map<String, String> options;

    public Args(String[] args) {
        List<String> temp = Stream.of(args)
                .map(String::trim)
                .flatMap(Args::expandCharFlags)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toCollection(ArrayList::new));
        this.args = Collections.unmodifiableList(Arrays.asList(args));

        this.flags = new HashSet<>();
        this.options = new LinkedHashMap<>();

        for (int i = 0; i < temp.size(); i++) {
            String arg = temp.get(i);

            if (arg.startsWith(FLAG) || arg.startsWith(VERBOSE_FLAG)) {
                String cleaned = stripDashes(arg);
                String optionVal = Optional.of(i + 1)
                        .filter(n -> n < temp.size())
                        .map(temp::get)
                        .map(String::trim)
                        .filter(s -> !s.isEmpty() && !s.startsWith(FLAG))
                        .orElse(null);
                if (optionVal == null) {
                    flags.add(cleaned);
                } else {
                    options.put(cleaned, optionVal);
                }
            }
        }
    }

    /**
     * Get the value of the i'th argument.
     *
     * @param i The index of the argument to get
     * @return The value of i'th argument
     * @throws ArrayIndexOutOfBoundsException if the given i is greater than the number of arguments
     */
    public String get(int i) {
        if (i < 0 || i >= args.size()) {
            throw new ArrayIndexOutOfBoundsException(args.size() + " argument(s) passed in, can not get position " + i + " for: " + args);
        }
        return args.get(i);
    }

    /**
     * Get the value of the first arg (index 0).
     */
    public String first() {
        return get(0);
    }

    /**
     * Get the value of the second arg (index 1).
     */
    public String second() {
        return get(1);
    }

    /**
     * Get the value of the third arg (index 2).
     */
    public String third() {
        return get(2);
    }

    /**
     * Get the value of the fourth arg (index 3).
     */
    public String fourth() {
        return get(3);
    }

    /**
     * Get the value of the fifth arg (index 4).
     */
    public String fifth() {
        return get(4);
    }

    /**
     * Get the total number of arguments passed in.
     */
    public int length() {
        return args.size();
    }


    /**
     * Check if the flag was set, for example: args like "-Aefl", flag('e') =&gt; true.
     *
     * @param c the flag to search for
     * @return true if the flag is present in the arguments, else false
     */
    public boolean flag(char c) {
        return flags.contains(Character.toString(c));
    }

    /**
     * Check if the flag was set, for example: args like "--extra", flag("extra") =&gt; true.
     *
     * @param flag the flag to search for
     * @return true if the flag is present in the arguments, else false
     */
    public boolean flag(String flag) {
        return flags.contains(flag);
    }

    /**
     * Check if the flag was set, using either the short or long name.
     *
     * @param longName  the long name for the flag, ex: "debug"
     * @param shortName the short name for the flag, ex: 'd'
     * @return true if the flag was set
     * @see #flag(String)
     * @see #flag(char)
     */
    public boolean flag(String longName, char shortName) {
        return flag(longName) || flag(shortName);
    }

    /**
     * Get the value of an option with a fallback value if not set.
     *
     * @param c        the short name for the option
     * @param fallback the fallback value to return if the option was not set
     * @return the value of the option, or the fallback value if not set
     * @see #option(String)
     */
    public String option(char c, String fallback) {
        return option(Character.toString(c), fallback);
    }

    /**
     * Get the value of an option with a fallback value if not set.
     *
     * @param name     the name of the option
     * @param fallback the fallback value to return if the option was not set
     * @return the value of the option, or the fallback value if not set
     * @see #option(String)
     */
    public String option(String name, String fallback) {
        return options.getOrDefault(name, fallback);
    }

    /**
     * Get the value of an option;
     * e.g. args like "-e dev", option('e') =&gt; "dev"
     *
     * @param c the short name for the option
     * @return the value of the option, or null if not set
     */
    public String option(char c) {
        return option(Character.toString(c));
    }

    /**
     * Get the value of an option;
     * e.g. args like "--environment dev", option("environment") =&gt; "dev"
     *
     * @param name the name of the option
     * @return the value of the option, or null if not set
     */
    public String option(String name) {
        return options.get(name);
    }

    /**
     * Get the value of an option using either the short or long name.
     *
     * @param longName  the long name for the option, ex: "environment"
     * @param shortName the short name for the option, ex: 'e'
     * @return the value of the option, or null if not set
     * @see #option(String)
     * @see #option(char)
     */
    public String option(String longName, char shortName) {
        return Optional.ofNullable(option(longName)).orElse(option(shortName));
    }

    /**
     * Get the value of an option using either the short or long name, or default to a fallback value.
     *
     * @param longName  the long name for the option, ex: "environment"
     * @param shortName the short name for the option, ex: 'e'
     * @param fallback  the fallback value to return if the option was not set
     * @return the value of the option, or the fallback value if not set
     */
    public String option(String longName, char shortName, String fallback) {
        return Optional.ofNullable(option(longName)).orElse(option(shortName, fallback));
    }

    /**
     * Returns true if the arguments contain the given value.
     */
    public boolean contains(String arg) {
        return args.contains(arg);
    }

    @Override
    public String toString() {
        return args.toString();
    }

    @Override
    public Iterator<String> iterator() {
        return args.iterator();
    }

    private static Stream<String> expandCharFlags(String s) {
        if (s.startsWith(FLAG) && !s.startsWith(VERBOSE_FLAG)) {
            return s.substring(1).chars()
                    .mapToObj(c -> Character.toString((char) c))
                    .map(f -> '-' + f);
        } else {
            return Stream.of(s);
        }
    }

    private static String stripDashes(String s) {
        if (s.startsWith(VERBOSE_FLAG)) {
            return s.substring(2);
        } else if (s.startsWith(FLAG)) {
            return s.substring(1);
        } else {
            return s;
        }
    }
}