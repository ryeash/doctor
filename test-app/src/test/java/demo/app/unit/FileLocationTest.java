package demo.app.unit;

import org.testng.Assert;
import org.testng.annotations.Test;
import vest.doctor.runtime.FileLocation;

import java.util.stream.Stream;

public class FileLocationTest extends Assert {

    @Test
    public void testEverything() {
        Stream.of("classpath:META-INF/persistence.xml",
                        "classpath:/META-INF/persistence.xml",
                        "file:./src/main/resources/META-INF/persistence.xml",
                        "./src/main/resources/META-INF/persistence.xml")
                .map(FileLocation::new)
                .map(FileLocation::readToString)
                .forEach(string -> assertTrue(string.contains("<persistence-unit name=\"default\">")));
    }

}
