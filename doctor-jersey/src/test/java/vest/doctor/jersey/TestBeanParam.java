package vest.doctor.jersey;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;

public class TestBeanParam {
    String pathParam;
    @QueryParam("queryParam")
    String queryParam;
    @HeaderParam("X-Header")
    String header;
    @CookieParam("_cookie")
    String cookie;
    @Context
    HttpServletRequest request;
    @Attribute("start")
    Long start;
    @Provided
    TestFilter testFilter;

    public String getPathParam() {
        return pathParam;
    }

    @PathParam("pathParam")
    public void setPathParam(String pathParam) {
        this.pathParam = pathParam;
    }

    public String getQueryParam() {
        return queryParam;
    }

    public void setQueryParam(String queryParam) {
        this.queryParam = queryParam;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    public String getCookie() {
        return cookie;
    }

    public void setCookie(String cookie) {
        this.cookie = cookie;
    }

    public HttpServletRequest getRequest() {
        return request;
    }

    public void setRequest(HttpServletRequest request) {
        this.request = request;
    }

    public Long getStart() {
        return start;
    }

    public void setStart(Long start) {
        this.start = start;
    }

    public TestFilter getTestFilter() {
        return testFilter;
    }

    public void setTestFilter(TestFilter testFilter) {
        this.testFilter = testFilter;
    }
}
