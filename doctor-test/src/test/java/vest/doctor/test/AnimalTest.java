package vest.doctor.test;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@TestConfiguration(
        modules = {"animals"},
        propertyFiles = {"classpath:test-config.props"})
public class AnimalTest extends AbstractDoctorTest {

    @Inject
    private Cat cat;

    @Inject
    protected Dog dog;

    @Inject
    @Named("dog")
    public Animal animal;

    @Inject
    private List<Animal> allAnimals;

    @Inject
    public Optional<Dog> maybeADog;

    @Inject
    Provider<Dog> providedDog;

    @Test
    public void catTest() {
        assertEquals(cat.makeNoise(), "meow");
    }

    @Test
    public void dogTest() {
        assertEquals(dog.makeNoise(), "bark");
    }

    @Test
    public void animalTest() {
        assertEquals(animal.makeNoise(), "bark");
    }

    @Test
    public void providerRegistryTest() {
        assertNotNull(providerRegistry());
    }

    @Test
    public void configTest() {
        assertEquals(providerRegistry().configuration().get("test.string"), "value");
    }

    @Test
    public void listInject() {
        List<String> collect = allAnimals.stream().map(Animal::makeNoise).collect(Collectors.toList());
        assertEquals(collect.size(), 3);
        assertTrue(collect.contains("bark"));
        assertTrue(collect.contains("meow"));
    }

    @Test
    public void optional() {
        assertTrue(maybeADog.isPresent());
        assertEquals(maybeADog.get().makeNoise(), "bark");
    }

    @Test
    public void provider() {
        assertEquals(providedDog.get().makeNoise(), "bark");
    }
}
