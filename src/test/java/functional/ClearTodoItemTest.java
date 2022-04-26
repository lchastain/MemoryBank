import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

// This test is to verify that a given Todo item can be cleared.
// This addresses the problem that was originally reported via
// SCRs 0047 and 0051.


class ClearTodoItemTest {

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        TestUtil.getTheAppTreePanel();

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

    }

    @Test
    void testClearTodoItem() {
        // Construct a TodoNoteGroup for a list that is known to exist.
        TodoNoteGroupPanel tng = new TodoNoteGroupPanel("Get New Job");
        Assertions.assertNotNull(tng);

        // Select the first item in the list
        TodoNoteComponent tnc = tng.getNoteComponent(0);
        Assertions.assertNotNull(tnc);

        // We 'know' this item - but verify it, now, by examining the visual component
        String theTask = tnc.noteTextField.getText();
        Assertions.assertEquals("Update Resume", theTask);
        TodoNoteComponent.PriorityButton thePriorityButton = tnc.getPriorityButton();
        String thePriorityString = thePriorityButton.getText();
        Assertions.assertEquals("1", thePriorityString);
        TodoNoteComponent.StatusButton theStatusButton = tnc.getStatusButton();
        int theStatus = theStatusButton.getStatus();
        Assertions.assertEquals(2, theStatus);

        // If the assertions to this point have passed then the underlying data for the
        // TodoNoteComponent - TodoNoteData - must also have the correct values.  We
        // don't need to look there as well.

        // So now - clear the component.
        tnc.clear();

        TodoNoteData tnd = (TodoNoteData) tnc.getNoteData();
        Assertions.assertNotNull(tnd);
        Assertions.assertEquals("", tnd.getNoteString());

        // Verify that the visual components accurately represent a 'cleared' state
        theTask = tnc.noteTextField.getText();
        Assertions.assertEquals("", theTask);
        thePriorityString = thePriorityButton.getText();
        Assertions.assertEquals("  ", thePriorityString); // Zero priority is indicated with a 'blank' in the Button's label.
        theStatus = theStatusButton.getStatus();
        Assertions.assertEquals(0, theStatus);
    }

}