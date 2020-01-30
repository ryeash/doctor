package demo.app.dao;

import vest.doctor.Properties;
import vest.doctor.Property;

import javax.inject.Singleton;

@Singleton
@Properties("db.")
public interface DBProps {
    @Property("url")
    String url();

    @Property("username")
    String username();

    @Property("password")
    String password();
}
