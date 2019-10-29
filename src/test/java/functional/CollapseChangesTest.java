import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;

// Too much going on here to come up with a short name for it - what we are testing comes
// from the complaint in SCR0004, that when a Todo list with unsaved changes was in
// focus and then the Todo Lists node was collapsed, the user would see the 'Unable
// to delete file' error dialog.  Well, we didn't want it to be removed anyway.  Now
// the code is reworked to solve the issue so we'll just go thru those steps and verify
// that we get no warning but do preserve the changes, and the file remains.

class CollapseChangesTest {
    private AppTreePanel atp;
    private JTree theTree;
    private String viewsNodeName = "Views";
    private String notesNodeName = "Notes";
    private String todolistsNodeName = "To Do Lists";
    private String searchesNodeName = "Search Results";

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources.
        // This test user has a rich set of data, includes Search Results and Todo Lists
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        MemoryBank.loadOpts();
    }

    @Test
    void testFileNotRemovedOnCollapse() {
        // The AppTreePanel is what creates the Tree for us.
        atp = new AppTreePanel(new JFrame(), MemoryBank.appOpts);
        theTree = atp.getTree();

        // Get the TreePath to the collapsible node
        DefaultTreeModel theTreeModel = (DefaultTreeModel) theTree.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        DefaultMutableTreeNode todolistsNode = TestUtil.getTreeNodeForString(theRoot, todolistsNodeName);
        TreePath todolistsPath = AppUtil.getPath(todolistsNode);

        // Select the list we will use
        DefaultMutableTreeNode theListNode = TestUtil.getTreeNodeForString(theRoot, "Get New Job");
        TreePath thelistPath = AppUtil.getPath(theListNode);
        theTree.setSelectionPath(thelistPath);

        // Allow some time for the new selection to 'take'
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Get the corresponding TodoNoteGroup
        TodoNoteGroup todoNoteGroup = (TodoNoteGroup) atp.getTheNoteGroup();

        // Make an edit to the list
        TodoNoteComponent todoNoteComponent3 = todoNoteGroup.getNoteComponent(3);
        todoNoteComponent3.clear();

        // Now collapse the node
        theTree.collapsePath(todolistsPath);

        // Expecting no warning here; a popup dialog will ruin that
        // expectation and definitely counts as a test fail.
        // And aside from that, we should verify that our file still exists -
        String theFilePath = MemoryBank.userDataHome + File.separatorChar + "TodoLists" + File.separatorChar;
        String theFileName = "todo_Get New Job.json";
        File theFile = new File(theFilePath + theFileName);
        Assertions.assertTrue(theFile.exists());
    }

}