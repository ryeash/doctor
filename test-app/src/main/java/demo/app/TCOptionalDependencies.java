package demo.app;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import vest.doctor.Prototype;

import java.util.Optional;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.assertTrue;

@Prototype
public class TCOptionalDependencies {

    @Inject
    public TCOptionalDependencies(Optional<CoffeeMaker> optionalCoffeeMaker,
                                  @Named("missing!!!") Optional<CoffeeMaker> missingCoffeeMaker) {
        assertTrue(optionalCoffeeMaker.isPresent());
        assertEquals(optionalCoffeeMaker.get().brew(), "french pressing");
        assertFalse(missingCoffeeMaker.isPresent());
        assertThrows(NullPointerException.class, missingCoffeeMaker::get);
    }
}
