package vest.doctor.apache.server.impl;

import vest.doctor.apache.server.RequestContext;
import vest.doctor.apache.server.RoutePredicate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MultiPredicate implements RoutePredicate {
    private final List<RoutePredicate> predicates;

    public MultiPredicate(RoutePredicate... predicates) {
        this(Arrays.asList(predicates));
    }

    public MultiPredicate(List<RoutePredicate> predicates) {
        this.predicates = new ArrayList<>(Objects.requireNonNull(predicates));
        if (predicates.isEmpty()) {
            throw new IllegalArgumentException("predicate list may not be empty");
        }
    }

    @Override
    public boolean test(RequestContext requestContext) {
        for (RoutePredicate predicate : predicates) {
            if (!predicate.test(requestContext)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int compareTo(RoutePredicate o) {
        for (RoutePredicate predicate : predicates) {
            int c = predicate.compareTo(o);
            if (c != 0) {
                return c;
            }
        }
        return 0;
    }
}
