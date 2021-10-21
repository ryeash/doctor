package vest.doctor.http.server;

import vest.doctor.http.server.impl.Router;
import vest.doctor.netty.common.HttpServerConfiguration;

/**
 * Configuration for {@link HttpServer}.
 */
public class DoctorHttpServerConfiguration extends HttpServerConfiguration {

    /**
     * Whether to match routes in {@link Router}
     * using case-insensitive regular expressions.
     */
    private boolean caseInsensitiveMatching = true;

    /**
     * Whether to add debug headers to the response when routing requests.
     */
    private boolean debugRequestRouting = false;

    /**
     * The prefix to prepend to all routes and filters registered with the {@link Router}.
     */
    private String routerPrefix = "";

    public boolean getCaseInsensitiveMatching() {
        return caseInsensitiveMatching;
    }

    public void setCaseInsensitiveMatching(boolean caseInsensitiveMatching) {
        this.caseInsensitiveMatching = caseInsensitiveMatching;
    }

    public boolean isDebugRequestRouting() {
        return debugRequestRouting;
    }

    public void setDebugRequestRouting(boolean debugRequestRouting) {
        this.debugRequestRouting = debugRequestRouting;
    }

    public String getRouterPrefix() {
        return routerPrefix;
    }

    public void setRouterPrefix(String routerPrefix) {
        this.routerPrefix = routerPrefix;
    }
}
