package vest.doctor;

public class ApplicationStartedEvent {

    private final BeanProvider beanProvider;

    public ApplicationStartedEvent(BeanProvider beanProvider) {
        this.beanProvider = beanProvider;
    }

    public BeanProvider beanProvider() {
        return beanProvider;
    }
}
