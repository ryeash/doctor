/* =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * COPYRIGHT 2013. ALL RIGHTS RESERVED. THIS MODULE CONTAINS
 * TIME WARNER CABLE CONFIDENTIAL AND PROPRIETARY INFORMATION.
 * THE INFORMATION CONTAINED HEREIN IS GOVERNED BY LICENSE AND
 * SHALL NOT BE DISTRIBUTED OR COPIED WITHOUT WRITTEN PERMISSION
 * FROM TIME WARNER CABLE.
 *
 * Author: E124078
 * File: Headers.java
 * Created: Mar 26, 2014
 *
 * Description:
 *
 * -------------------------------------------------------------
 * PERFORCE
 *
 * Last Revision: $Change: 361059 $
 * Last Checkin: $DateTime: 2014/06/12 07:34:25 $
 *
 * -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- */
package vest.doctor.ssf.impl;

import vest.doctor.ssf.BaseMessage;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.BiConsumer;

public abstract class Headers implements BaseMessage {
    public static final String AUTHORIZATION = "Authorization";
    public static final String CONTENT_LENGTH = "Content-Length";
    public static final String HOST = "Host";
    public static final String SERVER = "Server";
    public static final String DATE = "Date";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String CONTENT_ENCODING = "Content-Encoding";
    public static final String TRANSFER_ENCODING = "Transfer-Encoding";
    public static final String CONNECTION = "Connection";
    public static final String TEXT_PLAIN = "text/plain";
    public static final String APPLICATION_JSON = "application/json";
    public static final String OCTET_STREAM = "application/octet-stream";
    public static final String GZIP = "gzip";
    public static final String DEFLATE = "deflate";

    private final Map<String, List<String>> headers = new HashMap<>();

    public void addHeader(String keyValueHeaderString) {
        String[] header = keyValueHeaderString.split(":", 2);
        if (header.length > 1) {
            addHeader(header[0].trim(), header[1].trim());
        } else if (header.length > 0) {
            addHeader(header[0].trim(), null);
        }
    }

    public void addHeader(String headerName, String headerValue) {
        headers.computeIfAbsent(headerName, v -> new LinkedList<>()).add(headerValue);
    }

    public void setHeader(String headerName, String headerValue) {
        removeHeader(headerName);
        addHeader(headerName, headerValue);
    }

    public void removeHeader(String headerName) {
        headers.remove(headerName);
    }

    public String getHeader(String headerName) {
        return headers.getOrDefault(headerName, List.of())
                .stream()
                .findFirst()
                .orElse(null);
    }

    public List<String> getHeaders(String headerName) {
        return headers.getOrDefault(headerName, List.of());
    }

    public void eachHeader(BiConsumer<String, String> consumer) {
        for (Entry<String, List<String>> e : headers.entrySet()) {
            for (String s : e.getValue()) {
                consumer.accept(e.getKey(), s);
            }
        }
    }

    @Override
    public Collection<String> headerNames() {
        return headers.keySet();
    }

    @Override
    public int numHeaders() {
        return headers.size();
    }

    @Override
    public String toString() {
        return headers.toString();
    }
}
