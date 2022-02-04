package demo.app.reactor;

import jakarta.inject.Inject;
import vest.doctor.reactor.http.Param;

import java.util.Optional;

import static vest.doctor.reactor.http.Param.Cookie;
import static vest.doctor.reactor.http.Param.Header;
import static vest.doctor.reactor.http.Param.Query;

public class BeanParamObject {

    private String path;
    @Query
    private Integer size;
    @Header("X-Param")
    private String param;
    @Cookie
    private Optional<String> cook;

    @Inject
    public BeanParamObject(@Param.Path String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getSize() {
        return size;
    }

    public void setSize(Integer size) {
        this.size = size;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public Optional<String> getCook() {
        return cook;
    }

    public void setCook(Optional<String> cook) {
        this.cook = cook;
    }

    @Override
    public String toString() {
        return path + " " + size + " " + param + " " + cook.get();
    }
}
