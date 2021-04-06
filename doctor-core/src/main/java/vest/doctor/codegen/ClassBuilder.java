package vest.doctor.codegen;

import javax.annotation.processing.Filer;
import javax.tools.JavaFileObject;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

public class ClassBuilder extends AbstractCodeBuilder<ClassBuilder> {

    private String packageName = "";
    private String className;
    private String fullyQualifiedClassName;
    private String extendsClass;
    private Set<String> implementsInterfaces;
    private Set<String> importClasses;
    private Set<String> classAnnotations;
    private List<String> fields;
    private List<MethodBuilder> methods;
    private List<ClassBuilder> nestedClasses;

    public ClassBuilder() {
        super();
    }

    public ClassBuilder setPackage(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public ClassBuilder setClassName(Object... className) {
        this.fullyQualifiedClassName = join(className);
        int i = fullyQualifiedClassName.lastIndexOf(".");
        if (i < 0) {
            this.className = fullyQualifiedClassName;
        } else {
            this.packageName = fullyQualifiedClassName.substring(0, i);
            this.className = fullyQualifiedClassName.substring(i + 1);
        }
        return this;
    }

    public String getFullyQualifiedClassName() {
        return fullyQualifiedClassName;
    }

    public ClassBuilder setExtendsClass(Class<?> type) {
        addImportClass(type);
        this.extendsClass = type.getSimpleName();
        return this;
    }

    public ClassBuilder setExtendsClass(String extendsClass) {
        this.extendsClass = extendsClass;
        return this;
    }

    public ClassBuilder addImplementsInterface(Class<?> interfaceType) {
        if (!interfaceType.isInterface()) {
            throw new IllegalArgumentException("not an interface: " + interfaceType);
        }
        return addImplementsInterface(interfaceType.getSimpleName()).
                addImportClass(interfaceType);
    }

    public ClassBuilder addImplementsInterface(String interfaceType) {
        if (implementsInterfaces == null) {
            implementsInterfaces = new LinkedHashSet<>();
        }
        implementsInterfaces.add(interfaceType);
        return this;
    }

    public ClassBuilder addImportClass(Class<?> type) {
        return addImportClass(type.getCanonicalName());
    }

    public ClassBuilder addImportClass(Object... type) {
        if (importClasses == null) {
            importClasses = new LinkedHashSet<>();
        }
        importClasses.add(join(type));
        return this;
    }

    public ClassBuilder addClassAnnotation(Object... annotation) {
        if (classAnnotations == null) {
            this.classAnnotations = new LinkedHashSet<>();
        }
        this.classAnnotations.add(join(annotation));
        return this;
    }

    public ClassBuilder addField(Object... field) {
        if (fields == null) {
            fields = new LinkedList<>();
        }
        fields.add(join(field));
        return this;
    }

    public MethodBuilder newMethod() {
        if (methods == null) {
            methods = new LinkedList<>();
        }
        MethodBuilder methodBuilder = new MethodBuilder(this);
        this.methods.add(methodBuilder);
        return methodBuilder;
    }

    public MethodBuilder newMethod(Object... declaration) {
        return newMethod().declaration(declaration);
    }

    public ClassBuilder addMethod(String declaration, Consumer<MethodBuilder> builder) {
        builder.accept(newMethod(declaration));
        return this;
    }

    public ClassBuilder addNestedClass(ClassBuilder nested) {
        if (nestedClasses == null) {
            nestedClasses = new LinkedList<>();
        }
        nestedClasses.add(nested);
        return this;
    }

    public void writeClass(Filer filer) {
        try {
            JavaFileObject builderFile = filer.createSourceFile(fullyQualifiedClassName);
            try (PrintWriter out = new PrintWriter(builderFile.openWriter())) {

                out.print("package ");
                out.print(packageName);
                out.println(";");
                out.println();

                if (nestedClasses != null) {
                    for (ClassBuilder nestedClass : nestedClasses) {
                        for (String importClass : nestedClass.importClasses) {
                            addImportClass(importClass);
                        }
                    }
                }
                if (importClasses != null) {
                    for (String importClass : new LinkedHashSet<>(importClasses)) {
                        out.println("import " + importClass + ";");
                    }
                }

                if (classAnnotations != null) {
                    for (String classAnnotation : classAnnotations) {
                        out.println(classAnnotation);
                    }
                }

                // @Generated was removed from standard java as part of jigsaw
//                out.print("@" + Generated.class.getCanonicalName() + "(value = \"vest.doctor\", date = \"" + new Date() + "\")\n");
                out.print("public class ");
                out.print(className);
                if (extendsClass != null && !extendsClass.isEmpty()) {
                    out.print(" extends " + extendsClass);
                }

                if (implementsInterfaces != null && !implementsInterfaces.isEmpty()) {
                    String interfaces = String.join(", ", implementsInterfaces);
                    out.print(" implements " + interfaces);
                }
                out.println("{");
                writeClassBody(this, out);
                out.println("}");
            }
        } catch (IOException e) {
            throw new UncheckedIOException("error writing class " + className, e);
        }
    }

    private static void writeClassBody(ClassBuilder builder, PrintWriter out) {
        if (builder.fields != null && !builder.fields.isEmpty()) {
            for (String field : builder.fields) {
                out.println(builder.fill(field) + ';');
            }
        }

        if (builder.methods != null && !builder.methods.isEmpty()) {
            for (MethodBuilder method : builder.methods) {
                method.writeTo(out);
                out.println();
            }
        }

        if (builder.nestedClasses != null) {
            for (ClassBuilder nestedClass : builder.nestedClasses) {
                writeNestedClass(nestedClass, out);
            }
        }
    }

    private static void writeNestedClass(ClassBuilder nested, PrintWriter out) {
        out.print("public static final class ");
        out.print(nested.className);
        if (nested.extendsClass != null && !nested.extendsClass.isEmpty()) {
            out.print(" extends " + nested.extendsClass);
        }

        if (nested.implementsInterfaces != null && !nested.implementsInterfaces.isEmpty()) {
            String interfaces = String.join(", ", nested.implementsInterfaces);
            out.print(" implements " + interfaces);
        }
        out.println("{");
        writeClassBody(nested, out);
        out.println("}");
    }

}
