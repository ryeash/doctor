package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.aop.Aspects;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Singleton
@Aspects(LoggingAspect.class)
public class TCAspects {

    private final boolean dummy = true;

    @Aspects(TimingAspect.class)
    public void execute(String name, List<String> values) {
        System.out.println("execute " + name + " " + values);
    }

    @Aspects(StringModificationAspect.class)
    public String parrot(@ParameterAnnotation("toast") String name) throws IOException {
        return name;
    }

    @Aspects(StringModificationAspect.class)
    public String parrot2(String name) {
        return name;
    }

    @Aspects(StringModificationAspect.class)
    public String parrot3(String name) {
        return name;
    }

    public <T extends String & CharSequence, R extends InputStream> List<T> parametric(Class<T> type, boolean[] booleans) {
        return Collections.emptyList();
    }

    private void privateMethodShouldntBeAProblem() {
        System.out.println("private method");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TCAspects tcAspects = (TCAspects) o;
        return dummy == tcAspects.dummy;
    }

    @Override
    public int hashCode() {
        return Objects.hash(dummy);
    }
}
