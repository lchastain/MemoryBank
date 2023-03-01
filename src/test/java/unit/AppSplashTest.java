import org.junit.jupiter.api.Test;

import javax.swing.*;

class AppSplashTest {
    private final AppSplash as = new AppSplash(new ImageIcon());

    // Nothing to check on, for this test; it's more about getting coverage.
    @Test
    void testSetProgress() {
        as.setProgress("Starting", 50);
    }
}