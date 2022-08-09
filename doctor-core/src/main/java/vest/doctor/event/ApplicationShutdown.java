package vest.doctor.event;

import vest.doctor.ProviderRegistry;

/**
 * Event sent by the doctor instance when shutdown has started.
 */
public record ApplicationShutdown(ProviderRegistry providerRegistry) {
}
