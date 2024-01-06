/* =-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=
 * COPYRIGHT 2014. ALL RIGHTS RESERVED. THIS MODULE CONTAINS
 * TIME WARNER CABLE CONFIDENTIAL AND PROPRIETARY INFORMATION.
 * THE INFORMATION CONTAINED HEREIN IS GOVERNED BY LICENSE AND
 * SHALL NOT BE DISTRIBUTED OR COPIED WITHOUT WRITTEN PERMISSION
 * FROM TIME WARNER CABLE.
 *
 * Author: E124078
 * File: Status.java
 * Created: Nov 5, 2014
 *
 * Description:
 *
 * -------------------------------------------------------------
 * PERFORCE
 *
 * Last Revision: $Change: 378295 $
 * Last Checkin: $DateTime: 2014/12/01 09:21:23 $
 *
 * -=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=- */
package vest.sleipnir.http;


import java.nio.charset.StandardCharsets;

/**
 * Enumerates status codes and their descriptions
 */
public final class Status {
    // 1xx
    public static final Status CONTINUE = new Status(100, "Continue");
    public static final Status SWITCHING_PROTOCOLS = new Status(101, "Switching Protocols");
    public static final Status PROCESSING = new Status(102, "Processing");
    // 2xx
    public static final Status OK = new Status(200, "OK");
    public static final Status CREATED = new Status(201, "Created");
    public static final Status ACCEPTED = new Status(202, "Accepted");
    public static final Status NON_AUTHORITATIVE_INFORMATION = new Status(203, "Non-Authoritative Information");
    public static final Status NO_CONTENT = new Status(204, "No Content");
    public static final Status RESET_CONTENT = new Status(205, "Reset Content");
    public static final Status PARTIAL_CONTENT = new Status(206, "Partial Content");
    public static final Status MULTI_STATUS = new Status(207, "Multi-Status");
    public static final Status ALREADY_REPORTED = new Status(208, "Already Reported");
    public static final Status IM_USED = new Status(226, "IM Used");
    // 3xx
    public static final Status MULTIPLE_CHOICES = new Status(300, "Multiple Choices");
    public static final Status MOVED_PERMANENTLY = new Status(301, "Moved Permanently");
    public static final Status FOUND = new Status(302, "Found");
    public static final Status SEE_OTHER = new Status(303, "See Other");
    public static final Status NOT_MODIFIED = new Status(304, "Not Modified");
    public static final Status USE_PROXY = new Status(305, "Use Proxy");
    public static final Status SWITCH_PROXY = new Status(306, "Switch Proxy");
    public static final Status TEMPORARY_REDIRECT = new Status(307, "Temporary Redirect");
    public static final Status PERMANENT_REDIRECT = new Status(308, "Permanent Redirect");
    // 4xx
    public static final Status BAD_REQUEST = new Status(400, "Bad Request");
    public static final Status UNAUTHORIZED = new Status(401, "Unauthorized");
    public static final Status PAYMENT_REQUIRED = new Status(402, "Payment Required");
    public static final Status FORBIDDEN = new Status(403, "Forbidden");
    public static final Status NOT_FOUND = new Status(404, "Not Found");
    public static final Status METHOD_NOT_ALLOWED = new Status(405, "Method Not Allowed");
    public static final Status NOT_ACCEPTABLE = new Status(406, "Not Acceptable");
    public static final Status PROXY_AUTHENTICATION_REQUIRED = new Status(407, "Proxy Authentication Required");
    public static final Status REQUEST_TIMEOUT = new Status(408, "Request Timeout");
    public static final Status CONFLICT = new Status(409, "Conflict");
    public static final Status GONE = new Status(410, "Gone");
    public static final Status LENGTH_REQUIRED = new Status(411, "Length Required");
    public static final Status PRECONDITION_FAILED = new Status(412, "Precondition Failed");
    public static final Status REQUEST_ENTITY_TOO_LARGE = new Status(413, "Request Entity Too Large");
    public static final Status REQUEST_URI_TOO_LONG = new Status(414, "Request URI Too Long");
    public static final Status UNSUPPORTED_MEDIA_TYPE = new Status(415, "Unsupported Media Type");
    public static final Status REQUESTED_RANGE_NOT_SATISFIABLE = new Status(416, "Requested Range Not Satisfiable");
    public static final Status EXPECTATION_FAILED = new Status(417, "Expectation Failed");
    public static final Status IM_A_TEAPOT = new Status(418, "I'm a teapot");
    public static final Status AUTHENTICATION_TIMEOUT = new Status(419, "Authentication Timeout");
    public static final Status UNPROCESSABLE_ENTITY = new Status(422, "Unprocessable Entity");
    public static final Status LOCKED = new Status(423, "Locked");
    public static final Status FAILED_DEPENDENCY = new Status(424, "Failed Dependency");
    public static final Status UPGRADE_REQUIRED = new Status(426, "Upgrade Required");
    public static final Status PRECONDITION_REQUIRED = new Status(428, "Precondition Required");
    public static final Status TOO_MANY_REQUESTS = new Status(429, "Too Many Requests");
    public static final Status REQUEST_HEADER_FIELDS_TOO_LARGE = new Status(431, "Request Header Fields Too Large");
    // 5xx
    public static final Status INTERNAL_SERVER_ERROR = new Status(500, "Internal Server Error");
    public static final Status NOT_IMPLEMENTED = new Status(501, "Not Implemented");
    public static final Status BAD_GATEWAY = new Status(502, "Bad Gateway");
    public static final Status SERVICE_UNAVAILABLE = new Status(503, "Service Unavailable");
    public static final Status GATEWAY_TIMEOUT = new Status(504, "Gateway Timeout");
    public static final Status HTTP_VERSION_NOT_SUPPORTED = new Status(505, "HTTP Version Not Supported");
    public static final Status VARIANT_ALSO_NEGOTIATES = new Status(506, "Variant Also Negotiates");
    public static final Status INSUFFICIENT_STORAGE = new Status(507, "Insufficient Storage");
    public static final Status LOOP_DETECTED = new Status(508, "Loop Detected");
    public static final Status NOT_EXTENDED = new Status(510, "Not Extended");
    public static final Status NETWORK_AUTHENTICATION_REQUIRED = new Status(511, "Network Authentication Required");

    private final int code;
    private final String message;
    private final byte[] bytes;

    public Status(int code, String message) {
        this.code = code;
        this.message = message;
        this.bytes = (code + " " + message).getBytes(StandardCharsets.UTF_8);
    }

    public int code() {
        return code;
    }

    public String message() {
        return message;
    }

    public byte[] bytes() {
        return bytes;
    }

    @Override
    public String toString() {
        return code + " " + message;
    }
}
