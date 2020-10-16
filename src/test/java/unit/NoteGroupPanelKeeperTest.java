import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

//@Disabled("Looking for a rogue 'working' dialog")
class NoteGroupPanelKeeperTest {
    TodoNoteGroupPanel theNoteGroup1;
    TodoNoteGroupPanel theNoteGroup2;
    NoteGroupPanelKeeper noteGroupPanelKeeper;

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
        noteGroupPanelKeeper = new NoteGroupPanelKeeper();
        theNoteGroup1 = new TodoNoteGroupPanel("Get New Job");
        noteGroupPanelKeeper.add(theNoteGroup1);
        theNoteGroup2 = new TodoNoteGroupPanel("New Car Shopping");
        noteGroupPanelKeeper.add(theNoteGroup2);
    }

    @Test
    void testGet() {
        // This one can be found.
        NoteGroupPanel theNoteGroupPanel = noteGroupPanelKeeper.get("Get New Job");
        Assertions.assertNotNull(theNoteGroupPanel);
        assertEquals(theNoteGroupPanel, theNoteGroup1);

        // This one will not be found.
        theNoteGroupPanel = noteGroupPanelKeeper.get("Get Lost");
        Assertions.assertNull(theNoteGroupPanel);
    }

    @Test
    void testRemove() {
        // This verifies our starting point, plus shows that 'add' worked correctly.
        assertEquals(2, noteGroupPanelKeeper.size());

        // This one can be found.
        noteGroupPanelKeeper.remove("New Car Shopping");
        assertEquals(1, noteGroupPanelKeeper.size());

        // This one will not be found.
        noteGroupPanelKeeper.remove("Get Lost");
        assertEquals(1, noteGroupPanelKeeper.size());
    }
}