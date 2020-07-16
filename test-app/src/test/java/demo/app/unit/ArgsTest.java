package demo.app.unit;

import org.testng.Assert;
import org.testng.annotations.Test;
import vest.doctor.Args;

public class ArgsTest {

    @Test
    public void args() {
        Args args = new Args(new String[]{
                "vest.app.BootConfig",
                "-e", "extra",
                "--debug", "--withValue=something",
                "-b"
        });
        // validation expects the arguments: vest.assist.app.BootConfig -e extra --debug --withValue=something
        Assert.assertEquals(args.length(), 6);

        Assert.assertEquals(args.first(), "vest.app.BootConfig");

        Assert.assertTrue(args.flag("e"));
        Assert.assertFalse(args.flag("f"));

        Assert.assertEquals(args.flagValue("e"), "extra");
        Assert.assertEquals(args.flagValue("e", "fallback"), "extra");
        Assert.assertEquals(args.flagValue("u", "fallback"), "fallback");

        Assert.assertTrue(args.verboseFlag("debug"));
        Assert.assertFalse(args.verboseFlag("info"));

        Assert.assertEquals(args.verboseFlagValue("withValue"), "something");
        Assert.assertEquals(args.verboseFlagValue("withValue", "fallback"), "something");
        Assert.assertEquals(args.verboseFlagValue("u", "fallback"), "fallback");
        Assert.assertNull(args.verboseFlagValue("unknown"));
        Assert.assertNull(args.verboseFlagValue("debug"));

        Assert.assertTrue(args.contains("-e"));

        Assert.assertEquals(args.first(), "vest.app.BootConfig");
        Assert.assertEquals(args.second(), "-e");
        Assert.assertEquals(args.third(), "extra");
        Assert.assertEquals(args.fourth(), "--debug");
        Assert.assertEquals(args.fifth(), "--withValue=something");

        Assert.assertThrows(IndexOutOfBoundsException.class, () -> args.get(10));
        Assert.assertThrows(IndexOutOfBoundsException.class, () -> args.get(-1));
    }
}
