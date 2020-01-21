package demo.app;

import org.testng.Assert;
import vest.doctor.BeanProvider;
import vest.doctor.ConfigurationFacade;
import vest.doctor.DoctorProvider;

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
                            DoctorProvider<CoffeeMaker> coffeeMakerNurseProvider,
                            @Named("pourOver") Provider<CoffeeMaker> pourOverProvider,
                            @Named("pourOver") DoctorProvider<CoffeeMaker> pourOverNurseProvider,
                            List<CoffeeMaker> coffeeMakers,
                            CoffeeMaker[] coffeeMakersArr) {
        Assert.assertEquals(coffeeMakerProvider.get().brew(), "french pressing");
        Assert.assertEquals(coffeeMakerNurseProvider.get().brew(), "french pressing");
        Assert.assertEquals(pourOverProvider.get().brew(), "pouring over");
        Assert.assertEquals(pourOverNurseProvider.get().brew(), "pouring over");
        Assert.assertEquals(coffeeMakers.size(), 2);
        Assert.assertEquals(coffeeMakersArr.length, 2);
    }

    @PostConstruct
    public void postConstruct(BeanProvider beanProvider,
                              ConfigurationFacade configurationFacade) {
        Assert.assertNotNull(beanProvider);
        Assert.assertNotNull(configurationFacade);
    }
}
