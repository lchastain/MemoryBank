import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;

class ToggleAboutTest {
    private static AppTreePanel atp;
    private int theSelectionRow;
    private AppMenuBar amb;

    @BeforeAll
    static void meFirst() {
        // The problem with just having this in the BeforeEach was that we started
        // multiple JMenuItem listeners and not all of them would go away before
        // they were activated by other tests, causing much confusion.
        atp = new AppTreePanel(new JFrame(), new AppOptions());
    }

    @BeforeEach
    void setUp() {
        // No significance to this value other than it needs to be a row that
        // we know for a fact will be there, even for a brand-new AppTreePanel.
        // In this case we've chosen a relatively low (safer) value, currently
        // should be 'Notes', with the first two being singles, and 'Views' collapsed.
        theSelectionRow = 3;
        amb = AppTreePanel.amb;
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        amb = null;
        // These tests drive the app faster than it would go if it was only under user control.
        Thread.sleep(200); // Otherwise we see NullPointerExceptions after tests pass.
    }

    // Test that showing the About graphic will do that, and then
    // if selected again and it is already showing, it will go
    // back to the previous tree selection.
    @Test
    void testShowAboutToggle() throws Exception {
        JTree theTree = atp.getTree();
        int[] theRows;

        // Make a selection - actual row content does not matter.
        theTree.setSelectionRow(theSelectionRow);

        // This delay IS needed (sometimes), because the main app has a lot more going on and does
        // not come back as quickly from setting the selection row.  When this test does not have
        // the delay, it gets the wrong value on the third read of the selection row.  Not sure why
        // the delay is needed here then, vs down there, but that's the way it is.  Suspecting some
        // crossed threads (with the 'Working' dialog, maybe) are causing the issues but this is
        // where I'm leaving it, for now.
        // Also - this test was failing when run individually but not when running all the tests
        // in this file.  It also had different behaviors depending on whether it was a run or a
        // debug session, even when debug had no breakpoints.  So if you 'fix' it - be sure to
        // test with all variants of execution.
//        Thread.sleep(100);

        // Now we ensure that our setting 'took'.
        theRows = theTree.getSelectionRows(); // First reading
        assert theRows != null;
        assert(theRows[0] == theSelectionRow);

        // Ok, now show the About graphic
        atp.showAbout();

        // And verify that we no longer have a tree selection.
        theRows = theTree.getSelectionRows(); // Second reading
        assert theRows != null;
        assert(theRows.length == 0);

        // And now select 'About' again, even though it's already showing.
        // This is what activates the 'toggle' functionality.
        atp.showAbout();

        // And then verify that the 'toggle' feature worked, to put us back
        // to the tree selection that was made earlier.
        theRows = theTree.getSelectionRows(); // Third reading
        assert theRows != null;
        assert(theRows[0] == theSelectionRow);
    }

}