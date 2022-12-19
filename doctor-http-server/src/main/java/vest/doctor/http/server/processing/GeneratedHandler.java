package vest.doctor.http.server.processing;

import vest.doctor.ProviderRegistry;
import vest.doctor.http.server.BodyInterchange;
import vest.doctor.http.server.Handler;
import vest.doctor.http.server.impl.Router;

/**
 * Internal use. Interface used for generated http server request handlers to load them with service loader.
 */
public interface GeneratedHandler extends Handler {

    void init(ProviderRegistry providerRegistry, Router router, BodyInterchange bodyInterchange);
}
