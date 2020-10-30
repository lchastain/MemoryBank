import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

class BranchHelperTest {
    private DefaultMutableTreeNode searches;

    // Note that we configure our instance of a Helper for Search Results, but it could have
    // just as easily been for Todo Lists or any other editable branch.  But we chose searches
    // because that's the flavor of the test data that we will use for the 'doApply' testing.
    private BranchHelper searchBranchHelper;

    @BeforeAll
    static void meFirst() throws IOException {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();
    }


    @BeforeEach
    void setUp() {
        NoteGroupPanelKeeper theSearchResultsKeeper = new NoteGroupPanelKeeper();
        DefaultMutableTreeNode trunk = new DefaultMutableTreeNode("App");
        searches = new DefaultMutableTreeNode("Search Results");
        trunk.add(searches);
        searches.add(new DefaultMutableTreeNode("20190927161325"));

        // Create a default model based on the 'App' node, and create a tree from that model.
        DefaultTreeModel treeModel = new DefaultTreeModel(trunk);
        JTree tree = new JTree(treeModel);

        searchBranchHelper = new BranchHelper(tree, theSearchResultsKeeper, BranchHelper.AreaName.SEARCH);
        searchBranchHelper.setNotifier(new TestUtil());
    }

    @AfterEach
    void tearDown() {
    }

    @Test  // a 'bad' selected node for rename will fail.  We don't allow rename of a 'parent' node.
    void testAllowRenameFrom() {
        DefaultMutableTreeNode theNode = new DefaultMutableTreeNode("Test Branch", true);
        boolean theResult = searchBranchHelper.allowRenameFrom(theNode);
        Assertions.assertFalse(theResult);
    }

    @Test
    void testAllowRenameTo() {
        // The 'allow' methods are separate but the framework always calls 'from' first
        // and then if true, calls 'to'.  The implementation relies on this and so this
        // test also needs to call both, in the right order, with a 'true' result coming
        // from the 'renameFrom' call.
        DefaultMutableTreeNode theNode = new DefaultMutableTreeNode("Test Leaf", false);
        boolean theResult = searchBranchHelper.allowRenameFrom(theNode);
        Assertions.assertTrue(theResult);

        // Ok, got thru the renameFrom and set a 'good' value for the allowTo method
        // to check against.  Now get on with all the 'bad' names.

        // Bad - same as 'from'
        theResult = searchBranchHelper.allowRenameTo("Test Leaf");
        Assertions.assertFalse(theResult);

        // Bad - the new name evaporates
        theResult = searchBranchHelper.allowRenameTo("     ");
        Assertions.assertFalse(theResult);

        // Bad - ends in '.json'
        theResult = searchBranchHelper.allowRenameTo("blarg.json");
        Assertions.assertFalse(theResult);

        // Bad - too long
        theResult = searchBranchHelper.allowRenameTo("123456789012345678901234567890123");
        Assertions.assertFalse(theResult);

        // Bad - illegal characters in the name
        theResult = searchBranchHelper.allowRenameTo("blarg: the movie");
        Assertions.assertFalse(theResult);

        // The 'happy path'
        theResult = searchBranchHelper.allowRenameTo("blarg");
        Assertions.assertTrue(theResult);
    }

    // Note that the majority of the handling for Selection events (including deselections) is done by
    // the TreeBranchEditor which is tested elsewhere; the smaller part of selection events that is
    // handled by the Helper is common to the same code that handles renames and deletions, so we are
    // only going to test the renames and deletions, below.

    @Test
    void testDoApply() {
        ArrayList<NodeChange> changeList = new ArrayList<>();
        changeList.add(new NodeChange("20191029073938", "new name"));
        changeList.add(new NodeChange("20190927161325", NodeChange.REMOVED));

        // This gets us the coverage; may still want to verify that changes were made,
        // to either the tree, the filesystem, or both.
        // But may not want to spend cycles verifying filesystem changes, when that data
        // storage methodology is not optimal and is under consideration for migration to a DB.
        searchBranchHelper.doApply(searches, changeList);
    }

    // The NodeChange class is so small, why not go ahead and include the only other test needed for it?
    // This way, no additional test class needed for unit testing NodeChange.
    @Test
    void testNodeChangeToString() {
        NodeChange nodeChange;
        String theString;
        nodeChange = new NodeChange("test", NodeChange.SELECTED);
        theString = nodeChange.toString();
        Assertions.assertNotNull(theString);
        nodeChange = new NodeChange("test", NodeChange.MARKED);
        theString = nodeChange.toString();
        Assertions.assertNotNull(theString);
        nodeChange = new NodeChange("test", NodeChange.UNMARKED);
        theString = nodeChange.toString();
        Assertions.assertNotNull(theString);
        nodeChange = new NodeChange("test", NodeChange.MOVED);
        theString = nodeChange.toString();
        Assertions.assertNotNull(theString);
        nodeChange = new NodeChange("test", NodeChange.DESELECTED);
        theString = nodeChange.toString();
        Assertions.assertNotNull(theString);
        nodeChange = new NodeChange("test", NodeChange.RENAMED);
        theString = nodeChange.toString();
        Assertions.assertNotNull(theString);
        nodeChange = new NodeChange("test", 101);
        theString = nodeChange.toString();
        Assertions.assertNotNull(theString);
    }
}