import junit.framework.TestCase;

import javax.swing.*;

public class AppTreePanelTest extends TestCase {
    private AppTreePanel atp;
    private int theSelectionRow;

    public void setUp() throws Exception {
        super.setUp();

        // No significance to this value other than it needs to be a row that
        // we know for a fact will be there, even for a brand-new AppTreePanel.
        // In this case we've chosen a relatively low (safer) value, currently
        // should be 'Notes', with the first two being singles, and 'Views' collapsed.
        theSelectionRow = 3;
        atp = new AppTreePanel(new JFrame(), new AppOptions());
    }

    public void tearDown() throws Exception {
        atp = null;
    }

    public void testAddSearchResult() throws Exception {

    }

    public void testCloseSearchResult() throws Exception {

    }

    public void testDeepClone() throws Exception {

    }

    public void testGetTodoListKeeper() throws Exception {

    }

    public void testGetTree() throws Exception {

    }

    public void testPreClose() throws Exception {

    }

    // THIS TEST is not unit test, but functional.  MOVE it, after
    // a new test module is ready for it.  Restore a showAbout (only) here.
    //
    // Test that showing the About graphic will do that, and then
    // if selected again and it is already showing, it will go
    // back to the previous tree selection.
    // Critical assumption, currently, is that if no node of the
    // tree is selected, it means that the About graphic is shown.
    // Future dev on the tree MAY offer other cases where the
    // selection is null and if so, this could need rework.
    public void testShowAboutToggle() throws Exception {
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
        Thread.sleep(100);

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

    public void testShowDay() throws Exception {

    }

    public void testShowFoundIn() throws Exception {

    }

    public void testShowHelp() throws Exception {

    }

    public void testShowMonth() throws Exception {

    }

    public void testShowToday() throws Exception {

    }

    public void testShowWeek() throws Exception {

    }

    public void testShowWorkingDialog() throws Exception {

    }

    public void testValueChanged() throws Exception {

    }
}