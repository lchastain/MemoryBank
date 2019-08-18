import org.junit.jupiter.api.Test;

import javax.swing.*;

public class AppSplashTest {
    private AppSplash as = new AppSplash(new ImageIcon());

    // Nothing to check on, for this test; it's more about getting coverage.
    @Test
    public void testSetProgress() throws Exception {
        as.setProgress("Starting", 50);
    }
}