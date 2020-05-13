package demo.app;

import org.testng.Assert;
import vest.doctor.ConfigurationFacade;
import vest.doctor.DoctorProvider;
import vest.doctor.ProviderRegistry;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.List;

@Singleton
public class TCProviderInject {

    @Inject
    public TCProviderInject(Provider<CoffeeMaker> coffeeMakerProvider,
                            DoctorProvider<CoffeeMaker> coffeeMakerDoctorProvider,
                            @Named("pourOver") Provider<CoffeeMaker> pourOverProvider,
                            @Named("pourOver") DoctorProvider<CoffeeMaker> pourOverDoctorProvider,
                            List<CoffeeMaker> coffeeMakers,
                            CoffeeMaker[] coffeeMakersArr) {
        Assert.assertEquals(coffeeMakerProvider.get().brew(), "french pressing");
        Assert.assertEquals(coffeeMakerDoctorProvider.get().brew(), "french pressing");
        Assert.assertEquals(pourOverProvider.get().brew(), "pouring over");
        Assert.assertEquals(pourOverDoctorProvider.get().brew(), "pouring over");
        Assert.assertEquals(coffeeMakers.size(), 3);
        Assert.assertEquals(coffeeMakersArr.length, 3);
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
