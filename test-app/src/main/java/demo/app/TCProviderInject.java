package demo.app;

import org.testng.Assert;
import vest.doctor.DoctorProvider;

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
    }
}
