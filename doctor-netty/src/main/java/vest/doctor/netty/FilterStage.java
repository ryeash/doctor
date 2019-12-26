package vest.doctor.netty;

import io.netty.handler.codec.http.HttpMethod;

public enum FilterStage {
    BEFORE_MATCH,
    BEFORE_ROUTE,
    AFTER_ROUTE;

    private HttpMethod methodAlias;

    FilterStage() {
        this.methodAlias = new HttpMethod(this.name());
    }

    public HttpMethod methodAlias() {
        return methodAlias;
    }
}