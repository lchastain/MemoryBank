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
    void testReuseYearNote() {
    }

    @Test
    void testReuseDayNote() {
        DayNoteGroup dayNoteGroup = new DayNoteGroup();

        // Set the day to one where the note content can be used for this test.
        Calendar theCal = new GregorianCalendar(2018, Calendar.JANUARY, 8);
        dayNoteGroup.setChoice(theCal.getTime());

        // Get a component to clear, and clear it.
        DayNoteComponent dayNoteComponent2 = dayNoteGroup.getNoteComponent(2);
        dayNoteComponent2.clear();

        // Now reuse that component
        DayNoteData dayNoteData = new DayNoteData();
        dayNoteData.setNoteString("This is it!");
        dayNoteComponent2.setNoteData(dayNoteData);

        // Verify that reusing the line did not add a new one (SCR0065)
        int highest = dayNoteGroup.vectGroupData.size();
        Assertions.assertEquals(highest, 5);

        // Note: during dev of this test a new problem (caused by the initial fix for SCR0065)
        // was seen - that the reused note was not getting a new timestamp or default icon.
        // The fix was modified so that a reused note is effectively reinitialized and now that
        // problem does not occur.  This part of the fix has been verified manually and will
        // continue to be seen regularly.  Not that it doesn't deserve an automated test, just
        // not sure how to do that here yet, AND - it would require closer examination of the
        // time/date based data, that at this time is to be renovated throughout the entire
        // app.  That 'renovation' / upgrade is going to take higher priority than any / all
        // tests, because most of those tests (and probably this one) will have to change to
        // keep up with the upgrade to Java 8+ date / time classes.  9/15/2019

        // TODO - automated verification of a new timestamp and default icon.

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
