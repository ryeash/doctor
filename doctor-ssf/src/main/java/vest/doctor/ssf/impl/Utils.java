package vest.doctor.ssf.impl;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public class Utils {
    public static final String HTTP_1_1 = "HTTP/1.1";
    public static final String CRLF = "\r\n";
    public static final char SPACE = ' ';
    public static final char COLON = ':';
    public static final byte CR = 13;
    public static final byte LF = 10;
    public static final String CHUNKED = "chunked";
    public static final String CLOSED = "closed";
    public static final String GMT = "GMT";
    public static final String DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

    public static final String ANY = "__ANY__";
    public static final String GET = "GET";
    public static final String POST = "POST";
    public static final String PUT = "PUT";
    public static final String DELETE = "DELETE";

    public static final DateTimeFormatter RFC1123_FORMATTER = DateTimeFormatter.ofPattern(DATE_PATTERN, Locale.US);
    public static final ZoneId GMT_ZONE = ZoneId.of(GMT);
    public static final Charset UTF_8 = StandardCharsets.UTF_8;

    public static String getServerTime() {
        ZonedDateTime time = ZonedDateTime.now(GMT_ZONE);
        return time.format(RFC1123_FORMATTER);
    }

    public static String urlDecode(String source) {
        if (source == null) {
            return null;
        }
        int length = source.length();
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            char ch = source.charAt(i);
            if (ch == '%') {
                if ((i + 2) < length) {
                    char hex1 = source.charAt(i + 1);
                    char hex2 = source.charAt(i + 2);
                    int u = Character.digit(hex1, 16);
                    int l = Character.digit(hex2, 16);
                    if (u == -1 || l == -1) {
                        throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                    }
                    char c = (char) ((u << 4 & 0xF0) + l);
                    sb.append(c);
                    i += 2;
                } else {
                    throw new IllegalArgumentException("Invalid encoded sequence \"" + source.substring(i) + "\"");
                }
            } else if (ch == '+') {
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
