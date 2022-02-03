package demo.app.reactor;

import jakarta.inject.Singleton;
import vest.doctor.reactor.http.GET;
import vest.doctor.reactor.http.Path;
import vest.doctor.reactor.http.RunOn;

@Singleton
@RunOn("websocketScheduler")
@Path("/isolated")
public class ClassLevelRunAsEndpoint {

    @GET
    @Path("/classLevelRunAs")
    public String get() {
        return Thread.currentThread().getName();
    }
}
