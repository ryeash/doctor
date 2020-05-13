package vest.doctor;

import java.util.function.Consumer;

/**
 * Helper class used internally to create generated methods.
 */
public class MethodBuilder {

    private final StringBuilder sb = new StringBuilder();
    private final Consumer<String> onFinish;

    public MethodBuilder(String methodDefinition, Object... args) {
        this(CodeLine.line(methodDefinition, args), (Consumer<String>) null);
    }

    public MethodBuilder(String methodDefinition, Consumer<String> onFinish) {
        sb.append(methodDefinition).append("{\n");
        this.onFinish = onFinish;
    }

    public MethodBuilder line(String line) {
        sb.append(line).append("\n");
        return this;
    }

    public MethodBuilder line(String line, Object... args) {
        sb.append(CodeLine.line(line, args)).append("\n");
        return this;
    }

    public String finish() {
        sb.append("}");
        String s = sb.toString();
        if (onFinish != null) {
            onFinish.accept(s);
        }
        return s;
    }
}
