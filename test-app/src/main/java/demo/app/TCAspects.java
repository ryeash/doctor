package demo.app;

import vest.doctor.aop.Aspects;

import javax.inject.Singleton;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Singleton
@Aspects(LoggingAspect.class)
public class TCAspects {

    @Aspects(TimingAspect.class)
    public void execute(String name, List<String> values) {
        System.out.println("execute " + name + " " + values);
    }

    @Aspects(StringModificationAspect.class)
    public String parrot(String name) {
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

    public <T extends String & CharSequence, R extends InputStream> List<T> parametric(Class<T> type) {
        return Collections.emptyList();
    }

    private void privateMethodShouldntBeAProblem() {
        System.out.println("private method");
    }
}
