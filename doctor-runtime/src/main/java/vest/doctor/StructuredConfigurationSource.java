package vest.doctor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Objects;

import static java.io.StreamTokenizer.TT_EOF;
import static java.io.StreamTokenizer.TT_EOL;

public class StructuredConfigurationSource implements ConfigurationSource {

    private final URL propertyFile;
    private final String levelDelimiter;
    private Map<String, String> properties;

    public StructuredConfigurationSource(URL url) {
        this(url, ".");
    }

    public StructuredConfigurationSource(URL url, String levelDelimiter) {
        this.propertyFile = Objects.requireNonNull(url, "the configuration url can not be null");
        this.levelDelimiter = levelDelimiter;
        reload();
    }

    @Override
    public String get(String propertyName) {
        return properties.get(propertyName);
    }

    @Override
    public Iterable<String> propertyNames() {
        return properties.keySet();
    }

    @Override
    public void reload() {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(propertyFile.openStream(), StandardCharsets.UTF_8))) {
            this.properties = parseStructuredPropertiesFile(reader, levelDelimiter);
        } catch (IOException e) {
            throw new UncheckedIOException("Error reading structured properties file", e);
        }
    }

    @Override
    public String toString() {
        return "StructuredConfiguration(" + propertyFile + ")";
    }

    public static Map<String, String> parseStructuredPropertiesFile(Reader reader, String levelDelimiter) {
        StreamTokenizer tokenizer = newTokenizer(reader);
        Map<String, String> map = new LinkedHashMap<>(128);
        try {
            LinkedList<String> stack = new LinkedList<>();
            String previous = null;
            while (tokenizer.nextToken() != TT_EOF) {
                switch (tokenizer.ttype) {
                    case '{':
                        stack.addLast(previous);
                        break;
                    case '=':
                    case ':':
                        stack.addLast(previous);
                        map.put(String.join(levelDelimiter, stack), readValue(tokenizer));
                        stack.removeLast();
                        break;
                    case '}':
                        stack.removeLast();
                        break;
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

        tokenizer.slashSlashComments(true);
        tokenizer.slashStarComments(true);
        tokenizer.commentChar('#');
        tokenizer.eolIsSignificant(true);
        tokenizer.quoteChar('"');
        tokenizer.quoteChar('\'');
        tokenizer.ordinaryChar('{');
        tokenizer.ordinaryChar('}');
        tokenizer.ordinaryChar('=');
        tokenizer.ordinaryChar(';');
        tokenizer.ordinaryChar(':');
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