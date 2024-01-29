package vest.doctor.sleipnir.http;

public record Header(String name, String value) implements HttpData {
    public boolean matches(String name, String value) {
        return this.name.equalsIgnoreCase(name) && this.value.equals(value);
    }
}
