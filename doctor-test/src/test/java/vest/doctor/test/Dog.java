package vest.doctor.test;

public class Dog implements Animal {

    private final String sound;

    public Dog() {
        this("bark");
    }

    public Dog(String sound) {
        this.sound = sound;
    }

    @Override
    public String makeNoise() {
        return sound;
    }
}
