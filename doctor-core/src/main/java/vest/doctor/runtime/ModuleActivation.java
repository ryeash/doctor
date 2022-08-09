package vest.doctor.runtime;

import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;

import java.util.List;
import java.util.function.BiPredicate;

/**
 * An activation predicate that enables a provider based on the active modules and the modules
 * marked on the provider using {@link vest.doctor.Modules}.
 */
public final class ModuleActivation implements BiPredicate<ProviderRegistry, DoctorProvider<?>> {
    @Override
    public boolean test(ProviderRegistry providerRegistry, DoctorProvider<?> provider) {
        List<String> modules = provider.modules();
        if (modules == null || modules.isEmpty()) {
            return true;
        }
        for (String activeModule : providerRegistry.getActiveModules()) {
            if (modules.contains(activeModule)) {
                return true;
            }
        }
        return false;
    }
}
