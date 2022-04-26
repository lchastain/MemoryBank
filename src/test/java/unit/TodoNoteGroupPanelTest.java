import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.time.LocalDate;

class TodoNoteGroupPanelTest {
    private static TodoNoteGroupPanel todoNoteGroup;
    private static TestUtil testUtil;

    @BeforeAll
    static void beforeAll() throws IOException {
        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        TestUtil.getTheAppTreePanel();

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        todoNoteGroup = new TodoNoteGroupPanel("Get New Job");
        testUtil = new TestUtil();
        NoteGroupPanel.optionPane = testUtil;
    }

    @Test
    // This test looks at three code paths; one essentially a no-op, one
    // considered a user-fail, and finally the saveAs() method's happy path.
    void testSaveAs() {
        // New name accepted but it wasn't a change -
        String theNewName = "Get New Job";
        testUtil.setTheAnswerString(theNewName);
        testUtil.setNotifyCount(0); // arbitrary, just needs to be known.
        todoNoteGroup.saveAs();
        // Verify that the 'tell me the new name' dialog was shown.
        Assertions.assertEquals(1, testUtil.getNotifyCount());

        // New name collision - it already exists.
        theNewName = "New Car Shopping";
        testUtil.setTheAnswerString(theNewName);
        testUtil.setNotifyCount(5); // arbitrary, just needs to be known.
        todoNoteGroup.saveAs();
        // Verify that two dialogs were shown.
        Assertions.assertEquals(7, testUtil.getNotifyCount());

        // The Happy Path
        theNewName = "blarg";
        testUtil.setTheAnswerString(theNewName);
        todoNoteGroup.saveAs();
        Assertions.assertEquals(theNewName, todoNoteGroup.myNoteGroup.getGroupProperties().getGroupName());
        Assertions.assertTrue(todoNoteGroup.myNoteGroup.exists());
    }

    @Test
    void testSetOptions() throws NoSuchMethodException {
        // We're just here for the coverage; options are stored with the list and list
        // storage/retrieval is already tested above.  The actual value of options
        // and whether or not they had an effect can be tested in the functional testing.

        // Preserve the current properties; needed by the 'saveAs' test.
        GroupProperties originalTodoGroupProperties = todoNoteGroup.myNoteGroup.myProperties;

        // Make some options and set them differently than the ones our test user has now.
        TodoGroupProperties tlp = new TodoGroupProperties("noname");
        tlp.showPriority = false;

        // Testing this method will cause it to try to get User input.  But we have changed
        // its Notifier, so that process is automated for this test.
        // Tell the Notifier to call a method on the TodoOpts ('message') prior to returning.
        // Get the method from TodoOpts that the Notifier will call, to change the options.
        Method newMethod = TodoOpts.class.getDeclaredMethod("setNewProperties", TodoGroupProperties.class);
        testUtil.setTheMethod(newMethod);
        testUtil.setTheMessage(tlp); // Provide the needed method parameter

        testUtil.setNotifyCount(0); // arbitrary, just needs to be known.
        todoNoteGroup.setOptions();
        Assertions.assertEquals(1, testUtil.getNotifyCount());

        // Restore the original properties.
        todoNoteGroup.myNoteGroup.setGroupProperties(originalTodoGroupProperties);
    }

    @Test
    void testDateSelected() {
        // Coverage, could use some more...
        todoNoteGroup.dateSelected(LocalDate.now());
    }

    @Test
    void testMerge() {
        // Just the coverage -
        testUtil.setTheAnswerString("New Car Shopping");
        todoNoteGroup.merge();
    }

    @Test
    void testPageNumberChanged() {
        // Just the coverage -
        todoNoteGroup.pageNumberChanged();
    }

//    @Test
//    // This one needs rework in the app - it appears to pop up TWO (identical) dialogs
//    // for the request, but our Notifier cannot handle this type of user interaction
//    // (yet).  Wrote a new SCR (0066).  Disabling this test for now.
//    void printList() {
//        todoNoteGroup.printList();
//    }

    @Test
    void testSorting() {
        // Just the coverage -
        todoNoteGroup.sortPriority(NoteGroupPanel.ASCENDING);
        todoNoteGroup.sortPriority(NoteGroupPanel.DESCENDING);
    }

 }