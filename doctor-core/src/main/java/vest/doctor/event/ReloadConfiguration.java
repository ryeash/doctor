package vest.doctor.event;

import vest.doctor.conf.ConfigurationFacade;

/**
 * An event that can be sent to trigger a {@link ConfigurationFacade#reload()}.
 */
public record ReloadConfiguration() {
}
