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
import java.util.List;

public final class GenericTypeVisitor implements TypeVisitor<TypeMirror, Void> {

    @Override
    public TypeMirror visit(TypeMirror t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visit(TypeMirror t) {
        return null;
    }

    @Override
    public TypeMirror visitPrimitive(PrimitiveType t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visitNull(NullType t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visitArray(ArrayType t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visitDeclared(DeclaredType t, Void aVoid) {
        List<? extends TypeMirror> typeArguments = t.getTypeArguments();
        if (!typeArguments.isEmpty()) {
            return typeArguments.get(0);
        }
        return null;
    }

    @Override
    public TypeMirror visitError(ErrorType t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visitTypeVariable(TypeVariable t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visitWildcard(WildcardType t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visitExecutable(ExecutableType t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visitNoType(NoType t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visitUnknown(TypeMirror t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visitUnion(UnionType t, Void aVoid) {
        return null;
    }

    @Override
    public TypeMirror visitIntersection(IntersectionType t, Void aVoid) {
        return null;
    }
}
