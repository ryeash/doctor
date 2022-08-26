package vest.doctor.conf;

import vest.doctor.runtime.FileLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

import static java.io.StreamTokenizer.TT_EOF;
import static java.io.StreamTokenizer.TT_EOL;

/**
 * A configuration source that reads a structured properties file and builds a map of properties.
 *
 * <p>
 * Structured properties files allow for organizing/name-spacing properties in a less dense, easier to read manner.
 * <p>
 * Definition:
 * Nesting is defined by '{' and '}':
 * <code>
 * <pre>
 * root {
 *  child {
 *   propertyName1 = propertyValue1
 *   propertyName2 = propertyValue2
 *   ... more properties defined ...
 *  }
 * }
 * </pre>
 * </code>
 * This will be parsed as <br>
 * <code>
 * root.child.propertyName1 = propertyValue1<br>
 * root.child.propertyName2 = propertyValue2<br>
 * ...
 * </code>
 * <br/>
 * <br/>
 * Reserved characters:
 * <pre>
 * '{' : used to nest a level deeper in the structure
 * '}' : used to close a nested structure
 * '=', ':' : sets the value of a property, e.g. name = value OR name: value
 * ';' : can be used to signify the end of a line (though it is not necessary)
 * '#' : comments
 * </pre>
 * Quoted strings using either ' or " can be used to escape reserved characters
 * e.g. <pre>name = "value contains { } = : and ;"</pre>
 * Quotes are necessary when interpolating values, i.e. values like: <pre>http://${referenced.property}/</pre>
 */
public class StructuredConfigurationSource implements ConfigurationSource {

    private final FileLocation propertyFile;
    private MapConfigurationSource delegate;

    public StructuredConfigurationSource(String location) {
        this(new FileLocation(location));
    }

    public StructuredConfigurationSource(FileLocation url) {
        this.propertyFile = Objects.requireNonNull(url, "the configuration url can not be null");
        reload();
    }

    @Override
    public String get(String propertyName) {
        return delegate.get(propertyName);
    }

    @Override
    public Collection<String> propertyNames() {
        return delegate.propertyNames();
    }

    @Override
    public void reload() {
        try (Reader reader = new InputStreamReader(propertyFile.openStream(), StandardCharsets.UTF_8)) {
            Map<String, String> config = parse(reader);
            this.delegate = new MapConfigurationSource(config);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public String toString() {
        return "StructuredConfigurationSource{" + propertyFile + "}";
    }

    private Map<String, String> parse(Reader reader) {
        StreamTokenizer tokenizer = newTokenizer(reader);
        Map<String, String> map = new LinkedHashMap<>(128);
        try {
            LinkedList<String> stack = new LinkedList<>();
            String previous = null;
            while (tokenizer.nextToken() != TT_EOF) {
                switch (tokenizer.ttype) {
                    case '{' -> stack.addLast(previous);
                    case '=', ':' -> {
                        stack.addLast(previous);
                        map.put(String.join(ConfigurationFacade.NESTING_DELIMITER + "", stack), readValue(tokenizer));
                        stack.removeLast();
                    }
                    case '}' -> stack.removeLast();
                    case '[', ']' ->
                            throw new UnsupportedOperationException("arrays not supported in structured config: " + propertyFile);
                }
                previous = tokenizer.sval;
            }

            if (!stack.isEmpty()) {
                throw new IllegalStateException("un-matched closing: " + tokenizer);
            }

            return map;
        } catch (IOException e) {
            throw new UncheckedIOException("error reading properties file - line: " + tokenizer.lineno() + " type:" + (char) tokenizer.ttype + " val:" + tokenizer.sval + " toString:" + tokenizer.toString(), e);
        }
    }

    private static StreamTokenizer newTokenizer(Reader reader) {
        StreamTokenizer tokenizer = new StreamTokenizer(reader);

        tokenizer.resetSyntax();
        tokenizer.wordChars(0x21, 0xFF);
        tokenizer.whitespaceChars(0x00, 0x20);

        tokenizer.slashSlashComments(false);
        tokenizer.slashStarComments(false);
        tokenizer.commentChar('#');
        tokenizer.eolIsSignificant(true);
        tokenizer.quoteChar('"');
        tokenizer.quoteChar('\'');
        tokenizer.ordinaryChar('{');
        tokenizer.ordinaryChar('}');
        tokenizer.ordinaryChar('[');
        tokenizer.ordinaryChar(']');
        tokenizer.ordinaryChar(':');
        tokenizer.ordinaryChar('=');
        return tokenizer;
    }

    private static String readValue(StreamTokenizer tokenizer) throws IOException {
        StringBuilder sb = new StringBuilder();
        while (tokenizer.nextToken() != TT_EOL) {
            int t = tokenizer.ttype;
            if (t == TT_EOF || t == ';' || t == '}') {
                break;
            } else if (tokenizer.sval != null) {
                sb.append(tokenizer.sval);
            } else if (t > 0x20) {
                sb.append((char) t);
            }
        }
        return sb.toString().trim();
    }
}