package vest.doctor.flow;

/**
 * Emitter of values to downstream processors.
 */
@FunctionalInterface
public interface Emitter<T> {
    /**
     * Emit a value to the next stage in the pipeline.
     *
     * @param value the value to emit
     */
    void emit(T value);
}
