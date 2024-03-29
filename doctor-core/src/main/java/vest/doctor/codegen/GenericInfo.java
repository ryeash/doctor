package vest.doctor.codegen;

import vest.doctor.processing.AnnotationProcessorContext;

import javax.lang.model.AnnotatedConstruct;
import javax.lang.model.element.Element;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ErrorType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.IntersectionType;
import javax.lang.model.type.NoType;
import javax.lang.model.type.NullType;
import javax.lang.model.type.PrimitiveType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.type.TypeVariable;
import javax.lang.model.type.TypeVisitor;
import javax.lang.model.type.UnionType;
import javax.lang.model.type.WildcardType;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Utility class used during annotation processing.
 */
public final class GenericInfo {

    public static Optional<TypeMirror> firstParameterizedType(TypeMirror type) {
        GenericInfo info = new GenericInfo(type);
        if (info.hasTypeParameters()) {
            return Optional.of(info.parameterTypes().get(0).type());
        }
        return Optional.empty();
    }

    private final AnnotatedConstruct element;
    private final TypeMirror type;
    private final List<GenericInfo> generics;

    public GenericInfo(Element element) {
        this.element = element;
        this.type = element.asType();
        this.generics = Collections.unmodifiableList(type.accept(new GenericInfoVisitor(), null));
    }

    public GenericInfo(TypeMirror type) {
        this.element = null;
        this.type = type;
        this.generics = Collections.unmodifiableList(type.accept(new GenericInfoVisitor(), null));
    }

    public GenericInfo(AnnotatedConstruct element, TypeMirror type) {
        this.element = element;
        this.type = type;
        this.generics = Collections.unmodifiableList(type.accept(new GenericInfoVisitor(), null));
    }

    public TypeMirror type() {
        return type;
    }

    public List<GenericInfo> parameterTypes() {
        return generics;
    }

    public boolean hasTypeParameters() {
        return generics != null && !generics.isEmpty();
    }

    public String newTypeInfo(AnnotationProcessorContext context) {
        StringBuilder sb = new StringBuilder("new TypeInfo(");
        if (type.getKind().isPrimitive()) {
            sb.append(ProcessorUtils.rawPrimitiveClass(type));
        } else {
            TypeMirror reportedType = ProcessorUtils.rawClass(type);
            if (reportedType == null) {
                sb.append("Object.class");
            } else {
                sb.append(ProcessorUtils.typeWithoutParameters(reportedType)).append(".class");
            }
        }

        if (element != null) {
            String newAnnotationMetadata = ProcessorUtils.writeNewAnnotationMetadata(context, element);
            sb.append(",").append(newAnnotationMetadata);
        }

        if (!hasTypeParameters()) {
            sb.append(")");
        } else {
            String param = parameterTypes()
                    .stream()
                    .map(gi -> gi.newTypeInfo(context))
                    .collect(Collectors.joining(", "));
            sb.append(", ").append(param).append(")");
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return type.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        GenericInfo that = (GenericInfo) o;
        return Objects.equals(type, that.type) &&
               Objects.equals(generics, that.generics);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, generics);
    }

    private static final class GenericInfoVisitor implements TypeVisitor<List<GenericInfo>, Void> {

        @Override
        public List<GenericInfo> visit(TypeMirror t, Void aVoid) {
            return null;
        }

        @Override
        public List<GenericInfo> visit(TypeMirror t) {
            return null;
        }

        @Override
        public List<GenericInfo> visitPrimitive(PrimitiveType t, Void aVoid) {
            return Collections.emptyList();
        }

        @Override
        public List<GenericInfo> visitNull(NullType t, Void aVoid) {
            return Collections.emptyList();
        }

        @Override
        public List<GenericInfo> visitArray(ArrayType t, Void aVoid) {
            return Collections.emptyList();
        }

        @Override
        public List<GenericInfo> visitDeclared(DeclaredType t, Void aVoid) {
            return t.getTypeArguments()
                    .stream()
                    .map(GenericInfo::new)
                    .collect(Collectors.toList());
        }

        @Override
        public List<GenericInfo> visitError(ErrorType t, Void aVoid) {
            return Collections.emptyList();
        }

        @Override
        public List<GenericInfo> visitTypeVariable(TypeVariable t, Void aVoid) {
            TypeMirror upperBound = t.getUpperBound();
            if (upperBound instanceof IntersectionType) {
                TypeMirror typeMirror = ((IntersectionType) upperBound).getBounds().get(0);
                return Collections.singletonList(new GenericInfo(typeMirror));
            } else {
                return Collections.singletonList(new GenericInfo(upperBound));
            }
        }

        @Override
        public List<GenericInfo> visitWildcard(WildcardType t, Void aVoid) {
            if (t.getSuperBound() != null) {
                return Collections.singletonList(new GenericInfo(t.getSuperBound()));
            } else if (t.getExtendsBound() != null) {
                return Collections.singletonList(new GenericInfo(t.getExtendsBound()));
            } else {
                return Collections.emptyList();
            }
        }

        @Override
        public List<GenericInfo> visitExecutable(ExecutableType t, Void aVoid) {
            return Collections.emptyList();
        }

        @Override
        public List<GenericInfo> visitNoType(NoType t, Void aVoid) {
            return Collections.emptyList();
        }

        @Override
        public List<GenericInfo> visitUnknown(TypeMirror t, Void aVoid) {
            return Collections.emptyList();
        }

        @Override
        public List<GenericInfo> visitUnion(UnionType t, Void aVoid) {
            return Collections.emptyList();
        }

        @Override
        public List<GenericInfo> visitIntersection(IntersectionType t, Void aVoid) {
            return Collections.emptyList();
        }
    }
}
