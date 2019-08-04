import junit.framework.TestCase;

import javax.swing.*;

public class AppSplashTest extends TestCase {
    private AppSplash as = new AppSplash(new ImageIcon());

    // Nothing to check on, for this test; it's more about getting coverage.
    public void testSetProgress() throws Exception {
        as.setProgress("Starting", 50);
    }
}