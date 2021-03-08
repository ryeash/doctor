package demo.app;

import jakarta.inject.Inject;
import org.testng.Assert;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.rest.QueryParam;

import java.util.Optional;

public class NettyBeanParam<T> {

    @QueryParam("q")
    private Optional<String> q;
    private int num;

    @Inject
    public NettyBeanParam(@QueryParam("number") int num,
                          Request requestContext) {
        this.num = num;
        Assert.assertNotNull(requestContext);
    }

    public Optional<String> getQ() {
        return q;
    }

    public void setQ(Optional<String> q) {
        this.q = q;
    }

    public int getNum() {
        return num;
    }

    public void setNum(int num) {
        this.num = num;
    }
}
