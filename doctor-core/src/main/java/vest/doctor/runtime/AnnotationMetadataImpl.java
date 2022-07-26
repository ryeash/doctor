package vest.doctor.runtime;

import vest.doctor.AnnotationData;
import vest.doctor.AnnotationMetadata;

import java.lang.annotation.Annotation;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public record AnnotationMetadataImpl(List<AnnotationData> data) implements AnnotationMetadata {

    public AnnotationMetadataImpl {
        Objects.requireNonNull(data);
    }

    @Override
    public Iterator<AnnotationData> iterator() {
        return data.iterator();
    }

    @Override
    public Stream<AnnotationData> stream() {
        return data.stream();
    }

    @Override
    public Object objectValue(Class<? extends Annotation> type, String attributeName) {
        return findOne(type).map(ad -> ad.objectValue(attributeName)).orElse(null);
    }
}
