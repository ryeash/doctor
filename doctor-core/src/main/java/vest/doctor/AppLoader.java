package vest.doctor;

import java.util.List;

public interface AppLoader extends Prioritized, AutoCloseable {

    default void preProcess(BeanProvider beanProvider) {
        // no-op
    }

    default void load(BeanProvider beanProvider) {
        // no-op
    }

    default void postProcess(BeanProvider beanProvider) {
        // no-op
    }

    default boolean isActive(BeanProvider beanProvider, List<String> modules) {
        if (modules == null || modules.isEmpty()) {
            return true;
        }
        for (String activeModule : beanProvider.getActiveModules()) {
            if (modules.contains(activeModule)) {
                return true;
            }
        }
        return false;
    }
}
