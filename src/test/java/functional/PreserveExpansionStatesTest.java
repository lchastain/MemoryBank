import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

// This test is to verify that the state of the expandable / collapsible
// nodes of the tree can be restored.  SCR0054.

//@Disabled
class PreserveExpansionStatesTest {
    private AppTreePanel atp;
    private JTree theTree;
    private final String viewsNodeName = "Views";
    private final String notesNodeName = "Notes";
    private final String todolistsNodeName = "To Do Lists";
    private final String searchesNodeName = "Search Results";

    @BeforeAll
    static void setup() throws IOException {
        System.out.println("PreserveExpansionStatesTest Test");

        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception e) {
            System.out.println("ignored Exception: " + e.getMessage());
        }

        // Retrieve a fresh set of test data from test resources.
        // This test user has a rich set of data, includes Search Results and Todo Lists
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();

        // There were problems when trying to instantiate a single AppTreePanel here;
        // the framework's efforts to isolate test data interfered with our expectation
        // that changes that atp made to its own appOpts would be reflected back into
        // the appOpts in MemoryBank.  The atp instances had to be separated on a per-
        // test basis.
    }

    @AfterEach
    // Because there are two instances of atp - this is needed when running this class as part of a larger suite.
    // Otherwise, the 'working' dialog gets intermittently zombified and when that happens in the running of a
    // full suite, the todoItemFocusTest will fail.
    void notWorking() throws InterruptedException {
        atp = null;
        Thread.sleep(500);
    }


    @Test
    void testCollapsedNodesPreserved() {
        // The AppTreePanel is what creates the Tree for us.
        AppTreePanel.theInstance = null;
        atp = TestUtil.getTheAppTreePanel();
        theTree = atp.getTree();

        // Get the TreePaths to the collapsible nodes
        DefaultTreeModel theTreeModel = (DefaultTreeModel) theTree.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        DefaultMutableTreeNode viewsNode = BranchHelperInterface.getNodeByName(theRoot, viewsNodeName);
        DefaultMutableTreeNode notesNode = BranchHelperInterface.getNodeByName(theRoot, notesNodeName);
        DefaultMutableTreeNode todolistsNode = BranchHelperInterface.getNodeByName(theRoot, todolistsNodeName);
        DefaultMutableTreeNode searchResultsNode = BranchHelperInterface.getNodeByName(theRoot, searchesNodeName);
        TreePath viewsPath = AppUtil.getTreePath(viewsNode);
        TreePath notesPath = AppUtil.getTreePath(notesNode);
        TreePath todolistsPath = AppUtil.getTreePath(todolistsNode);
        TreePath searchesPath = AppUtil.getTreePath(searchResultsNode);

        // Collapse the paths.  This may not actually represent a change, depending on
        // the current state of the tree after the user's last session, but this test is
        // about what gets preserved, not about the toggle operation.
        theTree.collapsePath(viewsPath);
        theTree.collapsePath(notesPath);
        theTree.collapsePath(todolistsPath);
        theTree.collapsePath(searchesPath);

        // Get the tree state from the tree, and save it in the application options file.
        atp.updateAppOptions(false);
        AppOptions.saveOpts();

        // Load the file again -
        AppOptions.loadOpts();
        atp = null;

        // And verify that the 'expanded' flags are false.
        assertFalse(MemoryBank.appOpts.viewsExpanded);
        assertFalse(MemoryBank.appOpts.notesExpanded);
        assertFalse(MemoryBank.appOpts.todoListsExpanded);
        assertFalse(MemoryBank.appOpts.searchesExpanded);
    }

    @Test
    void testExpandedNodesPreserved() {
        // The AppTreePanel is what creates the Tree for us.
        atp = new AppTreePanel(new JFrame(), MemoryBank.appOpts);
        theTree = atp.getTree();

        // Get the TreePath to the Search Results
        DefaultTreeModel theTreeModel = (DefaultTreeModel) theTree.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        DefaultMutableTreeNode viewsNode = BranchHelperInterface.getNodeByName(theRoot, viewsNodeName);
        DefaultMutableTreeNode notesNode = BranchHelperInterface.getNodeByName(theRoot, notesNodeName);
        DefaultMutableTreeNode todolistsNode = BranchHelperInterface.getNodeByName(theRoot, todolistsNodeName);
        DefaultMutableTreeNode searchResultsNode = BranchHelperInterface.getNodeByName(theRoot, searchesNodeName);
        TreePath viewsPath = AppUtil.getTreePath(viewsNode);
        TreePath notesPath = AppUtil.getTreePath(notesNode);
        TreePath todolistsPath = AppUtil.getTreePath(todolistsNode);
        TreePath searchesPath = AppUtil.getTreePath(searchResultsNode);

        // Expand the paths.  This may not actually represent a change, depending on
        // the current state of the tree after the user's last session, but this test is
        // about what gets preserved, not about the toggle operation.
        theTree.expandPath(viewsPath);
        theTree.expandPath(notesPath);
        theTree.expandPath(todolistsPath);
        theTree.expandPath(searchesPath);

        // Get the tree state from the tree, and save it in the application options file.
        atp.updateAppOptions(false);
        AppOptions.saveOpts();

        // Load the file again -
        AppOptions.loadOpts();
        atp = null;

        // And verify that the 'expanded' flag for Search Results is true.
        assertTrue(MemoryBank.appOpts.viewsExpanded);
        assertTrue(MemoryBank.appOpts.notesExpanded);
        assertTrue(MemoryBank.appOpts.todoListsExpanded);
        assertTrue(MemoryBank.appOpts.searchesExpanded);
    }
}