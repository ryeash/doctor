package vest.doctor.conf;

import org.testng.Assert;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class ConfigurationTest extends Assert {

    ConfigurationFacade conf = new CompositeConfigurationFacade()
            .addSource(new MapConfigurationSource(Map.of(
                    "string", "value",
                    "boolean", "true",
                    "double", "42.1234",
                    "col", "1,2,3,3,3,6",
                    "executors.default.threads", "1",
                    "executors.background.threads", "2",
                    "executors.io.threads", "3"
            )));

    @Test
    public void basic() {
        assertEquals(conf.get("string"), "value");
        assertEquals(conf.get("missing", "fallback"), "fallback");
        assertTrue(conf.get("boolean", Boolean::parseBoolean));
        assertTrue(conf.get("missing", true, Boolean::parseBoolean));
        assertEquals(conf.get("double", Double::parseDouble), 42.1234D);
    }

    @Test
    public void collections() {
        assertEquals(conf.getList("col"), List.of("1", "2", "3", "3", "3", "6"));
        assertEquals(conf.getList("col", Integer::parseInt), List.of(1, 2, 3, 3, 3, 6));
        assertEquals(conf.getList("missing", List.of(1, 2), Integer::parseInt), List.of(1, 2));
        assertEquals(conf.getSet("col"), Set.of("1", "2", "3", "6"));
        assertEquals(conf.getSet("col", Integer::parseInt), Set.of(1, 2, 3, 6));
        assertEquals(conf.getSet("missing", Set.of(1, 2), Integer::parseInt), Set.of(1, 2));
    }

    @Test
    public void groups() {
        assertTrue(conf.getSubGroups("executors.").containsAll(Set.of("default", "background", "io")));
    }

    @Test
    public void prefix() {
        ConfigurationFacade prefix = conf.prefix("executors.default.");
        assertEquals(prefix.get("threads"), "1");
    }
}
