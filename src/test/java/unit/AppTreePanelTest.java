import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AppTreePanelTest implements Notifier {
    private static AppTreePanel atp;
    private static JTree theTree;
    private static int theSelectionRow;
    private static AppMenuBar amb;

    @BeforeAll
    static void meFirst() throws IOException {
        // Set the location for our user data (it will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        MemoryBank.loadOpts();

        // The problem with just having this in the BeforeEach was that we started
        // multiple JMenuItem listeners with each new atp, and each test ran so
        // fast that not all of the listeners would have gone
        // away before they were activated by other tests, causing some confusion.
        atp = new AppTreePanel(new JFrame(), MemoryBank.appOpts);

        atp.setNotifier(new TestUtil());
        theTree = atp.getTree(); // Usage here means no unit test needed for getTree().
        amb = AppTreePanel.amb;

        // No significance to this value other than it needs to be a row that
        // we know for a fact will be there, even for a brand-new AppTreePanel.
        // In this case we've chosen a relatively low (safer) value, currently
        // should be 'Notes', with the first two being singles, and 'Views' collapsed.
        theSelectionRow = 3;
    }

    @AfterEach
    void restit() throws InterruptedException {
        // These tests drive the app faster than it would go if it was only under user control.
        Thread.sleep(300); // Otherwise we see NullPointerExceptions after tests pass.
    }

    @AfterAll
    static void tearDown() throws InterruptedException {
        theTree = null;
        amb = null;
    }
    @Test
    void testDeepClone() throws Exception {
        theTree.setSelectionRow(theSelectionRow);
        TreePath treePath = theTree.getSelectionPath();
        DefaultMutableTreeNode original = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        DefaultMutableTreeNode clone = AppTreePanel.deepClone(original);
        assert clone.toString().equals(original.toString());
    }

    @Test
    void testGetTodoListKeeper() throws Exception {
        TodoListKeeper tlk = atp.getTodoListKeeper();
        assert tlk != null;
    }

    @Test
    void testPreClose() throws Exception {
        atp.preClose();
        // No assertions needed here; we're just assuring coverage.
    }

    // VERIFY that this is still needed - the functional test also covers this code.
    // But - if there WERE to be a failure, wouldn't this simpler test be a better
    // tool to use to track down the problem?

    // A critical assumption, currently, is that if no node of the
    // tree is selected, it means that the About graphic is shown.
    // Future dev on the tree MAY offer other cases where the
    // selection is null and if so, this could need rework.
    @Test
    void testShowAbout() {
        JTree theTree = atp.getTree();
        int[] theRows;

        // Make a selection - actual row content does not matter.
        theTree.setSelectionRow(theSelectionRow);

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
    }


    @Test
    void testShowDay() throws Exception {
        atp.showDay();
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        assert tp.getLastPathComponent().toString().equals("Day Notes");
    }

    @Test
    void testShowFoundIn() throws Exception {
        SearchResultData mySrd = new SearchResultData(new NoteData());
        mySrd.setFileFoundIn(new File("2008/Y_20190301095623"));
        atp.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.setFileFoundIn(new File("2008/M02_20190208182959"));
        atp.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.setFileFoundIn(new File("2008/D0927_20170927175850"));
        atp.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.setFileFoundIn(new File("Long Term.todolist"));
        atp.showFoundIn(mySrd);
    }


    // NOT a real test; this is just here to spit out the menu hierarchy so that
    // during 'real' test development, we will know what MenuItem should be retrieved
    // by the local getMenuItem function, so that it can be 'clicked'.
    public void showMenus() throws Exception {
        int numMenus = amb.getMenuCount();
        MemoryBank.debug("Number of menus found: " + numMenus);
        for (int i = 0; i < numMenus; i++) {
            JMenu jm = amb.getMenu(i);
            if (jm == null) continue;
            System.out.println("Menu: " + jm.getText());

            for (int j = 0; j < jm.getItemCount(); j++) {
                JMenuItem jmi = jm.getItem(j);
                if (jmi == null) continue; // Separator
                System.out.println("    Menu Item text: " + jmi.getText());
            } // end for j
        } // end for i
    }

    // A utility function to retrieve a specified JMenuItem.
    // Calling contexts will perform a 'doClick' on the returned value.
    JMenuItem getMenuItem(String menu, String text) {
        JMenu jm = null;
        JMenuItem jmi = null;

        int numMenus = amb.getMenuCount();
        for (int i = 0; i < numMenus; i++) {
            jm = amb.getMenu(i);
            if (jm == null) continue;
            //System.out.println("Menu: " + jm.getText());
            if(jm.getText().equals(menu)) {
                for (int j = 0; j < jm.getItemCount(); j++) {
                    jmi = jm.getItem(j);
                    if (jmi == null) continue; // Separator
                    //System.out.println("    Menu Item text: " + jmi.getText());
                    if(jmi.getText().equals(text)) return jmi;
                } // end for j
            }
        } // end for i

        return jmi;
    }

    // This does test the showHelp function, but that feature is not anywhere near
    // ready for production and will be redone.  Meanwhile this test runs it and
    // one unwanted result is that a Help-file viewer window is left open (as it
    // is by the main app, as well).  In the main app 'normal' usage, it is up to
    // the user to close the window.  For a test case, this is obviously not an
    // acceptable outcome.  Live with it until the Help feature is refactored.
    @Test
    void testShowHelp() throws Exception {
        JMenuItem jmi = getMenuItem("Help", "Contents");
        jmi.doClick(); // You could see multiple effects from this, if the other tests leave behind JMenuItem listeners.
        //atp.showHelp("badFile"); // This is NOT throwing an exception, but putting up a 'cant find file' window/message.
        System.out.println("End testShowHelp");
    }

    @Test
    void testShowMonth() throws Exception {
        atp.showMonth();
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        assert tp.getLastPathComponent().toString().equals("Month View");
        System.out.println("End testShowMonth");
    }

    @Test
    void testShowToday() throws Exception {
        atp.showToday();
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        // Not sure what else to look for, here.  We would need to know what view
        // was previously showing, so we could get its choice and do a compare.
        // Doesn't seem necessary.
        System.out.println("End testShowToday");
    }

    @Test
    void testShowWeek() throws Exception {
        atp.showWeek();
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        assert tp.getLastPathComponent().toString().equals("Week View");
        System.out.println("End testShowWeek");
    }

    @Test
    void testCloseSearchResult() throws Exception {
        // First, select a known search result (we know the content of our test data)
        String theSearchResult = "S20140312131216";
        DefaultTreeModel theTreeModel = (DefaultTreeModel) theTree.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        DefaultMutableTreeNode dmtn = TestUtil.getTreeNodeForString(theRoot, theSearchResult);
        Assertions.assertNotNull(dmtn);
        TreePath tp = AppUtil.getPath(dmtn);
        Assertions.assertNotNull(tp);
        theTree.setSelectionPath(tp);
        Thread.sleep(200);
        assert theTree.getSelectionPath().getLastPathComponent().toString().equals(theSearchResult);

        // Now close it.
        atp.closeSearchResult();

        // And verify that the file for it is gone.
        String filename = MemoryBank.userDataHome + File.separatorChar + theSearchResult + ".sresults";
        File f = new File(filename);
        assertFalse(f.exists());

        // And verify that it is gone from the tree
        dmtn = TestUtil.getTreeNodeForString(theRoot, "S20140312131216");
        assertNull(dmtn);

        // And that the tree selection switched up to 'Search Results'
        assert theTree.getSelectionPath().getLastPathComponent().toString().equals("Search Results");
    }

}