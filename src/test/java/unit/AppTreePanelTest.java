import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

public class AppTreePanelTest {
    private static AppTreePanel appTreePanel;
    private static JTree theTree;
    private static int theSelectionRow;
    private static AppMenuBar amb;

    @BeforeAll
    static void meFirst() throws IOException {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception ignore){}

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
        appTreePanel = new AppTreePanel(new JFrame(), MemoryBank.appOpts);
        appTreePanel.restoringPreviousSelection = true; // This should stop the multi-threading.

        appTreePanel.setNotifier(new TestUtil());
        theTree = appTreePanel.getTree(); // Usage here means no unit test needed for getTree().
        amb = AppTreePanel.appMenuBar;

        // No significance to this value other than it needs to be a row that
        // we know for a fact will be there, even for a brand-new AppTreePanel.
        // In this case we've chosen a relatively low (safer) value, currently
        // should be 'Notes', with the first two being singles, and 'Views' collapsed.
        theSelectionRow = 3;
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // These tests drive the app faster than it would go if it was only under user control.
        Thread.sleep(700); // Otherwise we see NullPointerExceptions after tests pass.
    }

    @AfterAll
    static void meLast() {
        theTree = null;
        amb = null;
        appTreePanel.restoringPreviousSelection = false;
    }

    @Test
    void testDeepClone() {
        theTree.setSelectionRow(theSelectionRow);
        TreePath treePath = theTree.getSelectionPath();
        assert treePath != null;
        DefaultMutableTreeNode original = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        DefaultMutableTreeNode clone = AppTreePanel.deepClone(original);
        assert clone.toString().equals(original.toString());
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
        JTree theTree = appTreePanel.getTree();
        int[] theRows;

        // Make a selection - actual row content does not matter.
        theTree.setSelectionRow(theSelectionRow);

        // Now we ensure that our setting 'took'.
        theRows = theTree.getSelectionRows(); // First reading
        assert theRows != null;
        assert(theRows[0] == theSelectionRow);

        // Ok, now show the About graphic
        appTreePanel.showAbout();

        // And verify that we no longer have a tree selection.
        theRows = theTree.getSelectionRows(); // Second reading
        assert theRows != null;
        assert(theRows.length == 0);
    }


    @Test
    void testShowDay() {
        appTreePanel.showDay();
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        assert tp.getLastPathComponent().toString().equals("Day Notes");
    }

    @Test
    void testShowFoundIn() throws Exception {
        SearchResultData mySrd = new SearchResultData(new NoteData());
        mySrd.setFileFoundIn(new File("2008/Y_20190301095623"));
        appTreePanel.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.setFileFoundIn(new File("2008/M02_20190208182959"));
        appTreePanel.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.setFileFoundIn(new File("2008/D0927_20170927175850"));
        appTreePanel.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.setFileFoundIn(new File("todo_Long Term.json"));
        appTreePanel.showFoundIn(mySrd);
    }


    // NOT a real test; this is just here to spit out the menu hierarchy so that
    // during 'real' test development, we will know what MenuItem should be retrieved
    // by the local getMenuItem function, so that it can be 'clicked'.
    public void showMenus() {
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
        JMenu jm;
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

    // This does test the showHelp function, but that feature itself is not anywhere near
    // ready for production and will be redone.  When the method executes in the main
    // app, a Help-file viewer window is left open where under 'normal' usage, it is
    // up to the user to close.  But for a test case this is obviously not an acceptable
    // outcome so we ask the external Windows (platform-specific) executable 'taskkill'
    // to get rid of the window for us.
    // As an FYI, however, while the command was under development and I was trying
    // various syntax to use with 'exec' and none were working, I needed to see the
    // output/complaint that the taskkill process was emitting.  But running it from
    // a test case, I saw nothing in the output.  This is the reason for all the extra
    // reading and printout of the process's stdout and stderr streams.  Now that code
    // is no longer needed here but I'm leaving it as an example that could definitely
    // be useful in other contexts.
    @Test
    void testShowHelp() {
        JMenuItem jmi = getMenuItem("Help", "Contents");
        jmi.doClick(); // You could see multiple effects from this, if the other tests leave behind JMenuItem listeners.
        try {
            // Sleep, long enough for the help window to appear.
            // This is because it runs in a different thread and we can get to our
            // task killer before it ever comes up, and then it stays.
            Thread.sleep(800);
            // Kill the help window -
            Process process = Runtime.getRuntime().exec("taskkill /FI \"WindowTitle eq XML Notepad Help\" /T /F");

            // The rest of this is just to get the feedback from TASKKILL, if any.
            InputStream is = process.getInputStream();
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line;

            while ((line = br.readLine()) != null && !line.isEmpty()) {
                System.out.println(line);
                System.out.flush();
            }

            InputStream es = process.getErrorStream();
            InputStreamReader esr = new InputStreamReader(es);
            BufferedReader br2 = new BufferedReader(esr);

            while ((line = br2.readLine()) != null && !line.isEmpty()) {
                System.out.println(line);
                System.out.flush();
            }

            int exitVal = process.waitFor();
            System.out.println("Process exitValue: " + exitVal);

            System.out.println("End testShowHelp");
        } catch (IOException | InterruptedException ioe) {
            ioe.printStackTrace();
        }
    }

    @Test
    void testShowMonth() {
        appTreePanel.setViewedDate(LocalDate.now(), ChronoUnit.MONTHS);
        appTreePanel.showMonthView();
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        assert tp.getLastPathComponent().toString().equals("Month View");
        System.out.println("End testShowMonth");
    }

    @Test
    void testShowToday() {
        appTreePanel.showToday();
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        // Not sure what else to look for, here.  We would need to know what view
        // was previously showing, so we could get its choice and do a compare.
        // Doesn't seem necessary.
        System.out.println("End testShowToday");
    }

    @Test
    void testShowWeek() {
        appTreePanel.showWeek(LocalDate.now());
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        assert tp.getLastPathComponent().toString().equals("Week View");
        System.out.println("End testShowWeek");
    }

    @Test
    void testCloseSearchResult() throws Exception {
        // First, select a known search result (we know the content of our test data)
        String theSearchResult = "20190927161325";
        DefaultTreeModel theTreeModel = (DefaultTreeModel) theTree.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        DefaultMutableTreeNode dmtn = TestUtil.getTreeNodeForString(theRoot, theSearchResult);
        Assertions.assertNotNull(dmtn);
        TreePath tp = AppUtil.getTreePath(dmtn);
        Assertions.assertNotNull(tp);
        theTree.setSelectionPath(tp);
        Thread.sleep(200);
        assert Objects.requireNonNull(theTree.getSelectionPath()).getLastPathComponent().toString().equals(theSearchResult);

        // Now close it.
        appTreePanel.closeGroup();

        // And verify that the file for it is gone.
        String filename = MemoryBank.userDataHome + File.separatorChar + theSearchResult + ".sresults";
        File f = new File(filename);
        assertFalse(f.exists());

        // And verify that it is gone from the tree
        dmtn = TestUtil.getTreeNodeForString(theRoot, "20140312131216");
        assertNull(dmtn);

        // And that the tree selection switched up to 'Search Results'
        assert theTree.getSelectionPath().getLastPathComponent().toString().equals("Search Results");
    }

}