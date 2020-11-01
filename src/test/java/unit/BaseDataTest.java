import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.UUID;

class BaseDataTest {

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve the test data for this class from test resources.
        String fileName = "setup@testing.com";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);
    }

    // This will verify that the instanceIds do not change between Group instantiations.
    @Test
    void testInstanceId() {
        // These two Strings came from the known test data.
        String instanceIdGroup = "788f99d2-406a-45cb-b9b2-f28f7a138a94";
        String instanceIdNote = "0c0ec15a-eeb0-4ee4-9267-3baf54b05ee4";

        // First, load a group and snag the ID for both the Group and its first note.
        TodoNoteGroup todoNoteGroup = new TodoNoteGroup("Preparations");
        UUID groupId = todoNoteGroup.getGroupProperties().instanceId;
        TodoNoteData todoNoteData = (TodoNoteData) todoNoteGroup.noteGroupDataVector.elementAt(0);
        UUID noteId = todoNoteData.instanceId;

        Assertions.assertEquals(instanceIdGroup, groupId.toString());
        Assertions.assertEquals(instanceIdNote, noteId.toString());
    }

    @Test
    void testLastMod() {
        // These two Strings came from the known test data.
        String zdtLastModGroup = "2020-10-17T10:47:28.590+04:00[Europe/Samara]";
        String zdtLastModNote = "2020-10-17T10:47:50.533+04:00[Europe/Samara]";

        // First, load a group and snag the Last Mod dates for both the Group and its first note.
        TodoNoteGroup todoNoteGroup = new TodoNoteGroup("Preparations");
        String groupLastMod = todoNoteGroup.getGroupProperties().zdtLastModString;
        TodoNoteData todoNoteData = (TodoNoteData) todoNoteGroup.noteGroupDataVector.elementAt(0);
        String noteLastMod = todoNoteData.zdtLastModString;

        Assertions.assertEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertEquals(zdtLastModNote, noteLastMod);
    }

}