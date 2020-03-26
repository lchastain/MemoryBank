import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class AddTodoListTest {

    // NOTE that this test does not involve the AppTreePanel or the application tree.  Therefore
    // if you run the app as the test user after running this test you will not see the new list
    // unless you go to the TodoLists node and select the list for inclusion in the tree.

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
        AppOptions.loadOpts();
    }

    @Test
    void testAddNewTodoList() {
        // Construct a TodoNoteGroup for a list that is known to NOT exist.
        String strGroupFilename = "Assemble The Avengers";
        TodoNoteGroup tng = new TodoNoteGroup(strGroupFilename);
        Assertions.assertNotNull(tng);

        // Initializations
        TodoNoteData tnd = new TodoNoteData();
        String s1 = "Ask Tony Stark to get in touch with Iron Man";
        String s2 = "Contact Bruce Banner - be nice";
        String s3 = "Call on the Asgard line, ask for Thor";
        NoteComponent tnc1 = tng.getNoteComponent(0);
        NoteComponent tnc2 = tng.getNoteComponent(1);
        NoteComponent tnc3 = tng.getNoteComponent(2);

        // Verify that initially, no file exists
        strGroupFilename = tng.getGroupFilename();
        File theFile = new File(strGroupFilename);
        Assertions.assertFalse(theFile.exists());

        // Set some data to save to the new list.
        // We use the copy-constructor of a new NoteData or else we would only
        // be setting the notestring of the same one in all three components.
        tnd.setNoteString(s1);
        tnd.setPriority(2);
        tnd.setStatus(2);
        // For TodoNoteComponents, setNoteData also sets initialized to true.
        tng.getNoteComponent(0).setTodoNoteData(new TodoNoteData(tnd));
        tng.activateNextNote(0);

        tnd.setNoteString(s2);
        tnd.setPriority(3);
        tnd.setStatus(3);
        tng.getNoteComponent(1).setTodoNoteData(new TodoNoteData(tnd));
        tng.activateNextNote(1);

        tnd.setNoteString(s3);
        tnd.setPriority(4);
        tnd.setStatus(4);
        tng.getNoteComponent(2).setTodoNoteData(new TodoNoteData(tnd));
        tng.activateNextNote(2);

        // Now, verify that the data did get into the group.
        Assertions.assertEquals(tnc1.getNoteData().noteString, s1);
        Assertions.assertEquals(tnc2.getNoteData().noteString, s2);
        Assertions.assertEquals(tnc3.getNoteData().noteString, s3);

        // Now save the group
        tng.preClose();

        // Verify that there is now a file for it
        Assertions.assertTrue(theFile.exists());
    }
}