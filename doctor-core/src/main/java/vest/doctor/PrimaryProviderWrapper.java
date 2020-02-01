package vest.doctor;

/**
 * Used internally to create a primary provider; i.e. always return 'null' for the qualifier.
 */
public final class PrimaryProviderWrapper<T> extends DoctorProviderWrapper<T> {

    public PrimaryProviderWrapper(DoctorProvider<T> delegate) {
        super(delegate);
    }

    @Override
    public String qualifier() {
        return null;
    }
}
