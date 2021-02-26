package demo.app;

import jakarta.inject.Singleton;
import vest.doctor.Eager;

@Eager
@Singleton
public class TCEager {
    public static boolean created = false;

    public TCEager() {
        created = true;
    }
}
