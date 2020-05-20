package vest.doctor.netty.impl;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.DefaultHttpContent;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpResponseStatus;
import vest.doctor.netty.HttpException;
import vest.doctor.netty.Request;
import vest.doctor.netty.Response;
import vest.doctor.netty.ResponseBody;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.StandardOpenOption;
import java.util.Date;

/**
 * Can be returned from an endpoint to efficiently stream a file as a response body.
 */
public class FileResponseBody implements ResponseBody {

    private final java.nio.file.Path filepath;
    private final File file;
    private final long modified;

    /**
     * Create a new streaming file body. The file is checked to makes sure that it is present under the root,
     * else an exception is thrown.
     *
     * @param rootDirectory the root directory for the file, will be prefixed to the filePath to create the complete
     *                      file path
     * @param filePath      the path to the file to send
     */
    public FileResponseBody(String rootDirectory, String filePath) {
        java.nio.file.Path rootPath = new File(rootDirectory).getAbsoluteFile().toPath();

        this.file = new File(new File(rootDirectory), filePath).getAbsoluteFile();
        this.filepath = file.toPath();
        if (!filepath.startsWith(rootPath)) {
            throw new HttpException(HttpResponseStatus.FORBIDDEN, "invalid path: " + filePath);
        }
        if (!file.canRead() || file.isHidden() || !file.isFile()) {
            throw new HttpException(HttpResponseStatus.NOT_FOUND, "file does not exist: " + filePath);
        }
        this.modified = file.lastModified();
    }

    @Override
    public HttpContent toContent(Request request, Response response) {
        Long ifModifiedSince = request.headers().getTimeMillis(HttpHeaderNames.IF_MODIFIED_SINCE);
        if (!isModified(ifModifiedSince)) {
            response.status(HttpResponseStatus.NOT_MODIFIED);
            return new DefaultHttpContent(Unpooled.EMPTY_BUFFER);
        }

        try {
            FileChannel fc = FileChannel.open(filepath, StandardOpenOption.READ);
            MappedByteBuffer bb = fc.map(FileChannel.MapMode.READ_ONLY, 0, file.length());
            ByteBuf body = Unpooled.wrappedBuffer(bb);
            fc.close();

            // only set these headers if they don't exists -> allows them to be overwritten by user code
            if (!response.headers().contains(HttpHeaderNames.CONTENT_TYPE)) {
                response.headers().set(HttpHeaderNames.CONTENT_TYPE, getContentType(file));
            }
            if (!response.headers().contains(HttpHeaderNames.LAST_MODIFIED)) {
                response.headers().set(HttpHeaderNames.LAST_MODIFIED, new Date(file.lastModified()));
            }
            return new DefaultHttpContent(body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private boolean isModified(Long ifModifiedSince) {
        if (ifModifiedSince != null) {
            long ifModifiedSinceDateSeconds = ifModifiedSince / 1000;
            long fileLastModifiedSeconds = modified / 1000;
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
