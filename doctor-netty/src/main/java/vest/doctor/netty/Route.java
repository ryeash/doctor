package vest.doctor.netty;

import vest.doctor.BeanProvider;

public interface Route {

    void init(BeanProvider beanProvider);

    void accept(RequestContext requestContext) throws Exception;

}