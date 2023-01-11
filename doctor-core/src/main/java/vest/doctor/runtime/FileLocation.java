package vest.doctor.runtime;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Represents a location for a file. Locations can be on the classpath, served by http(s) endpoints, or
 * on the local file system.
 */
public record FileLocation(String location) {

    public static final String CLASSPATH = "classpath:";
    public static final String FILE = "file:";

    /**
     * Open an {@link InputStream} suitable for reading the entire contents of the file.
     *
     * @return the input stream for the file
     */
    public InputStream openStream() {
        try {
            if (location.startsWith(CLASSPATH)) {
                for (int i = 0; i < 2; i++) {
                    URL url = ClassLoader.getSystemResource(location.substring(CLASSPATH.length() + i));
                    if (url != null) {
                        return url.openStream();
                    }
                }
                throw new IllegalArgumentException("unable to find classpath resource: " + location);
            } else if (location.startsWith(FILE)) {
                return new FileInputStream(location.substring(FILE.length()));
            } else {
                return new FileInputStream(location);
            }
        } catch (IOException e) {
            throw new UncheckedIOException("error reading file data from " + location, e);
        }
    }

    /**
     * Determine if this file location is valid by attempting to open a stream to the source.
     */
    @SuppressWarnings("unused")
    public boolean valid() {
        try (InputStream is = openStream()) {
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Fully read the file to a string using the UTF-8 charset.
     *
     * @return a string representation of the contents of the file
     * @throws UncheckedIOException for any IO error
     */
    public String readToString() {
        return readToString(StandardCharsets.UTF_8);
    }

    /**
     * Fully read the file to a string using the given charset.
     *
     * @param charset the charset to use to decode the data
     * @return a string representation of the contents of the file
     * @throws UncheckedIOException for any IO error
     */
    public String readToString(Charset charset) {
        return new String(readToBytes(), charset);
    }

    /**
     * Fully read the file to a byte array.
     *
     * @return an array of all bytes read from the file location
     * @throws UncheckedIOException for any IO error
     */
    public byte[] readToBytes() {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
        int read;
        byte[] buf = new byte[1024];
        try (InputStream inputStream = openStream()) {
            while ((read = inputStream.read(buf)) >= 0) {
                baos.write(buf, 0, read);
            }
            baos.close();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return baos.toByteArray();
    }

    @Override
    public String toString() {
        return location;
    }
}
