import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

// The purpose of this testing is to verify that in all circumstances where persisted data is
// retrieved and used in recreating NoteGroup panels, the Last Modified date & time is
// reproduced faithfully and is not updated to current values solely due to Panel constructions
// and initial component data-setting.  It goes on to further verify that the LMDs remain
// unchanged when the data is pulled back out of the Panel and reinserted into its NoteGroup.
// There is one test for each 'flavor' of data/group.

class LastModRetentionTest {

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

    // GoalGroupPanel
    @Test
    void testGoalGroupPanel() {
        // These two Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-07-19T06:44:39.018+04:00[Europe/Samara]";
        String zdtLastModNote = "2020-10-14T14:57:07.577+04:00[Europe/Samara]";

        // Load the Panel under test.
        GoalGroupPanel goalGroupPanel = new GoalGroupPanel("Retire");

        // Since we haven't made any change, for this test only we need to call this NoteGroupPanel method
        //   directly, rather than via preClosePanel().
        goalGroupPanel.getPanelData();  // Pull the data out of the Panel and put it back into the NoteGroup.
        // Note that operationally, getPanelData does not get called if the groupChanged flag is false.

        // Access the group (not the Panel) and snag the Last Mod date of its properties.
        GoalGroup theNoteGroup = (GoalGroup) goalGroupPanel.myNoteGroup;
        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;

        // Compare the expected info with the freshly loaded info.
        Assertions.assertEquals(zdtLastModGroup, groupLastMod);
    }

    // EventNoteGroupPanel
    @Test
    void testEventNoteGroupPanel() {
        // These two Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-10-18T16:39:47.296+04:00[Europe/Samara]";
        String zdtLastModNote = "2020-10-18T08:11:29.641+04:00[Europe/Samara]";

        // Load the Panel under test.
        EventNoteGroupPanel noteGroupPanel = new EventNoteGroupPanel("Reverse Links");

        // Since we haven't made any change, for this test only we need to call this NoteGroupPanel method
        //   directly, rather than via preClosePanel().
        noteGroupPanel.getPanelData();  // Pull the data out of the Panel and put it back into the NoteGroup.
        // Operationally, getPanelData does not get called if the groupChanged flag is false.

        // Access the group (not the Panel) and snag the Last Mod dates for both the Group and its first note.
        EventNoteGroup theNoteGroup = (EventNoteGroup) noteGroupPanel.myNoteGroup;
        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        NoteData theNoteData = (EventNoteData) theNoteGroup.noteGroupDataVector.elementAt(0);
        String noteLastMod = theNoteData.zdtLastModString;

        // Compare the expected info with the freshly loaded info.
        Assertions.assertEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertEquals(zdtLastModNote, noteLastMod);
    }

    // DayNoteGroupPanel
    @Test
    void testDayNoteGroupPanel() {
        // These two Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-10-18T16:41:05.010+04:00[Europe/Samara]";
        String zdtLastModNote = "2020-10-17T11:12:47.718+04:00[Europe/Samara]";

        // Load the Panel under test.
        DayNoteGroupPanel noteGroupPanel = new DayNoteGroupPanel();
        LocalDate theDate = LocalDate.of(2020, 10, 18);
        noteGroupPanel.setDate(theDate);

        // Since we haven't made any change, for this test only we need to call this NoteGroupPanel method
        //   directly, rather than via preClosePanel().
        noteGroupPanel.getPanelData();  // Pull the data out of the Panel and put it back into the NoteGroup.
        // Operationally, getPanelData does not get called if the groupChanged flag is false.

        // Access the group (not the Panel) and snag the Last Mod dates for both the Group and its first note.
        DayNoteGroup theNoteGroup = (DayNoteGroup) noteGroupPanel.myNoteGroup;
        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        NoteData theNoteData = (DayNoteData) theNoteGroup.noteGroupDataVector.elementAt(0);
        String noteLastMod = theNoteData.zdtLastModString;

        // Compare the expected info with the freshly loaded info.
        Assertions.assertEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertEquals(zdtLastModNote, noteLastMod);
    }

