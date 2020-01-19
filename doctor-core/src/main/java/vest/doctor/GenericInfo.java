package vest.doctor;

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
import java.util.Optional;
import java.util.stream.Collectors;

public class GenericInfo {

    public static Optional<TypeMirror> firstParameterizedType(TypeMirror type) {
        GenericInfo info = new GenericInfo(type);
        if (info.parameterTypes() != null && !info.parameterTypes().isEmpty()) {
            return Optional.of(info.parameterTypes().get(0).type());
        }
        return Optional.empty();
    }

    private TypeMirror type;
    private List<GenericInfo> generics;

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
            return null;
        }

        @Override
        public List<GenericInfo> visitNull(NullType t, Void aVoid) {
            return null;
        }

        @Override
        public List<GenericInfo> visitArray(ArrayType t, Void aVoid) {
            return null;
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
            return null;
        }

        @Override
        public List<GenericInfo> visitTypeVariable(TypeVariable t, Void aVoid) {
            return null;
        }

        @Override
        public List<GenericInfo> visitWildcard(WildcardType t, Void aVoid) {
            return null;
        }

        @Override
        public List<GenericInfo> visitExecutable(ExecutableType t, Void aVoid) {
            return null;
        }

        @Override
        public List<GenericInfo> visitNoType(NoType t, Void aVoid) {
            return null;
        }

        @Override
        public List<GenericInfo> visitUnknown(TypeMirror t, Void aVoid) {
            return null;
        }

        @Override
        public List<GenericInfo> visitUnion(UnionType t, Void aVoid) {
            return null;
        }

        @Override
        public List<GenericInfo> visitIntersection(IntersectionType t, Void aVoid) {
            return null;
        }
    }
}
