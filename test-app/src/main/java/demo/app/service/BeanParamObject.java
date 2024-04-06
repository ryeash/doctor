package demo.app.service;

import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;

public class BeanParamObject {

    private final String path;
    @QueryParam("size")
    private Integer size;
    @HeaderParam("X-Param")
    private String param;
    @CookieParam("cook")
    private String cook;

    @Inject
    public BeanParamObject(@PathParam("path") String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
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

    public String getCook() {
        return cook;
    }

    public void setCook(String cook) {
        this.cook = cook;
    }

    @Override
    public String toString() {
        return path + " " + size + " " + param + " " + cook;
    }
}
