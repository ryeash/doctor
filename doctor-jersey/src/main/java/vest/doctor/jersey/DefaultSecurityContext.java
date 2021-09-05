package vest.doctor.jersey;

import jakarta.inject.Provider;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;

public class DefaultSecurityContext implements SecurityContext {
    public static final SecurityContext INSTANCE = new DefaultSecurityContext();

    public static final Provider<SecurityContext> PROVIDER = () -> INSTANCE;

    @Override
    public Principal getUserPrincipal() {
        return null;
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return false;
    }

    @Override
    public String getAuthenticationScheme() {
        return null;
    }
}
