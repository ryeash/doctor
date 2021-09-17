package demo.app;

import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import org.testng.Assert;
import vest.doctor.ConfigurationFacade;
import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;

import java.util.List;
import java.util.stream.Stream;

@Singleton
public class TCProviderInject {

    @Inject
    public TCProviderInject(Provider<CoffeeMaker> coffeeMakerProvider,
                            DoctorProvider<CoffeeMaker> coffeeMakerDoctorProvider,
                            @Named("pourOver") Provider<CoffeeMaker> pourOverProvider,
                            @Named("pourOver") DoctorProvider<CoffeeMaker> pourOverDoctorProvider,
                            List<CoffeeMaker> coffeeMakers,
                            Stream<CoffeeMaker> coffeeMakerStream,
                            CoffeeMaker[] coffeeMakersArr) {
        Assert.assertEquals(coffeeMakerProvider.get().brew(), "french pressing");
        Assert.assertEquals(coffeeMakerDoctorProvider.get().brew(), "french pressing");
        Assert.assertEquals(pourOverProvider.get().brew(), "pouring over");
        Assert.assertEquals(pourOverDoctorProvider.get().brew(), "pouring over");
        Assert.assertEquals(coffeeMakers.size(), 4);
        Assert.assertEquals(coffeeMakerStream.count(), 4L);
        Assert.assertEquals(coffeeMakersArr.length, 4);
    }

    public boolean postConstructCalled = false;

    @PostConstruct
    public void postConstruct(ProviderRegistry providerRegistry,
                              ConfigurationFacade configurationFacade) {
        Assert.assertNotNull(providerRegistry);
        Assert.assertNotNull(configurationFacade);
        postConstructCalled = true;
    }
}
