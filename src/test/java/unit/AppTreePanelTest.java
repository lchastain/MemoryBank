import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.WindowEvent;
import java.io.*;
import java.time.LocalDate;
import java.util.Enumeration;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertNull;

// Note about using a menu-item click to run private methods:
// You could see multiple effects from that, if the other tests in the full suite leave behind JMenuItem listeners.
//   So a possibly better alternative is to change the access level of the method to package-private
//   and just call it directly, rather than via a menu item click.    Just sayin..


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AppTreePanelTest {
    private static AppTreePanel appTreePanel;
    private static int theSelectionRow;

    @BeforeAll
    static void meFirst() throws IOException {
        System.out.println("AppTreePanel Test");
        MemoryBank.debug = true;
        AppTreePanel.theInstance = null; // Ensure that we get the right one.

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.userEmail = "test.user@lcware.net";
        MemoryBank.appEnvironment = "ide";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(FileDataAccessor.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception ignore) {
            System.out.println("Was unable to clean the test.user@lcware.net directory!");
        }

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();

        // The problem with just having this in the BeforeEach was that we started
        // multiple JMenuItem listeners with each new atp, and each test ran so
        // fast that not all of the listeners would have gone
        // away before they were activated by other tests, causing some confusion.
        appTreePanel = TestUtil.getTheAppTreePanel();
        appTreePanel.restoringPreviousSelection = true; // This should stop the multi-threading.

        // No significance to this value other than it needs to be a row that
        // we know for a fact will be there, even for a brand-new AppTreePanel.
        // In this case we've chosen a relatively low (safer) value, currently
        // should be 'Notes', with the first two being singles, and 'Views' collapsed.
        theSelectionRow = 3;
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // These tests drive the app faster than it would go if it was only under user control.
        Thread.sleep(500); // Otherwise we see NullPointerExceptions after tests pass.
    }

    @AfterAll
    static void meLast() {
        appTreePanel = null;
    }

    @Test
    @Order(1)
    void testDeepClone() {
        JTree theTree = appTreePanel.getTree(); // Usage here means no united test needed for getTree().
        theTree.setSelectionRow(theSelectionRow);
        TreePath treePath = theTree.getSelectionPath();
        Assertions.assertNotNull(treePath);
        DefaultMutableTreeNode original = (DefaultMutableTreeNode) treePath.getLastPathComponent();
        DefaultMutableTreeNode clone = AppTreePanel.deepClone(original);
        Assertions.assertEquals(original.toString(), clone.toString());
    }

    // VERIFY that this is still needed - the functional test also covers this code.
    // But - if there WERE to be a failure, wouldn't this simpler test be a better
    // tool to use to track down the problem?
    //
    // A critical assumption, currently, is that if no node of the
    // tree is selected, it means that the About graphic is shown.
    // Future dev on the tree MAY offer other cases where the
    // selection is null and if so, this could need rework.
    @Test
    @Order(2)
    void testShowAbout() {
        JTree theTree = appTreePanel.getTree();
        int[] theRows;

        // Make a selection - actual row content does not matter.
        theTree.setSelectionRow(theSelectionRow);

        // Now we ensure that our setting 'took'.
        theRows = theTree.getSelectionRows(); // First reading
        assert theRows != null;
        assert (theRows[0] == theSelectionRow);

        // Ok, now show the About graphic
        appTreePanel.showAbout();

        // And verify that we no longer have a tree selection.
        theRows = theTree.getSelectionRows(); // Second reading
        assert theRows != null;
        assert (theRows.length == 0);
    }


    @Test
    @Order(3)
    void testShowDay() {
        appTreePanel.showDay();
        JTree theTree = appTreePanel.getTree();
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        assert tp.getLastPathComponent().toString().equals("Day Notes");
    }

    @Test
    @Order(4)
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
    //@Test
    public void showMenus() {
        int numMenus = appTreePanel.getAppMenuBar().getMenuCount();
        MemoryBank.debug("Number of menus found: " + numMenus);
        for (int i = 0; i < numMenus; i++) {
            JMenu jm = appTreePanel.getAppMenuBar().getMenu(i);
            if (jm == null) continue;
            System.out.println("Menu: " + jm.getText());

            for (int j = 0; j < jm.getItemCount(); j++) {
                JMenuItem jmi = jm.getItem(j);
                if (jmi == null) continue; // Separator
                System.out.println("    Menu Item text: " + jmi.getText());
            } // end for j
        } // end for i
    }

    @Test
    @Order(5)
    void testShowHelp() {
        appTreePanel.showHelp();
    }

    @Test
    @Order(6)
    void testShowMonth() {
        // For the test user there is icon data in this month; needs to have
        //   icon data to get the coverage we're looking for here.
        LocalDate theMonthToShow = LocalDate.of(2019, 7, 15);
        appTreePanel.theMonthView = new MonthView(theMonthToShow);
        appTreePanel.showMonthView();
        JTree theTree = appTreePanel.getTree();
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        assert tp.getLastPathComponent().toString().equals("Month View");
        System.out.println("End testShowMonthView");
    }

    @Test
    @Order(7)
    void testShowToday() {
        // Too simple to pass by - just getting coverage
        appTreePanel.showToday();
    }

    @Test
    @Order(8)
    void testShowWeek() {
        appTreePanel.showWeek(LocalDate.now());
        JTree theTree = appTreePanel.getTree();
        TreePath tp = theTree.getSelectionPath();
        assert tp != null;
        assert tp.getLastPathComponent().toString().equals("Week View");
        System.out.println("End testShowWeek");
    }

    @Test
    @Order(9)
    void testCreateArchive() {
//        appTreePanel.dateDecremented(LocalDate.now(), ChronoUnit.DAYS);
//        appTreePanel.dateIncremented(LocalDate.now(), ChronoUnit.DAYS);

        // The 'show' tests can hose the menus during testing, so this is needed to restore them.
        appTreePanel.appMenuBar.manageMenus("No Selection");

        TestUtil testUtil = (TestUtil) appTreePanel.optionPane;
        JMenuItem jmi = testUtil.getMenuItem("App", "Archive...");
        jmi.doClick(); // You could see multiple effects from this, if the other tests leave behind JMenuItem listeners.
    }

    @Test
    @Order(10)
    void testAddNew() {
        appTreePanel.appMenuBar.manageMenus("To Do List");
        appTreePanel.restoringPreviousSelection = false; // not this time; it's not only about threading.
        TestUtil testUtil = (TestUtil) appTreePanel.optionPane;
        JMenuItem jmi = testUtil.getMenuItem("To Do List", "Add New...");
        jmi.doClick(); // You could see multiple effects from this, if other tests have left behind JMenuItem listeners.
        appTreePanel.restoringPreviousSelection = true; // put it back.
    }

    @Test
    @Order(11)
    void testGroupSaveAs() throws Exception {
        // First, Load a current list
        JTree theTree = appTreePanel.getTree();
        DefaultTreeModel theTreeModel = (DefaultTreeModel) theTree.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        DefaultMutableTreeNode dmtn = getTreeNodeForString(theRoot, "Get New Job");
        Assertions.assertNotNull(dmtn);
        TreePath tp = AppUtil.getTreePath(dmtn);
        Assertions.assertNotNull(tp);
        theTree.setSelectionPath(tp);
        Thread.sleep(800);  // Allow the change time to bake in, and reset the app menus.

        // A side-activity; checks off another method test.
        appTreePanel.showCurrentNoteGroup();

        appTreePanel.appMenuBar.manageMenus("To Do List");
        TestUtil testUtil = (TestUtil) appTreePanel.optionPane;
        JMenuItem jmi = testUtil.getMenuItem("To Do List", "Save As...");
        testUtil.setTheAnswerString("Get New Joke");
        jmi.doClick(); // You could see multiple effects from this, if other tests have left behind JMenuItem listeners.

    }

    @Test
    @Order(12)
    void testCloseSearchResult() throws Exception {
        // First, select a known search result (we know the content of our test data)
        String theSearchResult = "20201107080423";
        JTree theTree = appTreePanel.getTree();
        DefaultTreeModel theTreeModel = (DefaultTreeModel) theTree.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        DefaultMutableTreeNode dmtn = getTreeNodeForString(theRoot, theSearchResult);
        Assertions.assertNotNull(dmtn);
        TreePath tp = AppUtil.getTreePath(dmtn);
        Assertions.assertNotNull(tp);
        theTree.setSelectionPath(tp);
        Thread.sleep(200);
        assert Objects.requireNonNull(theTree.getSelectionPath()).getLastPathComponent().toString().equals(theSearchResult);

        // Now close it.
        appTreePanel.closeGroup();

        // And verify that it is gone from the tree
        dmtn = getTreeNodeForString(theRoot, theSearchResult);
        assertNull(dmtn);

        // And that the tree selection is now null.
        // Previously it went up to 'Search Results' but that puts unnecessary work onto the branch helper;
        //   if the user used a menu item to close this result but now they also really want to see it has
        //   been deselected in the branch editor, then they just need to go there via their own action.
        assert theTree.getSelectionPath() == null;
    }

    @Test
    @Order(13)
    void testShowEvents() {
        // We need to start a 'window close' thread that will kick in only AFTER we have shown the Events.
        new Thread(() -> {
            try {
                Thread.sleep(1500);
                WindowEvent we = new WindowEvent(appTreePanel.theEventsDialog, WindowEvent.WINDOW_CLOSING);
                appTreePanel.theEventsDialog.dispatchEvent(we);
            } catch (InterruptedException ignore) { }
        }).start(); // Start the thread
        appTreePanel.showEvents();
    }

    @Test
    @Order(14)
    // This one tests the 'no current group' path; the 'good' paths may be getting tested elsewhere.
    void testShowCurrentGroup() {
        appTreePanel.showArchives();

        JTree theTree = appTreePanel.getTree();
        theTree.setSelectionRow(0);

        appTreePanel.showCurrentNoteGroup();
    }

    @SuppressWarnings("rawtypes")
    static DefaultMutableTreeNode getTreeNodeForString(DefaultMutableTreeNode theRoot, String theString) {
        DefaultMutableTreeNode dmtn = null;
        DefaultMutableTreeNode nextNode;
        Enumeration bfe = theRoot.breadthFirstEnumeration();

        while (bfe.hasMoreElements()) {
            nextNode = (DefaultMutableTreeNode) bfe.nextElement();
            if (nextNode.toString().equals(theString)) {
                dmtn = nextNode;
                break;
            }
        }
        return dmtn;
    }
}
