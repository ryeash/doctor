package vest.doctor.apache.server;

import java.util.function.Predicate;

@FunctionalInterface
public interface RoutePredicate extends Predicate<RequestContext>, Comparable<RoutePredicate> {
    boolean test(RequestContext requestContext);

    @Override
    default int compareTo(RoutePredicate o) {
        return 0;
    }
}
