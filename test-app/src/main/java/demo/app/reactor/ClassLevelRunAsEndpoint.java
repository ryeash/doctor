package demo.app.reactor;

import jakarta.inject.Singleton;
import vest.doctor.reactor.http.Endpoint;
import vest.doctor.reactor.http.HttpMethod;
import vest.doctor.reactor.http.RunOn;

@Singleton
@RunOn("websocketScheduler")
@Endpoint("/isolated")
public class ClassLevelRunAsEndpoint {

    @HttpMethod.GET
    @Endpoint("/classLevelRunAs")
    public String get() {
        return Thread.currentThread().getName();
    }
}
