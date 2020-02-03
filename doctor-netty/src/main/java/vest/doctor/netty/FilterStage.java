package vest.doctor.netty;

/**
 * Enumerates the different stages of filtering, used in {@link Filter} annotations.
 */
public enum FilterStage {
    /**
     * Indicates the filter should be invoked before a route has been matched.
     */
    BEFORE_MATCH,

    /**
     * Indicates the filter should be invoked before a matched route is invoked.
     */
    BEFORE_ROUTE,

    /**
     * Indicates the filter should be invoked after a matched route is invoked.
     */
    AFTER_ROUTE;

}