package vest.doctor.apache.server;

import org.apache.hc.core5.http.HttpException;

import java.io.IOException;

@FunctionalInterface
public interface Handler {

    void handle(RequestContext ctx) throws HttpException, IOException;
}
