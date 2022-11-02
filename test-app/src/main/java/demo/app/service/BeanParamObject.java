package demo.app.service;

import jakarta.inject.Inject;
import vest.doctor.http.server.Param.Context;
import vest.doctor.http.server.Request;
import vest.doctor.http.server.Response;

import java.util.Optional;

import static vest.doctor.http.server.Param.Cookie;
import static vest.doctor.http.server.Param.Header;
import static vest.doctor.http.server.Param.Path;
import static vest.doctor.http.server.Param.Query;

public class BeanParamObject {

    private final Request request;
    private final Response response;
    private final String path;
    @Query
    private Integer size;
    @Header("X-Param")
    private String param;
    @Cookie
    private Optional<String> cook;

    @Inject
    public BeanParamObject(@Context Request request,
                           @Context Response response,
                           @Path String path) {
        this.request = request;
        this.response = response;
        this.path = path;
    }

    public Request getRequest() {
        return request;
    }

    public Response getResponse() {
        return response;
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
