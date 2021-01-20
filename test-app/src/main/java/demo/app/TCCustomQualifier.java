package demo.app;

public class TCCustomQualifier {

    private final String designation;

    public TCCustomQualifier(String designation) {
        this.designation = designation;
    }

    public String designation() {
        return designation;
    }
}
