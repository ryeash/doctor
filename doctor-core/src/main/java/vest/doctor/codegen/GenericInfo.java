package vest.doctor.codegen;

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
public class GenericInfo {

    public static Optional<TypeMirror> firstParameterizedType(TypeMirror type) {
        GenericInfo info = new GenericInfo(type);
        if (info.hasTypeParameters()) {
            return Optional.of(info.parameterTypes().get(0).type());
        }
        return Optional.empty();
    }

    private final TypeMirror type;
    private final List<GenericInfo> generics;

    public GenericInfo(TypeMirror type) {
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
            return Collections.singletonList(new GenericInfo(Optional.ofNullable(t.getSuperBound()).orElse(t.getExtendsBound())));
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
