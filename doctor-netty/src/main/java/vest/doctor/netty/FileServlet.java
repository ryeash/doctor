package vest.doctor.netty;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaderValues;
import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.BeanProvider;

import java.io.File;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Date;

public class FileServlet implements Route {

    private Path baseDir;
    private String pathParam;

    FileServlet(String baseDir, String pathParam) {
        this.baseDir = new File(baseDir).toPath().normalize().toAbsolutePath();
        if (!this.baseDir.toFile().isDirectory()) {
            throw new IllegalArgumentException("not a directory " + baseDir);
        }
        this.pathParam = pathParam;
    }

    @Override
    public void init(BeanProvider beanProvider) {

    }

    @Override
    public void accept(RequestContext requestContext) {
        try {
            String uri = requestContext.pathParam(pathParam);
            if (uri == null) {
                requestContext.halt(HttpResponseStatus.INTERNAL_SERVER_ERROR, "file servlet was not initialized correctly, must use a wildcard route path like '/files/*'");
                requestContext.responseHeader(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                return;
            }
            Path p = new File(baseDir.toFile(), uri).toPath().normalize();
            if (!p.startsWith(baseDir)) {
                requestContext.halt(HttpResponseStatus.FORBIDDEN, "invalid path " + uri);
                requestContext.responseHeader(HttpHeaderNames.CONNECTION, HttpHeaderValues.CLOSE);
                return;
            }

            File file = p.toFile();
            if (file.isHidden() || !file.exists() || !file.isFile()) {
                requestContext.halt(HttpResponseStatus.NOT_FOUND, "file does not exist " + uri);
                return;
            }

            Long ifModifiedSince = requestContext.requestHeaders().getTimeMillis(HttpHeaderNames.IF_MODIFIED_SINCE);
            if (!isModified(file, ifModifiedSince)) {
                requestContext.halt(304);
                return;
            }

            FileChannel fc = FileChannel.open(file.toPath(), StandardOpenOption.READ);
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            ByteBuf body = Unpooled.wrappedBuffer(bb);
            fc.close();

            requestContext.responseHeader(HttpHeaderNames.CONTENT_TYPE, getContentType(file));
            requestContext.responseHeader(HttpHeaderNames.LAST_MODIFIED, new Date(file.lastModified()));
            requestContext.responseBody(body);
        } catch (Exception e) {
            throw new RuntimeException("error sending file", e);
        }
    }

    private static boolean isModified(File file, Long ifModifiedSince) {
        if (ifModifiedSince != null) {
            long ifModifiedSinceDateSeconds = ifModifiedSince / 1000;
            long fileLastModifiedSeconds = file.lastModified() / 1000;
            return ifModifiedSinceDateSeconds != fileLastModifiedSeconds;
        }
        return true;
    }

    private static String getContentType(File file) {
        return contentTypeStr(file.getName());
    }

    private static String contentTypeStr(String file) {
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