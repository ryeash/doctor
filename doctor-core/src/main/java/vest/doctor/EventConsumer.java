package vest.doctor;

public interface EventConsumer {

    boolean isCompatible(Object event);

    void accept(Object event);

    default boolean async() {
        return false;
    }

}
