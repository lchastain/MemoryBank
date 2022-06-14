import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.io.File;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class BranchSelectionTests {
    private static AppTreePanel appTreePanel;
    private static JTree theTree;

    static TestUtil notifier;
    static DefaultMutableTreeNode rootNode;

    // These members are retained between tests, thanks to the @TestInstance annotation.
    TreePath theSearchResultPath;

    @BeforeAll
    static void meFirst() {
        MemoryBank.debug = true;
        AppTreePanel.theInstance = null;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testDataLoc = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testDataLoc);
        } catch (Exception e) {
            System.out.println("ignored Exception: " + e.getMessage());
        }

        // The problem with just having this in the BeforeEach was that we started
        // multiple JMenuItem listeners each time, and each test ran so
        // fast that not all of the listeners would have gone
        // away before they were activated by other tests, causing some confusion.
        MemoryBank.appOpts = new AppOptions(); // Default starting point for a new user.
        AppTreePanel.theInstance = null;
        appTreePanel = TestUtil.getTheAppTreePanel();
        appTreePanel.restoringPreviousSelection = true; // This helps keep the thread count down.
        notifier = (TestUtil) appTreePanel.optionPane;

        theTree = appTreePanel.getTree(); // Usage here means no unit test needed for getTree().
        rootNode = (DefaultMutableTreeNode) theTree.getModel().getRoot();
    } // end BeforeAll method meFirst()

    @AfterAll
    static void meLast() {
        theTree = null;
        appTreePanel = null;
    }

    // 1.  A new user's tree matches the default AppOptions (including no 'Search Results' branch).
    @Test
    @Order(1)
    void testDefaultBranchStates() {
        DefaultMutableTreeNode dmtn;
        int theIndex;

        // Archives
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.ARCHIVES.toString());
        theIndex = rootNode.getIndex(dmtn);
        Assertions.assertEquals(0, theIndex);
        Assertions.assertFalse(dmtn.getAllowsChildren());

        // Goals
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.GOALS.toString());
        theIndex = rootNode.getIndex(dmtn);
        Assertions.assertEquals(1, theIndex);
        Assertions.assertTrue(dmtn.getAllowsChildren());
        Assertions.assertTrue(dmtn.isLeaf());

        // Events
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.UPCOMING_EVENTS.toString());
        theIndex = rootNode.getIndex(dmtn);
        Assertions.assertEquals(2, theIndex);
        Assertions.assertTrue(dmtn.getAllowsChildren());
        Assertions.assertTrue(dmtn.isLeaf());

        // Views
        dmtn = BranchHelperInterface.getNodeByName(rootNode, "Views");
        theIndex = rootNode.getIndex(dmtn);
        Assertions.assertEquals(3, theIndex);
        Assertions.assertTrue(dmtn.getAllowsChildren());

        // Notes
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.NOTES.toString());
        theIndex = rootNode.getIndex(dmtn);
        Assertions.assertEquals(4, theIndex);
        Assertions.assertTrue(dmtn.getAllowsChildren());

        // To Do Lists
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.TODO_LISTS.toString());
        theIndex = rootNode.getIndex(dmtn);
        Assertions.assertEquals(5, theIndex);
        Assertions.assertTrue(dmtn.getAllowsChildren());
        Assertions.assertTrue(dmtn.isLeaf());

        // Search Results
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.SEARCH_RESULTS.toString());
        Assertions.assertNull(dmtn);
    }

    // 2.  After the first search is conducted, the result appears under a new 'Search Results' branch.
    @Test
    @Order(2)
    void testFirstSearch() {
        DefaultMutableTreeNode dmtn;
        int theIndex;
        appTreePanel.restoringPreviousSelection = true; // This helps keep the thread count down.
        appTreePanel.doSearch(new SearchPanel());

        // Verify that after the above search, we now have a Search Results branch and a
        //   Search Result leaf under it, and that leaf is the current selection.
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.SEARCH_RESULTS.toString());
        theIndex = rootNode.getIndex(dmtn);
        Assertions.assertEquals(6, theIndex);
        Assertions.assertEquals(MemoryBank.appOpts.theSelectionRow, 7);

        // Save the reference to the individual search result for later - deletion.
        theSearchResultPath = theTree.getSelectionPath();

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }


    // 3.  When there are search results, selection of the branch will bring up its branch editor.
    @Test
    @Order(3)
    void testSearchResultsBranchEditor() {
        // Select the Search Results Branch
        DefaultMutableTreeNode dmtn;
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.SEARCH_RESULTS.toString());
        TreeNode[] pathToRoot = dmtn.getPath();
        theTree.setSelectionPath(new TreePath(pathToRoot));

        // Now check that the viewport is showing a branch editor.
        // If it is showing a branch editor at all, we can accept that it's the right one.
        JComponent theContent = appTreePanel.getViewedComponent();
        Assertions.assertTrue(theContent instanceof TreeBranchEditor);

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }

    // 4.  When the last available search result is deleted, the node reverts to a leaf vs a branch.
    @Test
    @Order(4)
    void testDeleteLastSearchResult() {
        // Select the previously created SearchResultGroupPanel
        theTree.setSelectionPath(theSearchResultPath);
        appTreePanel.deleteGroup();

        DefaultMutableTreeNode dmtn;
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.SEARCH_RESULTS.toString());
        Assertions.assertTrue(dmtn.isLeaf() && dmtn.getAllowsChildren());

        // A deletion also does a 'close', so that method has been tested by now as well.
        Assertions.assertEquals(-1, MemoryBank.appOpts.theSelectionRow);

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }

    // 5.  When the 'Search Results' node is a leaf, selection will lead to a new Search.
    //     But this is a bit of a corner case, in that we can only have a Search Results leaf
    //     as a result of deleting all other Search Results so that there is no Editor for the branch.
    @Test
    @Order(5)
    void testDoSearchFromLeaf() throws InterruptedException {
        // Get the path to the Search Results Branch
        DefaultMutableTreeNode dmtn;
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.SEARCH_RESULTS.toString());
        TreeNode[] pathToRoot = dmtn.getPath();

        // The handler for selecting the Search Results branch should kick off a new search, with a call to
        //   appTreePanel.prepareSearch(), which uses the Notifier to present the SearchPanel to the user.
        //   So first we pre-set the notifier to cancel the search, then Select the Search Results branch.
        int theAnsr = JOptionPane.CANCEL_OPTION;
        notifier.setTheAnswerInt(theAnsr);
        appTreePanel.restoringPreviousSelection = false; // We WANT it to present the Search Panel.
        theTree.setSelectionPath(new TreePath(pathToRoot));
        // But doing it this way, it happens in a separate thread, so we need to give it some time to show the dialog.
        Thread.sleep(500);

        // Now we need to verify that the notifier's 'message' contained the SearchPanel.
        Object theMessage = notifier.getTheMessage();
        // But the message is just a JPanel, with the SearchPanel as its 'center' component.
        Assertions.assertTrue(theMessage instanceof JPanel);
        JPanel thePanel = (JPanel) theMessage;
        BorderLayout bl = (BorderLayout) thePanel.getLayout();
        SearchPanel sp = (SearchPanel) bl.getLayoutComponent(thePanel, BorderLayout.CENTER);
        Assertions.assertNotNull(sp);

        // Since we cancelled the search, we cannot leave the selection row here....  it would be too high upon app
        //   restart.  And this cannot be 'fixed' in AppTreePanel because the logic there is written such that the
        //   search was being conducted due to a user request where their current selection may have been any other
        //   valid node.  This is an after-test data problem, so we need to fix it here.
        // Apparently, a 'too high' restored tree row is just ignored, so there is no handling of a selection event
        //   and menus do not get managed; the restarted app shows ALL menus.
        theTree.clearSelection();
        MemoryBank.appOpts.theSelectionRow = -1;
        MemoryBank.appOpts.theSelection = "No Selection";

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }

    // 6.  The selection of the Goals branch with no content will result in a 'new group' name prompt.
    @Test
    @Order(6)
    void testNewGoalFromLeaf() throws InterruptedException {
        // Get the path to the Goals Branch
        DefaultMutableTreeNode dmtn;
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.GOALS.toString());
        TreeNode[] pathToRoot = dmtn.getPath();

        // The handler for the branch will make a call to appTreePanel.addNewGroup(), which uses the Notifier to
        //   take in the new group name.
        notifier.setTheAnswerString("NewGoal");
        appTreePanel.restoringPreviousSelection = false; // We WANT it to present the input dialog
        theTree.setSelectionPath(new TreePath(pathToRoot));
        // But doing it this way, it happens in a separate thread, so we need to give it some time to show the dialog.
        Thread.sleep(1000);

        // Now we need to verify that we have 'landed' on a new Goal
        Assertions.assertTrue(appTreePanel.getTheNoteGroupPanel() instanceof GoalGroupPanel);

        // Add a note and save the group, so that we will have a persisted group later, in test #9
        appTreePanel.getTheNoteGroupPanel().myNoteGroup.appendNote(new NoteData("this is it."));
        appTreePanel.getTheNoteGroupPanel().myNoteGroup.saveNoteGroup();

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }

    // 7.  The selection of the Events branch with no content will result in a 'new group' name prompt.
    @Test
    @Order(7)
    void testNewEventFromLeaf() throws InterruptedException {
        // Get the path to the Goals Branch
        DefaultMutableTreeNode dmtn;
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.UPCOMING_EVENTS.toString());
        TreeNode[] pathToRoot = dmtn.getPath();

        // The handler for the branch will make a call to appTreePanel.addNewGroup(), which uses the Notifier to
        //   take in the new group name.
        notifier.setTheAnswerString("NewEvent");
        appTreePanel.restoringPreviousSelection = false; // We WANT it to present the input dialog
        theTree.setSelectionPath(new TreePath(pathToRoot));
        // But doing it this way, it happens in a separate thread, so we need to give it some time to show the dialog.
        Thread.sleep(1000);

        // Now we need to verify that we have 'landed' on a new Event
        Assertions.assertTrue(appTreePanel.getTheNoteGroupPanel() instanceof EventNoteGroupPanel);

        // Add a note and save the group, so that we will have a persisted group later, in test #10
        appTreePanel.getTheNoteGroupPanel().myNoteGroup.appendNote(new NoteData("this is it."));
        appTreePanel.getTheNoteGroupPanel().myNoteGroup.saveNoteGroup();

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }


    // 8.  The selection of the To Do Lists branch with no content will result in a 'new group' name prompt.
    @Test
    @Order(8)
    void testNewTodoListFromLeaf() throws InterruptedException {
        // Get the path to the Goals Branch
        DefaultMutableTreeNode dmtn;
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.TODO_LISTS.toString());
        TreeNode[] pathToRoot = dmtn.getPath();

        // The handler for the branch will make a call to appTreePanel.addNewGroup(), which uses the Notifier to
        //   take in the new group name.
        notifier.setTheAnswerString("NewTodoList");
        appTreePanel.restoringPreviousSelection = false; // We WANT it to present the input dialog
        theTree.setSelectionPath(new TreePath(pathToRoot));
        // But doing it this way, it happens in a separate thread, so we need to give it some time to show the dialog.
        Thread.sleep(1000);

        // Now we need to verify that we have 'landed' on a new To Do List
        Assertions.assertTrue(appTreePanel.getTheNoteGroupPanel() instanceof TodoNoteGroupPanel);

        // Add a note and save the group, so that we will have a persisted group later, in test #11
        appTreePanel.getTheNoteGroupPanel().myNoteGroup.appendNote(new NoteData("this is it."));
        appTreePanel.getTheNoteGroupPanel().myNoteGroup.saveNoteGroup();

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }


    // 9.  The selection of the Goals branch with content will result in a Branch Editor view.
    @Test
    @Order(9)
    void testGoalBranchEditor() throws InterruptedException {
        // Get the path to the Goals Branch
        DefaultMutableTreeNode dmtn;
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.GOALS.toString());
        TreeNode[] pathToRoot = dmtn.getPath();
        theTree.setSelectionPath(new TreePath(pathToRoot));

        Thread.sleep(500); // Needs a bit more time to construct the branch editor.

        // Now check that the viewport is showing a branch editor.
        // If it is showing a branch editor at all, we will accept that it's the right one.
        JComponent theContent = appTreePanel.getViewedComponent();
        Assertions.assertTrue(theContent instanceof TreeBranchEditor);

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }


    // 10.  The selection of the Upcoming Events branch with content will result in a Branch Editor view.
    @Test
    @Order(10)
    void testEventBranchEditor() throws InterruptedException {
        // Get the path to the Goals Branch
        DefaultMutableTreeNode dmtn;
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.UPCOMING_EVENTS.toString());
        TreeNode[] pathToRoot = dmtn.getPath();
        theTree.setSelectionPath(new TreePath(pathToRoot));

        Thread.sleep(500); // Needs a bit more time to construct the branch editor.

        // Now check that the viewport is showing a branch editor.
        // If it is showing a branch editor at all, we will accept that it's the right one.
        JComponent theContent = appTreePanel.getViewedComponent();
        Assertions.assertTrue(theContent instanceof TreeBranchEditor);

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }


    // 11.  The selection of the To Do Lists branch with content will result in a Branch Editor view.
    @Test
    @Order(11)
    void testTodoListBranchEditor() throws InterruptedException {
        // Get the path to the Goals Branch
        DefaultMutableTreeNode dmtn;
        dmtn = BranchHelperInterface.getNodeByName(rootNode, DataArea.TODO_LISTS.toString());
        TreeNode[] pathToRoot = dmtn.getPath();
        theTree.setSelectionPath(new TreePath(pathToRoot));

        Thread.sleep(500); // Needs a bit more time to construct the branch editor.

        // Now check that the viewport is showing a branch editor.
        // If it is showing a branch editor at all, we will accept that it's the right one.
        JComponent theContent = appTreePanel.getViewedComponent();
        Assertions.assertTrue(theContent instanceof TreeBranchEditor);

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }

}
