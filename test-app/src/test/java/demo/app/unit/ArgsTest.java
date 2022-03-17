package demo.app.unit;

import org.testng.Assert;
import org.testng.annotations.Test;
import vest.doctor.runtime.Args;

public class ArgsTest {

    @Test
    public void args() {
        Args args = new Args(new String[]{
                "vest.app.BootConfig",
                "-vb",
                "-e", "extra",
                "--debug",
                "--withValue", "something"});
        Assert.assertEquals(args.length(), 7);

        Assert.assertEquals(args.first(), "vest.app.BootConfig");

        Assert.assertTrue(args.flag('v'));
        Assert.assertTrue(args.flag("debug", 'd'));
        Assert.assertFalse(args.flag("f"));

        Assert.assertEquals(args.option('e'), "extra");
        Assert.assertEquals(args.option('e', "fallback"), "extra");
        Assert.assertEquals(args.option("u", "fallback"), "fallback");

        Assert.assertTrue(args.flag("debug"));
        Assert.assertFalse(args.flag("info"));

        Assert.assertEquals(args.option("withValue"), "something");
        Assert.assertEquals(args.option("withValue", "fallback"), "something");
        Assert.assertEquals(args.option("u", "fallback"), "fallback");
        Assert.assertNull(args.option("unknown"));
        Assert.assertNull(args.option("debug"));
        Assert.assertNull(args.option("long", 'l'));

        Assert.assertTrue(args.contains("-e"));

        Assert.assertEquals(args.first(), "vest.app.BootConfig");
        Assert.assertEquals(args.second(), "-vb");
        Assert.assertEquals(args.third(), "-e");
        Assert.assertEquals(args.fourth(), "extra");
        Assert.assertEquals(args.fifth(), "--debug");

        Assert.assertThrows(IndexOutOfBoundsException.class, () -> args.get(10));
        Assert.assertThrows(IndexOutOfBoundsException.class, () -> args.get(-1));
    }
}
