package vest.doctor.netty;

/**
 * Represents a consumer that handles http requests.
 */
@FunctionalInterface
public interface Route {

    /**
     * Accept the RequestContext of a client request.
     *
     * @param requestContext The RequestContext containing HttpRequest and HttpResponse objects
     * @throws Exception if any error occurs during route execution
     */
    void accept(RequestContext requestContext) throws Exception;

    default int priority() {
        return 1000;
    }

}