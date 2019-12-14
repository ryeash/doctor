package demo.app;

import org.testng.Assert;
import vest.doctor.ThreadLocal;

import javax.inject.Inject;

import static demo.app.CustomQualifier.Color.BLACK;
import static demo.app.CustomQualifier.Color.RED;

@ThreadLocal
public class TCCustomQualifierHolder {

    @Inject
    public TCCustomQualifierHolder(@CustomQualifier(name = "one", color = BLACK) TCCustomQualifier one,
                                   @CustomQualifier(name = "two", color = CustomQualifier.Color.BLUE) TCCustomQualifier two,
                                   @CustomQualifier(name = "defaultColor") TCCustomQualifier defaultColor,
                                   @CustomQualifier(name = "defaultColor", color = RED) TCCustomQualifier defaultColorAgain) {
        Assert.assertEquals(one.designation(), "blackOne");
        Assert.assertEquals(two.designation(), "blueTwo");
        Assert.assertEquals(defaultColor.designation(), "defaultColor");
        Assert.assertEquals(defaultColorAgain.designation(), "defaultColor");
    }
}
