package demo.app;

import vest.doctor.aop.Aspects;

import javax.inject.Singleton;
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

    public <T> List<T> parametric(Class<T> type) {
        return Collections.emptyList();
    }
}
