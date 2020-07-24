package vest.doctor.processor;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class DependencyGraph {

    private final Map<Node, Collection<Node>> nodes = new HashMap<>();

    public void addDependency(String type, String qualifier, String dependencyType, String dependencyQualifier) {
        Collection<Node> node = nodes.computeIfAbsent(new Node(type, qualifier), v -> new HashSet<>());
        node.add(new Node(dependencyType, dependencyQualifier));
        checkCircularDependencies();
    }

    private void checkCircularDependencies() {
        for (Node node : nodes.keySet()) {
            boolean circular = allDeps(node)
                    .anyMatch(node::equals);
            if (circular) {
                throw new IllegalArgumentException("circular dependency found for " + node + " full graph\n" + this);
            }
        }
    }

    private Stream<Node> allDeps(Node n) {
        return nodes.getOrDefault(n, Collections.emptyList())
                .stream()
                .flatMap(d -> Stream.concat(Stream.of(d), allDeps(d)));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        nodes.forEach((node, deps) -> {
            sb.append("- ").append(node).append("\n");
            if (!deps.isEmpty()) {
                String collect = deps.stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining("\n  * ", "  * ", ""));
                sb.append(collect).append('\n');
            }
        });
        return sb.toString();
    }

    private static final class Node {
        private final String type;
        private final String qualifier;

        public Node(String type, String qualifier) {
            this.type = type;
            this.qualifier = qualifier;
        }

        public String getType() {
            return type;
        }


        public String getQualifier() {
            return qualifier;
        }

        @Override
        public String toString() {
            if (qualifier == null) {
                return type;
            } else {
                return type + "(" + qualifier + ")";
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node = (Node) o;
            return Objects.equals(type, node.type) &&
                    Objects.equals(qualifier, node.qualifier);
        }

        @Override
        public int hashCode() {
            return Objects.hash(type, qualifier);
        }
    }
}