    // MonthNoteGroupPanel
    @Test
    void testMonthNoteGroupPanel() {
        // These two Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-10-18T16:44:08.260+04:00[Europe/Samara]";
        String zdtLastModNote = "2020-10-16T10:12:45.718+04:00[Europe/Samara]";

        // Load the Panel under test.
        MonthNoteGroupPanel noteGroupPanel = new MonthNoteGroupPanel();
        LocalDate theDate = LocalDate.of(2020, 10, 18);
        noteGroupPanel.setDate(theDate);

        // Since we haven't made any change, for this test only we need to call this NoteGroupPanel method
        //   directly, rather than via preClosePanel().
        noteGroupPanel.getPanelData();  // Pull the data out of the Panel and put it back into the NoteGroup.
        // Operationally, getPanelData does not get called if the groupChanged flag is false.

        // Access the group (not the Panel) and snag the Last Mod dates for both the Group and its first note.
        NoteGroup theNoteGroup = noteGroupPanel.myNoteGroup;
        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        NoteData theNoteData = (NoteData) theNoteGroup.noteGroupDataVector.elementAt(0);
        String noteLastMod = theNoteData.zdtLastModString;

        // Compare the expected info with the freshly loaded info.
        Assertions.assertEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertEquals(zdtLastModNote, noteLastMod);
    }

    // YearNoteGroupPanel
    @Test
    void testYearNoteGroupPanel() {
        // These two Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-10-18T16:41:33.337+04:00[Europe/Samara]";
        String zdtLastModNote = "2020-10-15T09:12:45.718+04:00[Europe/Samara]";

        // Load the Panel under test.
        YearNoteGroupPanel noteGroupPanel = new YearNoteGroupPanel();
        LocalDate theDate = LocalDate.of(2020, 10, 18);
        noteGroupPanel.setDate(theDate);

        // Since we haven't made any change, for this test only we need to call this NoteGroupPanel method
        //   directly, rather than via preClosePanel().
        noteGroupPanel.getPanelData();  // Pull the data out of the Panel and put it back into the NoteGroup.
        // Operationally, getPanelData does not get called if the groupChanged flag is false.

        // Access the group (not the Panel) and snag the Last Mod dates for both the Group and its first note.
        NoteGroup theNoteGroup = noteGroupPanel.myNoteGroup;
        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        NoteData theNoteData = (NoteData) theNoteGroup.noteGroupDataVector.elementAt(0);
        String noteLastMod = theNoteData.zdtLastModString;

        // Compare the expected info with the freshly loaded info.
        Assertions.assertEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertEquals(zdtLastModNote, noteLastMod);
    }

    // TodoNoteGroupPanel
    @Test
    void testTodoNoteGroupPanel() {
        // These two Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-10-17T10:47:28.590+04:00[Europe/Samara]";
        String zdtLastModNote = "2020-10-17T10:47:50.533+04:00[Europe/Samara]";

        // Load the Panel under test.
        TodoNoteGroupPanel noteGroupPanel = new TodoNoteGroupPanel("Preparations");

        // Since we haven't made any change, for this test only we need to call this NoteGroupPanel method
        //   directly, rather than via preClosePanel().
        noteGroupPanel.getPanelData();  // Pull the data out of the Panel and put it back into the NoteGroup.
        // Operationally, getPanelData does not get called if the groupChanged flag is false.

        // Access the group (not the Panel) and snag the Last Mod dates for both the Group and its first note.
        TodoNoteGroup todoNoteGroup = (TodoNoteGroup) noteGroupPanel.myNoteGroup;
        String groupLastMod = todoNoteGroup.getGroupProperties().zdtLastModString;
        TodoNoteData theNoteData = (TodoNoteData) todoNoteGroup.noteGroupDataVector.elementAt(0);
        String noteLastMod = theNoteData.zdtLastModString;

        // Compare the expected info with the freshly loaded info.
        Assertions.assertEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertEquals(zdtLastModNote, noteLastMod);
    }

    // SearchResultGroupPanel
    @Test
    void testSearchResultGroupPanel() {
        // These two Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-12-04T06:16:35.037+04:00[Europe/Samara]";
        String zdtLastModNote = "2020-10-17T11:17:00.008+04:00[Europe/Samara]";

        // Load the Panel under test.
        SearchResultGroupPanel noteGroupPanel = new SearchResultGroupPanel("day");

        // Since we haven't made any change, for this test only we need to call this NoteGroupPanel method
        //   directly, rather than via preClosePanel().
        noteGroupPanel.getPanelData();  // Pull the data out of the Panel and put it back into the NoteGroup.
        // Operationally, getPanelData does not get called if the groupChanged flag is false.

        // Access the group (not the Panel) and snag the Last Mod dates for both the Group and its first note.
        SearchResultGroup theNoteGroup = (SearchResultGroup) noteGroupPanel.myNoteGroup;
        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        SearchResultData theNoteData = (SearchResultData) theNoteGroup.noteGroupDataVector.elementAt(0);
        String noteLastMod = theNoteData.zdtLastModString;

        // Compare the expected info with the freshly loaded info.
        Assertions.assertEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertEquals(zdtLastModNote, noteLastMod);
    }

}