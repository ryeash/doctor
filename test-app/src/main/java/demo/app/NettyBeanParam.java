package demo.app;

import vest.doctor.netty.QueryParam;

import javax.inject.Inject;
import java.util.Optional;

public class NettyBeanParam<T> {

    @QueryParam("q")
    private Optional<String> q;
    private int num;

    @Inject
    public NettyBeanParam(@QueryParam("number") int num) {
        this.num = num;
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
