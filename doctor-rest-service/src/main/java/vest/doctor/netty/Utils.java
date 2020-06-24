package vest.doctor.netty;

import java.io.File;
import java.util.Iterator;

public class Utils {

    public static final String TRACE_ATTR = "doctor.netty.http.tracer";

    private static final String TEMPLATE_MACRO = "{}";

    public static String squeeze(String s, char c) {
        if (s == null || s.isEmpty()) {
            return s;
        }
        if (s.length() == 1 && s.charAt(0) == c) {
            return s;
        }
        char[] a = s.toCharArray();
        int n = 1;
        for (int i = 1; i < a.length; i++) {
            a[n] = a[i];
            if (a[n] != c) {
                n++;
            } else if (a[n - 1] != c) {
                n++;
            }
        }
        return new String(a, 0, n);
    }

    public static String fillTemplate(String template, Iterable<Object> values) {
        StringBuilder sb = new StringBuilder();
        Iterator<Object> iterator = values.iterator();
        int i = 0;
        int next;
        while (iterator.hasNext() && (next = template.indexOf(TEMPLATE_MACRO, i)) >= 0) {
            String replaceValue = String.valueOf(iterator.next());
            sb.append(template, i, next);
            sb.append(replaceValue);
            i = next + 2;
        }
        sb.append(template.substring(i));
        return sb.toString();
    }

//    public static void addTraceInfo(boolean enabled, RequestContext ctx, String message, Object... args) {
//        if (enabled) {
//            if (ctx.attribute(TRACE_ATTR) == null) {
//                ctx.attribute(TRACE_ATTR, new LinkedList<>());
//            }
//            long ms = System.currentTimeMillis() - ctx.requestStartTime();
//            ((List<String>) ctx.attribute(TRACE_ATTR)).add(ms + "ms " + Utils.fillTemplate(message, Arrays.asList(args)));
//        }
//    }

    public static String getContentType(File file) {
        return getContentType(file.getName());
    }

    public static String getContentType(String file) {
        int extStart = file.lastIndexOf('.');
        if (extStart < 0) {
            return "text/plain";
        }
        String ext = file.substring(extStart + 1);
        switch (ext) {
            case "html":
            case "htm":
                return "text/html";
            case "json":
            case "jsn":
                return "application/json";
            case "js":
            case "javascript":
                return "text/javascript";
            case "xml":
                return "application/xml";
            case "css":
                return "text/css";
            case "csv":
                return "text/csv";
            case "png":
                return "image/png";
            case "jpg":
            case "jpeg":
                return "image/jpeg";
            case "gif":
                return "image/gif";
            case "ico":
                return "image/x-icon";
            case "woff":
                return "application/woff";
            case "otf":
                return "font/opentype";
            case "bin":
                return "application/octet-stream";
            case "txt":
            case "text":
            default:
                return "text/plain";
        }
    }
}