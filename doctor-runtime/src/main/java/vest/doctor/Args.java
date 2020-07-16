package vest.doctor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * An immutable wrapper around the arguments passed into the main method.
 */
public class Args implements Iterable<String> {

    public static final String FLAG = "-";
    public static final String VERBOSE_FLAG = "--";

    private final List<String> args;
    private final Map<String, String> flagValues;

    public Args(String[] args) {
        List<String> temp = Stream.of(args).map(String::trim).filter(s -> !s.isEmpty()).collect(Collectors.toCollection(ArrayList::new));
        this.args = Collections.unmodifiableList(temp);

        Map<String, String> tempValues = new HashMap<>();
        for (int i = 0; i < args.length; i++) {
            String arg = args[i].trim();
            if (arg.startsWith(VERBOSE_FLAG)) {
                int eq = arg.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String name = arg.substring(2, eq).trim();
                String value = arg.substring(eq + 1).trim();
                tempValues.put(name, value);
            } else if (arg.startsWith(FLAG) && (i + 1) < args.length) {
                String lastChar = String.valueOf(arg.charAt(arg.length() - 1));
                String value = args[i + 1];
                if (value.startsWith(FLAG)) {
                    continue;
                }
                tempValues.put(lastChar, value.trim());
            }
        }
        this.flagValues = Collections.unmodifiableMap(tempValues);
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
     * Look for the flag, for example: args like "-e", flag("e") =&gt; true.
     *
     * @param flag The flag to search for
     * @return true if the flag is present in the arguments, else false
     */
    public boolean flag(String flag) {
        for (String arg : args) {
            if (arg.startsWith(FLAG) && !arg.startsWith(VERBOSE_FLAG) && arg.contains(flag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks for the value of a flag, for example: args like "-e dev", flagValue("e") =&gt; "dev"
     *
     * @param flag     The flag to get the value for
     * @param fallback The fallback value to use
     * @return The value of the flag, or the fallback if it's not present
     */
    public String flagValue(String flag, String fallback) {
        return flagValues.getOrDefault(flag, fallback);
    }

    /**
     * Looks for the value of a flag, for example: args like "-e dev", flagValue("e") =&gt; "dev"
     *
     * @param flag The flag to get the value for
     * @return The value of the flag, or null if it's not present
     */
    public String flagValue(String flag) {
        return flagValues.get(flag);
    }

    /**
     * Look for a verbose flag, for example: args like "--debug", verboseFlag("debug") =&gt; true.
     *
     * @param flag The verbose flag name to look for
     * @return true if the flag is present in the arguments, else false
     */
    public boolean verboseFlag(String flag) {
        for (String arg : args) {
            if (arg.startsWith(VERBOSE_FLAG) && Objects.equals(arg.substring(2), flag)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Look for the value of a verbose flag,
     * for example: args like "--properties=myprops.props", verboseFlagValue("properties") =&gt; "myprops.props".
     *
     * @param flag     The flag to get the value for
     * @param fallback The fallback value to use
     * @return The value of the flag, or the fallback if it's not present
     */
    public String verboseFlagValue(String flag, String fallback) {
        return flagValues.getOrDefault(flag, fallback);
    }

    /**
     * Look for the value of a verbose flag,
     * for example: args like "--properties=myprops.props", verboseFlagValue("properties") =&gt; "myprops.props".
     *
     * @param flag The flag to get the value for
     * @return The value of the flag, or null if it's not present
     */
    public String verboseFlagValue(String flag) {
        return flagValues.get(flag);
    }

    /**
     * Get either the flag value or verbose flag value for the given names. Useful when you have
     * the option of using short or verbose names for the same setting.
     *
     * @param flagName     the short name for the flag
     * @param verboseName  the verbose name for the flag
     * @param defaultValue the default value if the flag is unset
     * @return the value of the short name flag, or if null, the value of the verbose flag, if both are null
     * return the default value
     */
    public String anyFlagValue(String flagName, String verboseName, String defaultValue) {
        return Optional.ofNullable(flagValue(flagName))
                .orElse(verboseFlagValue(verboseName, defaultValue));
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
}