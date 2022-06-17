package vest.doctor.codegen;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.util.LinkedList;
import java.util.List;

public class MethodBuilder extends AbstractCodeBuilder<MethodBuilder> {

    private final ClassBuilder classBuilder;
    private final List<String> annotations = new LinkedList<>();
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

    public MethodBuilder addAnnotation(Class<? extends Annotation> annotation) {
        classBuilder.addImportClass(annotation);
        return addAnnotation("@" + annotation.getSimpleName());
    }

    public MethodBuilder addAnnotation(String annotation) {
        if (!annotation.startsWith("@")) {
            throw new IllegalArgumentException("method annotations must start with @");
        }
        annotations.add(annotation);
        return this;
    }

    void writeTo(PrintWriter out) {
        if (declaration == null || declaration.isEmpty()) {
            throw new IllegalStateException("no declaration has been set for this method");
        }
        for (String annotation : annotations) {
            out.println(annotation);
        }
        out.println(fill(declaration));
        allLines().forEach(out::println);
        out.println("}");
    }
}
