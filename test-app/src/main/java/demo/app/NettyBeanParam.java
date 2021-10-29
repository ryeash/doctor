package demo.app;

import jakarta.inject.Inject;
import org.testng.Assert;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.rest.Param;

import java.util.Optional;

import static vest.doctor.http.server.rest.Param.Type.Query;

public class NettyBeanParam<T> {

    @Param(type = Query, name = "q")
    private Optional<String> q;
    private int num;
    private int numberViaMethod;

    @Inject
    public NettyBeanParam(@Param(type = Query, name = "number") int num,
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

    public int getNumberViaMethod() {
        return numberViaMethod;
    }

    @Param(type = Query, name = "number")
    public NettyBeanParam<T> setNumberViaMethod(int numberViaMethod) {
        this.numberViaMethod = numberViaMethod;
        return this;
    }
}
