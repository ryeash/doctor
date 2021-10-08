package vest.doctor.test;

import vest.doctor.Modules;
import vest.doctor.Prototype;

@Prototype
@Modules("minerals")
public class Granite implements Mineral, AutoCloseable {
    @Override
    public int hardness() {
        return 7;
    }

    @Override
    public void close() throws Exception {
        System.out.println("GRANITE CLOSING NOW");
    }
}
