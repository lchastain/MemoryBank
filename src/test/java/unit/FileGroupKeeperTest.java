import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class FileGroupKeeperTest {
    TodoNoteGroup theNoteGroup1;
    TodoNoteGroup theNoteGroup2;
    FileGroupKeeper fileGroupKeeper;

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.debug = true;

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception ignore){}

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);
    }


    @BeforeEach
    void setUp() {
        // These steps also take care of coverage for the 'add' method.
        fileGroupKeeper = new FileGroupKeeper();
        theNoteGroup1 = new TodoNoteGroup("Get New Job");
        fileGroupKeeper.add(theNoteGroup1);
        theNoteGroup2 = new TodoNoteGroup("New Car Shopping");
        fileGroupKeeper.add(theNoteGroup2);
    }

    @Test
    void testGet() {
        // This one can be found.
        NoteGroup theNoteGroup = (NoteGroup) fileGroupKeeper.get("Get New Job");
        Assertions.assertNotNull(theNoteGroup);
        assertEquals(theNoteGroup, theNoteGroup1);

        // This one will not be found.
        theNoteGroup = (NoteGroup) fileGroupKeeper.get("Get Lost");
        Assertions.assertNull(theNoteGroup);
    }

    @Test
    void testRemove() {
        // This verifies our starting point, plus shows that 'add' worked correctly.
        assertEquals(2, fileGroupKeeper.size());

        // This one can be found.
        fileGroupKeeper.remove("New Car Shopping");
        assertEquals(1, fileGroupKeeper.size());

        // This one will not be found.
        fileGroupKeeper.remove("Get Lost");
        assertEquals(1, fileGroupKeeper.size());
    }
}