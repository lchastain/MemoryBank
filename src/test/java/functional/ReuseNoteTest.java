import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;

class ReuseNoteTest {

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
    }

    @Test
    void testReuseDayNote() {
        DayNoteGroup dayNoteGroup = new DayNoteGroup();

        // Set the day to one where the note content can be used for this test.
        // (we know there are 10 notes on this day).
        Calendar theCal = new GregorianCalendar(2010, Calendar.JUNE, 8);
        dayNoteGroup.setChoice(theCal.getTime());

        // Get a component to clear, and clear it.
        // This note has a time, extendeed note, and icon
        DayNoteComponent dayNoteComponent4 = dayNoteGroup.getNoteComponent(4);
        dayNoteComponent4.clear();

        // Now reuse that component
        DayNoteData dayNoteData = new DayNoteData();
        dayNoteData.setNoteString("This is it!");
        dayNoteComponent4.setNoteData(dayNoteData);

        // Verify that reusing the line did not add a new one (SCR0065)
        int highest = dayNoteGroup.vectGroupData.size();
        Assertions.assertEquals(highest, 10);

        // Note: during dev of this test a new problem (caused by the initial fix for SCR0065)
        // was seen - that the reused note was not getting a new timestamp or default icon.
        // The fix was modified so that a reused note is effectively reinitialized and now that
        // problem does not occur.
        //
        // These two assertions verify that the associated underlying data was correctly altered.
        // They do not check the visual aspects of the result but unit tests for the DayNoteComponent
        // (not yet written) will verify that the underlying data is used in the display of the info.
        // (and for now - it all looks ok)
        assert dayNoteData.getTimeOfDayString() != null;
        assert dayNoteData.getIconFileString() == null;

        // Not needed, unless you run only this test and then want to examine the result.
        dayNoteGroup.preClose();

    }

    // TODO - More tests  (other NoteGroup types; probably only need one from Month/Year)
//    @Test
//    void testReuseTodoNote() {
//    }
//
//    @Test
//    void testReuseEventNote() {
//    }

}
