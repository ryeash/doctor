package vest.doctor.conf;

import vest.doctor.runtime.FileLocation;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.io.StreamTokenizer.TT_EOF;
import static java.io.StreamTokenizer.TT_EOL;
import static java.io.StreamTokenizer.TT_WORD;

public class StructuredPropertiesParser {

    public static final char NESTING_DELIMITER = '.';

    public enum Type {
        INITIAL,
        FIELD_NAME,
        OPEN_OBJECT,
        CLOSE_OBJECT,
        OPEN_ARRAY,
        CLOSE_ARRAY,
        EQUALS,
        FIELD_VALUE,
        ARRAY_VALUE,
        TERMINAL;

        private static final Map<Type, List<Type>> VALID_TRANSITIONS = new LinkedHashMap<>();

        static {
            for (Type value : Type.values()) {
                switch (value) {
                    case INITIAL, TERMINAL -> VALID_TRANSITIONS.put(value, List.of(Type.values()));
                    case FIELD_NAME -> VALID_TRANSITIONS.put(value, List.of(INITIAL, OPEN_OBJECT, FIELD_VALUE, CLOSE_OBJECT, CLOSE_ARRAY));
                    case OPEN_OBJECT -> VALID_TRANSITIONS.put(value, List.of(FIELD_NAME, OPEN_OBJECT, OPEN_ARRAY, ARRAY_VALUE, CLOSE_OBJECT));
                    case CLOSE_OBJECT -> VALID_TRANSITIONS.put(value, List.of(FIELD_VALUE, CLOSE_ARRAY, CLOSE_OBJECT));
                    case OPEN_ARRAY -> VALID_TRANSITIONS.put(value, List.of(OPEN_ARRAY, ARRAY_VALUE, FIELD_NAME));
                    case CLOSE_ARRAY -> VALID_TRANSITIONS.put(value, List.of(ARRAY_VALUE, CLOSE_ARRAY, CLOSE_OBJECT));
                    case EQUALS -> VALID_TRANSITIONS.put(value, List.of(FIELD_NAME));
                    case FIELD_VALUE -> VALID_TRANSITIONS.put(value, List.of(EQUALS));
                    case ARRAY_VALUE -> VALID_TRANSITIONS.put(value, List.of(OPEN_ARRAY, ARRAY_VALUE));
                }
            }
        }

        public static void validate(Token previous, Token current) {
            if (!VALID_TRANSITIONS.get(current.type).contains(previous.type)) {
                List<Type> valid = VALID_TRANSITIONS.entrySet().stream()
                        .filter(e -> e.getValue().contains(previous.type))
                        .map(Map.Entry::getKey)
                        .filter(t -> t != INITIAL && t != TERMINAL).toList();
                throw new IllegalStateException("invalid state transition " + previous + " to " + current + ", expecting one of: " + valid);
            }
        }

        public boolean anyOf(Type... check) {
            for (Type type : check) {
                if (type == this) {
                    return true;
                }
            }
            return false;
        }
    }

    private final FileLocation fileLocation;

    public StructuredPropertiesParser(FileLocation fileLocation) {
        this.fileLocation = fileLocation;
    }

    @SuppressWarnings({"unchecked"})
    public Map<String, Object> parse() {
        Node root = new Node(null, new LinkedHashMap<>());
        Node reduce = streamTokens().reduce(root, Node::set, (a, b) -> b);
        return expand((Map) reduce.unwrap());
    }

    public Stream<Token> streamTokens() {
        AtomicReference<Token> previous = new AtomicReference<>(new Token(Type.INITIAL, null, null, 0));
        StreamTokenizer tk = newTokenizer(new InputStreamReader(fileLocation.openStream(), StandardCharsets.UTF_8));
        return Stream.generate(() -> {
                    try {
                        tk.nextToken();
                    } catch (IOException t) {
                        throw new UncheckedIOException(t);
                    }
                    return tk;
                })
                .takeWhile(t -> t.ttype != TT_EOF)
                .filter(t -> !(t.ttype == TT_EOL || t.ttype == ','))
                .map(tokenizer -> {
                    int ttype = tokenizer.ttype;
                    String sval = tokenizer.sval;
                    int line = tokenizer.lineno();
                    Token token = switch (ttype) {
                        case '{' -> new Token(Type.OPEN_OBJECT, previous.get().field, null, line);
                        case '}' -> new Token(Type.CLOSE_OBJECT, null, null, line);
                        case '[' -> new Token(Type.OPEN_ARRAY, previous.get().field, null, line);
                        case ']' -> new Token(Type.CLOSE_ARRAY, null, null, line);
                        case '=', ':' -> new Token(Type.EQUALS, previous.get().field, null, line);
                        case '\'', '"', TT_WORD -> word(previous.get(), previous.get().field, sval, line);
                        default -> throw new IllegalStateException("unhandled parse state near line " + line + " unknown token type " + ttype);
                    };
                    Type.validate(previous.get(), token);
                    previous.set(token);
                    return token;
                });
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
        tokenizer.ordinaryChar('[');
        tokenizer.ordinaryChar(']');
        tokenizer.ordinaryChar(',');
        tokenizer.ordinaryChar(':');
        tokenizer.ordinaryChar('=');
        return tokenizer;
    }

