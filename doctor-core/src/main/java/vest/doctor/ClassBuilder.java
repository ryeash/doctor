package vest.doctor;

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

/**
 * Helper class used internally to create generated classes.
 */
public class ClassBuilder {

    private String packageName = "";
    private String className;
    private String fullyQualifiedClassName;
    private String extendsClass;
    private Set<String> implementsInterfaces;
    private Set<String> importClasses;
    private Set<String> classAnnotations;
    private List<String> fields;
    private String constructor;
    private List<String> methods;
    private List<ClassBuilder> nestedClasses;

    public ClassBuilder setPackage(String packageName) {
        this.packageName = packageName;
        return this;
    }

    public ClassBuilder setClassName(String fullyQualifiedClassName) {
        this.fullyQualifiedClassName = fullyQualifiedClassName;
        int i = fullyQualifiedClassName.lastIndexOf(".");
        if (i < 0) {
            this.className = fullyQualifiedClassName;
        } else {
            this.packageName = fullyQualifiedClassName.substring(0, i);
            this.className = fullyQualifiedClassName.substring(i + 1);
        }
        return this;
    }

    public ClassBuilder setExtendsClass(Class<?> type) {
        addImportClass(type);
        this.extendsClass = type.getSimpleName();
        this.packageName = type.getPackage().toString();
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

    public ClassBuilder addImportClass(String className) {
        if (importClasses == null) {
            importClasses = new LinkedHashSet<>();
        }
        importClasses.add(className);
        return this;
    }

    public ClassBuilder addClassAnnotation(String annotation) {
        if (classAnnotations == null) {
            this.classAnnotations = new LinkedHashSet<>();
        }
        this.classAnnotations.add(annotation);
        return this;
    }

    public ClassBuilder addField(String field) {
        if (fields == null) {
            fields = new LinkedList<>();
        }
        fields.add(field);
        return this;
    }

    public ClassBuilder setConstructor(String definition, Consumer<MethodBuilder> builder) {
        MethodBuilder methodBuilder = new MethodBuilder(definition, this::setConstructor);
        builder.accept(methodBuilder);
        methodBuilder.finish();
        return this;
    }

    public ClassBuilder setConstructor(String constructor) {
        this.constructor = constructor;
        return this;
    }

    public ClassBuilder addMethod(String method) {
        if (methods == null) {
            methods = new LinkedList<>();
        }
        methods.add(method);
        return this;
    }

    public ClassBuilder addMethod(String definition, Consumer<MethodBuilder> builder) {
        MethodBuilder methodBuilder = new MethodBuilder(definition, this::addMethod);
        builder.accept(methodBuilder);
        methodBuilder.finish();
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
                    for (String importClass : importClasses) {
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

    private static void writeClassBody(ClassBuilder builder, PrintWriter out) {
        if (builder.fields != null && !builder.fields.isEmpty()) {
            for (String field : builder.fields) {
                out.println(field + ';');
            }
        }

        if (builder.constructor != null) {
            out.println(builder.constructor);
            out.println();
        }

        if (builder.methods != null && !builder.methods.isEmpty()) {
            for (String method : builder.methods) {
                out.println(method);
                out.println();
            }
        }

        if (builder.nestedClasses != null) {
            for (ClassBuilder nestedClass : builder.nestedClasses) {
                writeNestedClass(nestedClass, out);
            }
        }
    }
}
