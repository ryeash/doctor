package vest.doctor.test;

import jakarta.inject.Singleton;

@Singleton
public class Cat implements Animal {
    @Override
    public String makeNoise() {
        return "meow";
    }
}
