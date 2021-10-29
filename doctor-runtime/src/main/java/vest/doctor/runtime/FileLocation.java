package vest.doctor.runtime;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Represents a location for a file. Locations can be on the classpath, served by http(s) endpoints, or
 * on the local file system.
 */
public class FileLocation {

    public static final String CLASSPATH = "classpath:";
    public static final String HTTP = "http:";
    public static final String HTTPS = "https:";
    public static final String FILE = "file:";

    private final String location;

    /**
     * Create a new file location.
     *
     * @param location the location
     */
    public FileLocation(String location) {
        this.location = location;
    }

    /**
     * Create a URL that can be used to read the data from the file location.
     *
     * @return the url for the file location
     */
    public URL toURL() {
        try {
            if (location.startsWith(CLASSPATH)) {
                for (int i = 0; i < 2; i++) {
                    URL url = ClassLoader.getSystemResource(location.substring(CLASSPATH.length() + i));
                    if (url != null) {
                        return url;
                    }
                }
                throw new IllegalArgumentException("unable to find classpath resource: " + location);
            } else if (location.startsWith(HTTP) || location.startsWith(HTTPS)) {
                return URI.create(location).toURL();
            } else if (location.startsWith(FILE)) {
                return new File(location.substring(FILE.length())).toURI().toURL();
            } else {
                return new File(location).toURI().toURL();
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Determine if this file location is valid by attempting to open a stream to the source.
     */
    @SuppressWarnings("unused")
    public boolean valid() {
        try (InputStream is = toURL().openStream()) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Fully read the file to a string.
     *
     * @return a string representation of the contents of the file
     * @throws UncheckedIOException for any IO error
     */
    public String readToString() {
        StringBuilder sb = new StringBuilder();
        int read;
        byte[] buf = new byte[2048];
        try (InputStream inputStream = toURL().openStream()) {
            while ((read = inputStream.read(buf)) >= 0) {
                sb.append(new String(buf, 0, read, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return location;
    }
}
