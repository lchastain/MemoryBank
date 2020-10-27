import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

class ReuseNoteTest {

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve the test data for this class from test resources.
        String fileName = "ReuseNoteTest";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);
    }

    @Test
    void testReuseDayNote() {
        DayNoteGroupPanel dayNoteGroupPanel = new DayNoteGroupPanel();

        // Set the day to one where the note content can be used for this test.
        // (we know there are 10 notes for this user on 8 June 2010).
        dayNoteGroupPanel.setDate(LocalDate.of(2019, 6, 8));

        // Get a component to clear, and clear it.
        // This note has a time, an extended note, and an icon
        DayNoteComponent dayNoteComponent4 = dayNoteGroupPanel.getNoteComponent(4);
        dayNoteComponent4.clear();

        // Now reuse that component
        DayNoteData dayNoteData = new DayNoteData();
        dayNoteData.setNoteString("This is it!");
        dayNoteComponent4.setNoteData(dayNoteData);

        // Verify that reusing the line did not setNotes a new one (SCR0065)
        int highest = dayNoteGroupPanel.myNoteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(highest, 10);

        // Note: during dev of this test a new problem (caused by the initial fix for SCR0065)
        // was seen - that the reused note was not getting a new timestamp or default icon.
        // The fix was modified so that a reused note is effectively reinitialized and now that
        // problem does not occur.
        //
        // These two assertions verify that the associated underlying data was correctly altered.
        // They do not check the visual aspects of the result but unit tests for the DayNoteComponent
        // (not yet written) will verify that the underlying data is used in the display of the info.
        // (and for now - it all looks ok, so this satisfies the testing needed for the additional
        // issues that turned up while fixing SCR0065).
        assert dayNoteData.getTimeOfDayString() != null;  // A new time was set then the note was reused.
        assert dayNoteData.getIconFileString() == null;  // null means the default icon will be used.

        // Not needed, unless you run only this test and then want to examine the result.
        dayNoteGroupPanel.preClosePanel();

    }

    @Test
    void testReuseTodoNote() {
        TodoNoteGroupPanel todoNoteGroupPanel = new TodoNoteGroupPanel("Ten Things To Do");
        TodoNoteComponent todoNoteComponent = todoNoteGroupPanel.getNoteComponent(2);
        Assertions.assertEquals("Thing Three", todoNoteComponent.getNoteData().noteString);

        // Clear the component
        todoNoteComponent.clear();
        Assertions.assertEquals("", todoNoteComponent.getNoteData().noteString);

        // Reuse the component
        TodoNoteData todoNoteData = new TodoNoteData();
        todoNoteData.setNoteString("The third thang.");
        todoNoteComponent.setNoteData(todoNoteData);
        Assertions.assertEquals("The third thang.", todoNoteComponent.getNoteData().getNoteString());

        // Verify that reusing the line did not setNotes a new one (SCR0065)
        int highest = todoNoteGroupPanel.myNoteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(highest, 10);

        // Not needed, unless you run only this test and then want to examine the result.
        todoNoteGroupPanel.preClosePanel();
    }

    // Note 10/21/2019 1410 - The fix for SCR0065 was needed/done in common code, so having a 'reuse' test
    // for every note type is way overkill.  Already we have two, and that will be enough.  Incomplete
    // test development leftovers are below but could be removed.

//    @Test
//    void testReuseEventNote() throws Exception {
//        EventNoteGroup eventNoteGroup = new EventNoteGroup();
//        EventNoteComponent eventNoteComponent = (EventNoteComponent) eventNoteGroup.getNoteComponent(2);
//        Assertions.assertEquals("Valentine's Day", eventNoteComponent.getNoteData().noteString);
//    }
//
//    @Test
//    void testReuseYearNote() {
//    }
}
