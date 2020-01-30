package vest.doctor;

import java.util.function.Consumer;

public class MethodBuilder {

    private final StringBuilder sb = new StringBuilder();
    private final Consumer<String> onFinish;

    public MethodBuilder(String methodDefinition) {
        this(methodDefinition, null);
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
        sb.append(Line.line(line, args)).append("\n");
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
