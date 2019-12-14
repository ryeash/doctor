package demo.app;

import vest.doctor.Eager;

import javax.inject.Singleton;

@Eager
@Singleton
public class TCEager {
    public static boolean created = false;

    public TCEager() {
        created = true;
    }
}