    private static Token word(Token previous, String field, String sval, int line) {
        Type previousType = previous.type();
        if (previousType.anyOf(Type.EQUALS, Type.FIELD_NAME)) {
            return new Token(Type.FIELD_VALUE, field, sval, line);
        } else if (previousType.anyOf(Type.INITIAL, Type.FIELD_VALUE, Type.CLOSE_ARRAY, Type.OPEN_OBJECT, Type.CLOSE_OBJECT)) {
            return new Token(Type.FIELD_NAME, sval, sval, line);
        } else if (previousType.anyOf(Type.OPEN_ARRAY, Type.ARRAY_VALUE)) {
            return new Token(Type.ARRAY_VALUE, field, sval, line);
        } else {
            throw new UnsupportedOperationException("this was previous: " + previous);
        }
    }

    private static List<String> split(String str) {
        List<String> split = new LinkedList<>();
        int i = 0;
        int next;
        while ((next = str.indexOf(NESTING_DELIMITER, i)) > 0) {
            split.add(str.substring(i, next));
            i = next + 1;
        }
        split.add(str.substring(i));
        return split;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> expand(Map<String, Object> map) {
        Map<String, Object> clone = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (entry.getKey().contains(".")) {
                Object o = entry.getValue();
                List<String> split = split(entry.getKey());
                Map<String, Object> temp = clone;
                Iterator<String> iterator = split.iterator();
                while (iterator.hasNext()) {
                    String name = iterator.next();
                    if (iterator.hasNext()) {
                        Object current = temp.get(name);
                        if (current == null) {
                            temp.put(name, new LinkedHashMap<>());
                            temp = (Map<String, Object>) temp.get(name);
                        } else if (current instanceof Map m) {
                            temp = m;
                        } else {
                            throw new IllegalStateException("a key can not be a map and a scalar: " + entry.getKey());
                        }
                    } else {
                        temp.put(name, o);
                    }
                }
            } else {
                if (clone.containsKey(entry.getKey())) {
                    throw new IllegalStateException("duplicate key detected: " + entry.getKey());
                }
                clone.put(entry.getKey(), entry.getValue());
            }
        }
        return clone;
    }

    public record Token(Type type, String field, String value, int line) {
    }

    private static final class Node {
        private final Node parent;
        private final Map<String, Node> object;
        private final List<Node> list;
        private final String value;

        private Node(Node parent, Map<String, Node> object) {
            this.parent = parent;
            this.object = object;
            this.list = null;
            this.value = null;
        }

        private Node(Node parent, List<Node> list) {
            this.parent = parent;
            this.object = null;
            this.list = list;
            this.value = null;
        }

        private Node(Node parent, String value) {
            this.parent = parent;
            this.object = null;
            this.list = null;
            this.value = value;
        }

        public boolean isObject() {
            return object != null;
        }

        public boolean isList() {
            return list != null;
        }

        public boolean isValue() {
            return value != null;
        }

        public Node set(Token token) {
            switch (token.type()) {
                case OPEN_OBJECT -> {
                    Node next = new Node(this, new LinkedHashMap<>());
                    if (isList()) {
                        list().add(next);
                    } else if (isObject()) {
                        object().put(token.field(), next);
                    } else {
                        throw new IllegalStateException("invalid token: " + token);
                    }
                    return next;
                }
                case OPEN_ARRAY -> {
                    Node next = new Node(this, new LinkedList<>());
                    if (isList()) {
                        list().add(next);
                    } else if (isObject()) {
                        object().put(token.field(), next);
                    } else {
                        throw new IllegalStateException("invalid token: " + token);
                    }
                    return next;
                }
                case CLOSE_OBJECT, CLOSE_ARRAY -> {
                    return parent();
                }
                case FIELD_VALUE -> {
                    if (!isObject()) {
                        throw new IllegalStateException("invalid properties structure near line " + token.line());
                    }
                    object().put(token.field(), new Node(this, token.value()));
                    return this;
                }
                case ARRAY_VALUE -> {
                    if (!isList()) {
                        throw new IllegalStateException("invalid properties structure near line " + token.line());
                    }
                    list().add(new Node(this, token.value()));
                    return this;
                }
                default -> {
                    return this;
                }
            }
        }

        public Node parent() {
            return parent;
        }

        public Map<String, Node> object() {
            return object;
        }

        public List<Node> list() {
            return list;
        }

        public String value() {
            return value;
        }

        public Object unwrap() {
            if (isObject()) {
                return object().entrySet()
                        .stream()
                        .collect(Collectors.toMap(Map.Entry::getKey, (e) -> e.getValue().unwrap(), (a, b) -> b, LinkedHashMap::new));
            } else if (isList()) {
                return list().stream().map(Node::unwrap).toList();
            } else {
                return value;
            }
        }
    }
}
