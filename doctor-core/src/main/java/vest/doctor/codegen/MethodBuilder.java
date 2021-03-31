package vest.doctor.codegen;

import java.io.PrintWriter;

public class MethodBuilder extends AbstractCodeBuilder<MethodBuilder> {

    private final ClassBuilder classBuilder;
    public String declaration;

    MethodBuilder(ClassBuilder classBuilder) {
        super(classBuilder);
        this.classBuilder = classBuilder;
    }

    public MethodBuilder declaration(Object... declaration) {
        this.declaration = join(declaration);
        if (!this.declaration.endsWith("{")) {
            this.declaration = this.declaration + " {";
        }
        return this;
    }

    public MethodBuilder addImportClass(Class<?> type) {
        classBuilder.addImportClass(type);
        return this;
    }

    public MethodBuilder addImportClass(String className) {
        classBuilder.addImportClass(className);
        return this;
    }

    void writeTo(PrintWriter out) {
        if (declaration == null || declaration.isEmpty()) {
            throw new IllegalStateException("no declaration has been set for this method");
        }
        out.println(fill(declaration));
        allLines().forEach(out::println);
        out.println("}");
    }
}
