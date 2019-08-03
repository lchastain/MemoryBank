import junit.framework.TestCase;
import org.junit.Test;

import javax.swing.*;

/**
 * Created by Lee on 8/3/2019.
 */
public class AppSplashTest extends TestCase {
    private AppSplash as = new AppSplash(new ImageIcon());

    @Test
    // Nothing to check on, for this test; it's more about getting coverage.
    public void testSetProgress() throws Exception {
        as.setProgress("Starting", 50);
    }
}