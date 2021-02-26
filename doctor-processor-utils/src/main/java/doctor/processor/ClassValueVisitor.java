package doctor.processor;

import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.AnnotationValueVisitor;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeMirror;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * {@link AnnotationValueVisitor} that extracts the class names from an annotation value.
 */
public final class ClassValueVisitor implements AnnotationValueVisitor<List<String>, Void> {

    public static List<String> getValues(AnnotationValue val) {
        return val.accept(new ClassValueVisitor(), null);
    }

    @Override
    public List<String> visit(AnnotationValue av, Void aVoid) {
        return getValues(av);
    }

    @Override
    public List<String> visit(AnnotationValue av) {
        return getValues(av);
    }

    @Override
    public List<String> visitBoolean(boolean b, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitByte(byte b, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitChar(char c, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitDouble(double d, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitFloat(float f, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitInt(int i, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitLong(long i, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitShort(short s, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitString(String s, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitType(TypeMirror t, Void aVoid) {
        return Collections.singletonList(t.toString());
    }

    @Override
    public List<String> visitEnumConstant(VariableElement c, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitAnnotation(AnnotationMirror a, Void aVoid) {
        return null;
    }

    @Override
    public List<String> visitArray(List<? extends AnnotationValue> vals, Void aVoid) {
        return vals.stream()
                .map(ClassValueVisitor::getValues)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public List<String> visitUnknown(AnnotationValue av, Void aVoid) {
        return null;
    }
}
