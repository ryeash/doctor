package vest.doctor.processor;

import vest.doctor.processing.CodeProcessingException;
import vest.doctor.processing.ProviderDependency;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DependencyGraph {
    private final Map<ProviderDependency, Set<ProviderDependency>> typesToDependencies = new LinkedHashMap<>();

    public Map<ProviderDependency, Set<ProviderDependency>> getMap() {
        return typesToDependencies;
    }

    public void registerDependency(ProviderDependency target, ProviderDependency dependency) {
        if (target == null) {
            throw new CodeProcessingException("cannot register dependency for null target");
        }
        if (dependency == null) {
            throw new CodeProcessingException("cannot register null dependency for " + target);
        }
        typesToDependencies.computeIfAbsent(target, t -> new HashSet<>()).add(dependency);
        circularDependencyCheck(target, new LinkedHashSet<>());
    }

    private void circularDependencyCheck(ProviderDependency dep, Set<ProviderDependency> chain) {
        if (!chain.add(dep)) {
            String collect = chain.stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(" --> "));
            throw new ClassCircularityError("circular dependency cycle detected through chain: " + collect + " --> " + dep);
        }
        for (ProviderDependency providerDependency : typesToDependencies.getOrDefault(dep, Set.of())) {
            circularDependencyCheck(providerDependency, new LinkedHashSet<>(chain));
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<ProviderDependency, Set<ProviderDependency>> entry : typesToDependencies.entrySet()) {
            ProviderDependency key = entry.getKey();
            appendDeps(key, sb, 0);
        }
        return sb.toString();
    }

    private void appendDeps(ProviderDependency dep, StringBuilder sb, int depth) {
        Set<ProviderDependency> providerDependencies = typesToDependencies.get(dep);
        sb.append('\n');
        if (depth > 0) {
            sb.append(" ".repeat(depth * 2));
        }
        sb.append("- ").append(dep);
        if (providerDependencies != null) {
            for (ProviderDependency providerDependency : providerDependencies) {
                appendDeps(providerDependency, sb, depth + 1);
            }
        }
    }
}

