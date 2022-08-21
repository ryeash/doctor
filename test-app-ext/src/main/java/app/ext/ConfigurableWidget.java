package app.ext;

public class ConfigurableWidget implements Widget {

    private final String value;

    public ConfigurableWidget(String value) {
        this.value = value;
    }

    @Override
    public String wonk() {
        return value;
    }
}
