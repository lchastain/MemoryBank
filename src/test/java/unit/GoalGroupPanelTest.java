import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.util.UUID;

// Test Plan for menu item states:
//  1.  Create a new Panel via a Tree selection.  No changes, so verify that menu items are disabled.
//  2.  Make change to one tab, verify its menu items are enabled while other tab menu items remain disabled.


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class GoalGroupPanelTest {
    static AppTreePanel appTreePanel;
    static TestUtil testUtil;
    static JTree theTree;
    static TreePath theGoalTreePath;

    GoalGroupPanel goalPanel;
    JMenuItem jmi;
    UUID theId;

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.userEmail = "test.user@lcware.net";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(FileDataAccessor.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        // Load the user's options
        AppOptions.loadOpts();

        testUtil = new TestUtil();
        AppTreePanel.theInstance = null; // We don't want to 'inherit' one that was previously used.
        appTreePanel = TestUtil.getTheAppTreePanel(); // This sets a 'Test' Notifier
        appTreePanel.restoringPreviousSelection = true; // This disables the threading.
        theTree = appTreePanel.getTree();
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) theTree.getModel().getRoot();
        DefaultMutableTreeNode goalBranchNode = BranchHelperInterface.getNodeByName(rootNode, "Goals");
        DefaultMutableTreeNode dmtn = BranchHelperInterface.getNodeByName(goalBranchNode, "Graduate");
        theGoalTreePath = AppUtil.getTreePath(dmtn);
    } // end static

    @AfterAll
    static void afterAll() {
        AppTreePanel.theInstance = null; // Do not let settings that are made here get into other tests.
    }


    @Test
    @Order(1)
    // Baseline setting - Ensure we are on the 'right page', no changes, menu items disabled.
    public void menuDisablementTest() {
        theTree.clearSelection(); // To ensure the upcoming selection action since it might already be there.
        theTree.setSelectionPath(theGoalTreePath);

        // Get the current panel, ensure that it is a Goal panel.
        // No need to assert this; it passes if the cast does not throw an exception.
        goalPanel = (GoalGroupPanel) appTreePanel.getTheNoteGroupPanel();

        // Get the ID of this panel, to verify that later accesses are to this one and not a newly constructed one.
        theId = goalPanel.myNoteGroup.myProperties.instanceId;

        // Verify that we started on tab 0.
        Assertions.assertEquals(0, goalPanel.theTabbedPane.getSelectedIndex());

        // get the menu, check its items' enablement status.
        TestUtil testUtil = (TestUtil) appTreePanel.optionPane;
        jmi = testUtil.getMenuItem("Goal", "Save");
        System.out.println("Retrieved menu item text: " + jmi.getText());
        Assertions.assertFalse(jmi.isEnabled());

        // Change the tab, clear the Tree selection, re-select and verify we get the same Panel that we had earlier.
        goalPanel.theTabbedPane.setSelectedIndex(2); // Change to the Milestones tab.
        theTree.clearSelection();
        theTree.setSelectionPath(theGoalTreePath);
        goalPanel = (GoalGroupPanel) appTreePanel.getTheNoteGroupPanel();
        Assertions.assertEquals(theId, goalPanel.myNoteGroup.myProperties.instanceId);
        Assertions.assertEquals(2, goalPanel.theTabbedPane.getSelectedIndex());
        System.out.println("Goal Panel is on tab: " + goalPanel.theTabbedPane.getSelectedIndex());

        jmi = testUtil.getMenuItem("Goal", "Undo All");
        System.out.println("Retrieved menu item text: " + jmi.getText());
        Assertions.assertFalse(jmi.isEnabled());
    }

    @Test
    @Order(2)
    // Make changes on tab 2, ensure its menu items are enabled while tab 1 menu items remain disabled.
    public void menuEnablementTest() {
        // Make a change to Milestone data, ensure Save menu item is enabled.
        MilestoneNoteData milestoneNoteData = new MilestoneNoteData();
        milestoneNoteData.setNoteString("new milestone");
        goalPanel.theMilestoneNoteGroupPanel.appendNote(milestoneNoteData);
        jmi = testUtil.getMenuItem("Goal", "Save");
        System.out.println("Retrieved menu item text: " + jmi.getText());
        Assertions.assertTrue(jmi.isEnabled());

        // Clear the Tree selection and re-select the Goal, to ensure that the panel returns on the same
        //   tab and that its menu items remain enabled.
        theTree.clearSelection();
        theTree.setSelectionPath(theGoalTreePath);
        goalPanel = (GoalGroupPanel) appTreePanel.getTheNoteGroupPanel();
        Assertions.assertEquals(2, goalPanel.theTabbedPane.getSelectedIndex());
        jmi = testUtil.getMenuItem("Goal", "Undo All");
        System.out.println("Retrieved menu item text: " + jmi.getText());
        Assertions.assertTrue(jmi.isEnabled());

        // Change to tab 1 (the Log) and verify that its menu items are also enabled.
        // This represents a change in intended behavior.  Previously, tabs were meant
        //   to be individually handled, but when a Goal is saved, current thinking is
        //   that ALL tabs should be saved.  When that was not true, changes were being
        //   lost, hence the behavioral change and different outcome for this test.
        goalPanel.theTabbedPane.setSelectedIndex(1); // Change to the Log tab.
        jmi = testUtil.getMenuItem("Goal", "Save");
        System.out.println("Retrieved menu item text: " + jmi.getText());
        Assertions.assertTrue(jmi.isEnabled());
    }

    @Test
    @Order(3)
    public void coverageTest() {
        // Off-the-rails tests, to get NoteGroup coverage in places that are
        //   otherwise inappropriate to this class test.
        GoalGroupPanel goalGroup = new GoalGroupPanel("Graduate");
        goalGroup.myNoteGroup.setNotes(null);
        goalGroup.myNoteGroup.myProperties = null;
        goalGroup.myNoteGroup.myGroupInfo = new GroupInfo("badName", GroupType.DAY_NOTES);
        goalGroup.myNoteGroup.getGroupProperties();
        goalGroup.myNoteGroup.myProperties = null;
        goalGroup.myNoteGroup.myGroupInfo = new GroupInfo("badName", GroupType.MONTH_NOTES);
        goalGroup.myNoteGroup.getGroupProperties();
        goalGroup.myNoteGroup.myProperties = null;
        goalGroup.myNoteGroup.myGroupInfo = new GroupInfo("badName", GroupType.YEAR_NOTES);
        goalGroup.myNoteGroup.getGroupProperties();
    }

}