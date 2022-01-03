package vest.doctor.aop;

/**
 * Marker class for all aspect interfaces.
 */
public sealed interface Aspect permits Before, After, Around {
}
