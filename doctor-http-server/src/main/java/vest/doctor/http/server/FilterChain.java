package vest.doctor.http.server;

import vest.doctor.workflow.Workflow;

/**
 * A handle to the next step in the filter chain.
 */
@FunctionalInterface
public interface FilterChain {

    /**
     * Call the next step in the filter chain using the given {@link Request}.
     *
     * @param request the request to continue with
     * @return the future response
     * @throws Exception for any error processing the filter chain
     */
    Workflow<?, Response> next(Request request) throws Exception;
}
