import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;

// Test Plan for menu item states:
//  1.  Create a new Panel via a Tree selection.  No changes, so verify that menu items are disabled.
//  2.  Make change to one tab, verify its menu items are enabled while other tab menu items remain disabled.

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CalendarNoteGroupPanelTest {
    static AppTreePanel appTreePanel;
    static TestUtil testUtil;
    static JTree theTree;
    static TreePath theCalendarNotesTreePath;

    TabbedCalendarNoteGroupPanel tabbedCalendarNoteGroupPanel;
    JMenuItem jmi;

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
        MemoryBank.appOpts.groupCalendarNotes = true; // But ensure that the Calendar Notes are grouped.

        testUtil = new TestUtil();
        AppTreePanel.theInstance = null; // We don't want to 'inherit' one that was previously used.
        appTreePanel = TestUtil.getTheAppTreePanel(); // This sets a 'Test' Notifier
        theTree = appTreePanel.getTree();
//        appTreePanel.groupCalendarNotes(true);
        theCalendarNotesTreePath = appTreePanel.calendarNotesPath;
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
        theTree.setSelectionPath(theCalendarNotesTreePath);

        // Get the TabbedCalendarNoteGroupPanel from appTreePanel.
        tabbedCalendarNoteGroupPanel = appTreePanel.theTabbedCalendarNoteGroupPanel;

        // Verify that we started on tab 0.
        Assertions.assertEquals(0, tabbedCalendarNoteGroupPanel.theTabbedPane.getSelectedIndex());

        // get the menu, check its items' enablement status.
        TestUtil testUtil = (TestUtil) appTreePanel.optionPane;
        jmi = testUtil.getMenuItem("Calendar Notes", "Save");
        System.out.println("Retrieved menu item text: " + jmi.getText());
        Assertions.assertFalse(jmi.isEnabled());

        // Change the tab, clear the Tree selection, re-select and verify we get the same Panel that we had earlier.
        tabbedCalendarNoteGroupPanel.theTabbedPane.setSelectedIndex(2); // Change to the Year Notes tab.
        theTree.clearSelection();
        theTree.setSelectionPath(theCalendarNotesTreePath);
        tabbedCalendarNoteGroupPanel =  appTreePanel.theTabbedCalendarNoteGroupPanel;
        Assertions.assertEquals(2, tabbedCalendarNoteGroupPanel.theTabbedPane.getSelectedIndex());
        System.out.println("CalendarNotes Panel is on tab: " + tabbedCalendarNoteGroupPanel.theTabbedPane.getSelectedIndex());

        jmi = testUtil.getMenuItem("Calendar Notes", "Undo All");
        System.out.println("Retrieved menu item text: " + jmi.getText());
        Assertions.assertFalse(jmi.isEnabled());
    }

    @Test
    @Order(2)
    // Make changes on tab 2, ensure its menu items are enabled while tab 1 menu items remain disabled.
    public void menuEnablementTest() {
        // Make a change to Milestone data, ensure Save menu item is enabled.
        NoteData yearNoteData = new NoteData();
        yearNoteData.setNoteString("new year note");
        tabbedCalendarNoteGroupPanel.theYearNoteGroupPanel.appendNote(yearNoteData);
        jmi = testUtil.getMenuItem("Calendar Notes", "Save");
        System.out.println("Retrieved menu item text: " + jmi.getText());
        Assertions.assertTrue(jmi.isEnabled());

        // Clear the Tree selection and re-select the Goal, to ensure that the panel returns on the same
        //   tab and that its menu items remain enabled.
        theTree.clearSelection();
        theTree.setSelectionPath(theCalendarNotesTreePath);
        tabbedCalendarNoteGroupPanel =  appTreePanel.theTabbedCalendarNoteGroupPanel;
        Assertions.assertEquals(2, tabbedCalendarNoteGroupPanel.theTabbedPane.getSelectedIndex());
        jmi = testUtil.getMenuItem("Calendar Notes", "Undo All");
        System.out.println("Retrieved menu item text: " + jmi.getText());
        Assertions.assertTrue(jmi.isEnabled());

        // Change to tab 1 (the Log) and verify that its menu items are still disabled.
        tabbedCalendarNoteGroupPanel.theTabbedPane.setSelectedIndex(1); // Change to the Log tab.
        jmi = testUtil.getMenuItem("Calendar Notes", "Save");
        System.out.println("Retrieved menu item text: " + jmi.getText());
        Assertions.assertFalse(jmi.isEnabled());
    }

}