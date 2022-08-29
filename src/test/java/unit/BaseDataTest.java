import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

// The big difference between the two tests here and all the ones in the LastModRetentionTest is that here we look
// at the BaseData via its NoteGroup to ensure that it comes in without change, whereas there we use the data in
// Panel construction, then scrape it back out of the Panel to examine it and verify that it is unchanged.

class BaseDataTest {

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.userEmail = "test.user@lcware.net";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(FileDataAccessor.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve the test data for this class from test resources.
        String fileName = "setup@testing.com";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);
    }

    // This will verify that instanceIds are correctly retrieved from storage.
    // As opposed to just generating a new one with each instantiation.
    @Test
    void testInstanceIdRetrieval() {
        // These two Strings came from the known test data.
        String instanceIdGroup = "788f99d2-406a-45cb-b9b2-f28f7a138a94";
        String instanceIdNote = "0c0ec15a-eeb0-4ee4-9267-3baf54b05ee4";

        // Load the group under test and snag the ID for both the Group and its first note.
        TodoNoteGroup todoNoteGroup = new TodoNoteGroup(new GroupInfo("Preparations", GroupType.TODO_LIST));
        UUID groupId = todoNoteGroup.getGroupProperties().instanceId;
        TodoNoteData todoNoteData = (TodoNoteData) todoNoteGroup.noteGroupDataVector.elementAt(0);
        UUID noteId = todoNoteData.instanceId;

        // Compare the expected info with the freshly loaded info.
        Assertions.assertEquals(instanceIdGroup, groupId.toString());
        Assertions.assertEquals(instanceIdNote, noteId.toString());
    }

    // This verifies that the Last Mod info is correctly retrieved from storage.
    // As opposed to just updating to current date & time
    @Test
    void testLastModRetrieval() {
        // These two Strings came from the known test data.
        String zdtLastModGroup = "2020-10-17T10:47:28.590+04:00[Europe/Samara]";
        String zdtLastModNote = "2020-10-17T10:47:50.533+04:00[Europe/Samara]";

        // Load the group under test and snag the Last Mod dates for both the Group and its first note.
        TodoNoteGroup todoNoteGroup = new TodoNoteGroup(new GroupInfo("Preparations", GroupType.TODO_LIST));
        String groupLastMod = todoNoteGroup.getGroupProperties().zdtLastModString;
        TodoNoteData todoNoteData = (TodoNoteData) todoNoteGroup.noteGroupDataVector.elementAt(0);
        String noteLastMod = todoNoteData.zdtLastModString;

        // Compare the expected info with the freshly loaded info.
        Assertions.assertEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertEquals(zdtLastModNote, noteLastMod);
    }

}