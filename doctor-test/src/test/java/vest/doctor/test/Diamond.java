package vest.doctor.test;

import vest.doctor.Modules;
import vest.doctor.Prototype;

@Prototype
@Modules("minerals")
public class Diamond implements Mineral {
    @Override
    public int hardness() {
        return 10;
    }
}
