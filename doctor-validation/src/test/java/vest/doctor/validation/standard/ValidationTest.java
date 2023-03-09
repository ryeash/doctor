package vest.doctor.validation.standard;

import jakarta.validation.constraints.DecimalMax;
import org.testng.Assert;
import org.testng.annotations.Test;
import vest.doctor.runtime.AnnotationDataImpl;

import java.math.BigDecimal;
import java.util.Map;

public class ValidationTest extends Assert {

    @Test
    public void standards() {
        assertNotNull(StandardValidators.assertFalse(null, true, null));
        assertNull(StandardValidators.assertFalse(null, false, null));

        assertNotNull(StandardValidators.assertTrue(null, false, null));
        assertNull(StandardValidators.assertTrue(null, true, null));

        assertNotNull(StandardValidators.decimalMax(new AnnotationDataImpl(
                        DecimalMax.class, Map.of(
                        "message", "toast",
                        "value", "42.1",
                        "inclusive", true)),
                new BigDecimal("42.2"), null));
        assertNull(StandardValidators.decimalMax(new AnnotationDataImpl(
                        DecimalMax.class, Map.of(
                        "message", "toast",
                        "value", "42.1",
                        "inclusive", true)),
                42, null));

        assertNull(StandardValidators.decimalMin(new AnnotationDataImpl(
                        DecimalMax.class, Map.of(
                        "message", "toast",
                        "value", "42.1",
                        "inclusive", true)),
                new BigDecimal("42.2"), null));
        assertNotNull(StandardValidators.decimalMin(new AnnotationDataImpl(
                        DecimalMax.class, Map.of(
                        "message", "toast",
                        "value", "42.1",
                        "inclusive", true)),
                42, null));

        assertNotNull(StandardValidators.digits(new AnnotationDataImpl(
                        DecimalMax.class, Map.of(
                        "message", "toast",
                        "integer", 4,
                        "fraction", 3)),
                45552.1293D, null));
        assertNull(StandardValidators.digits(new AnnotationDataImpl(
                        DecimalMax.class, Map.of(
                        "message", "toast",
                        "integer", 4,
                        "fraction", 3)),
                42.3D, null));
    }
}
