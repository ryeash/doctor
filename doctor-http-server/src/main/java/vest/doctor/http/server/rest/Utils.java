package vest.doctor.http.server.rest;

import java.io.File;

public class Utils {

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

    public static String getContentType(File file) {
        return getContentType(file.getName());
    }

    public static String getContentType(String file) {
        int extStart = file.lastIndexOf('.');
        if (extStart < 0) {
            return "text/plain";
        }
        String ext = file.substring(extStart + 1);
        return switch (ext) {
            case "html", "htm" -> "text/html";
            case "json", "jsn" -> "application/json";
            case "js", "javascript" -> "text/javascript";
            case "xml" -> "application/xml";
            case "css" -> "text/css";
            case "csv" -> "text/csv";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "ico" -> "image/x-icon";
            case "woff" -> "application/woff";
            case "otf" -> "font/opentype";
            case "bin" -> "application/octet-stream";
            default -> "text/plain";
        };
    }
}