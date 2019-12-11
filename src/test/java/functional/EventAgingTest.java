import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.time.LocalDate;
import java.util.ArrayList;

class EventAgingTest {

    @BeforeAll
    static void setup() {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.debug = true;

        // Remove any pre-existing Test data
        File testDataLoc = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testDataLoc);
        } catch (Exception e) {
            System.out.println("ignored Exception: " + e.getMessage());
        }

    }

    // Verify that events are copied to the right Days and that it happens only once
    // regardless of how many times 'refresh()' is called.  SCR0029, SCR0082
    @Test
    @SuppressWarnings("rawtypes")
    void testAgeOffStopAfter() throws Exception {
        // Retrieve fresh test data from test resources.
        // We don't want a full set of data for these tests; just the UpcomingEvents.
        File newname = new File(EventNoteGroup.basePath() + "event_holidays.json");
        String fileName = "EventAgingTest/Age4Times&End.json";
        File testFile = FileUtils.toFile(EventNoteGroup.class.getResource(fileName));
        FileUtils.copyFile(testFile, newname);

        // the setup - After our BeforeAll there should be no Day data, at all.  Verify this, to some extent.
        File theFolder = new File(MemoryBank.userDataHome + File.separatorChar + "2018");
        Assertions.assertFalse(theFolder.exists()); // if no directory then no files either.
        // This one missing directory gives us a high confidence that there are none.

        // the aging (constructor calls refresh, which calls ageEvents)
        EventNoteGroup eventNoteGroup = new EventNoteGroup("holidays");
        Assertions.assertTrue(theFolder.exists()); // this verifies SCR0029

        // Verify four data files (and the directories to hold them) were created.
        LocalDate ld1 = LocalDate.of(2018, 10, 13);
        LocalDate ld2 = LocalDate.of(2018, 11, 10);
        LocalDate ld3 = LocalDate.of(2018, 12, 8);
        LocalDate ld4 = LocalDate.of(2019, 1, 12);
        String filename1 = AppUtil.findFilename(ld1, "D");
        Assertions.assertFalse(filename1.isEmpty());
        String filename2 = AppUtil.findFilename(ld2, "D");
        Assertions.assertFalse(filename2.isEmpty());
        String filename3 = AppUtil.findFilename(ld3, "D");
        Assertions.assertFalse(filename3.isEmpty());
        String filename4 = AppUtil.findFilename(ld4, "D");
        Assertions.assertFalse(filename4.isEmpty());

        // Read the last file, verify one note inside
        File theLastFile = new File(filename4);
        Object[] theData = AppUtil.loadNoteGroupData(theLastFile);
        Assertions.assertNotNull(theData);
        Assertions.assertEquals(1, ((ArrayList) theData[0]).size());

        // More refreshing.  If the event had not been aged, data files will grow.
        eventNoteGroup.refresh();
        eventNoteGroup.refresh();

        // Reload the data file and verify that it did not grow.
        theData = AppUtil.loadNoteGroupData(theLastFile);
        Assertions.assertNotNull(theData);
        Assertions.assertEquals(1, ((ArrayList) theData[0]).size());
    }

}
