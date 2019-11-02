import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SearchBranchHelperTest {
    private SearchBranchHelper searchBranchHelper;

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
        MemoryBank.loadOpts();
    }


    @BeforeEach
    void setUp() {
        NoteGroupKeeper theSearchResultsKeeper = new NoteGroupKeeper();
        DefaultMutableTreeNode trunk = new DefaultMutableTreeNode("App");

        // Create a default model based on the 'App' node, and create a tree from that model.
        DefaultTreeModel treeModel = new DefaultTreeModel(trunk);
        JTree tree = new JTree(treeModel);

        searchBranchHelper = new SearchBranchHelper(tree, theSearchResultsKeeper);
        searchBranchHelper.optionPane = new TestUtil();
    }

    @AfterEach
    void tearDown() {
    }

    @Test  // a 'bad' selected node to rename will fail.  We don't allow rename of a 'parent' node.
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

        // Bad - bad characters in the name
        theResult = searchBranchHelper.allowRenameTo("blarg: the movie");
        Assertions.assertFalse(theResult);

        // The 'happy path'
        theResult = searchBranchHelper.allowRenameTo("blarg");
        Assertions.assertTrue(theResult);
    }

    @Test
    void testDoApply() {
    }
}