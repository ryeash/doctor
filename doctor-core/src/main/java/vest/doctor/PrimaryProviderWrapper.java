package vest.doctor;

public final class PrimaryProviderWrapper<T> extends DoctorProviderWrapper<T> {

    public PrimaryProviderWrapper(DoctorProvider<T> delegate) {
        super(delegate);
    }

    @Override
    public String qualifier() {
        return null;
    }
}
