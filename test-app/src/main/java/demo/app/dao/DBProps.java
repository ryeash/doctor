package demo.app.dao;

import jakarta.inject.Singleton;
import vest.doctor.Properties;
import vest.doctor.Property;

import java.util.Optional;

@Singleton
@Properties("db.")
public interface DBProps {
    @Property("url")
    String url();

    @Property("username")
    String username();

    @Property("password")
    String password();

    @Property("timeout")
    int timeout();

    @Property("willBeNull")
    Optional<String> willBeNull();
}
