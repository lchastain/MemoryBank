import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class ClearTodoItemTest {

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
    void testClearTodoItem() {
        // Construct a TodoNoteGroup for a list that is known to exist.
        TodoNoteGroup tng = new TodoNoteGroup("Get New Job");
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
        // should only need to check those after the 'clear'.

        // So now - clear it.
        tnc.clear();

        // Verify that the underlying data object still exists but has been cleared out.
        TodoNoteData tnd = (TodoNoteData) tnc.getNoteData();
        Assertions.assertNotNull(tnd);
        Assertions.assertEquals("", tnd.getNoteString());
        Assertions.assertEquals(0, tnd.getPriority());
        Assertions.assertEquals(0, tnd.getStatus());

        // Verify that the visual components accurately represent the internal data
        theTask = tnc.noteTextField.getText();
        Assertions.assertEquals("", theTask);
        thePriorityString = thePriorityButton.getText();
        Assertions.assertEquals("  ", thePriorityString); // Zero priority is indicated with a 'blank' in the Button's label.
        theStatus = theStatusButton.getStatus();
        Assertions.assertEquals(0, theStatus);
    }

}