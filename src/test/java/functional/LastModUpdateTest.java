import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

// The purpose of this testing is to verify that when a Note in a Panel is changed,
// the LMD of both the Group and that Note are updated, while other notes in the
// Panel are not affected.
//
// It is true that the verifications here are weak; we are only testing that the LMDs
//   have changed, not that they were updated to current date&time.  But really, if they
//   are different then what other value would they have gone to?  I am hereby admitting
//   that I am ASSuming that different == current.
// Ok, a bit more justification for the assumption - different COULD just be 'wrong', but
//   we get confidence that they are 'right' by virtue of the fact that the sister test
//   in this group (LastModRetentionTest) uses the same values that are in the persisted
//   data, so they can't be right there and then 'wrong' here; the conclusion then is that
//   they are either 'right' or 'updated'.

class LastModUpdateTest {

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

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
    // This test was more robust prior to the revamp of Goals; now, it needs reevaluation to determine if
    //    there is enough value-added to justify doing it.
    void testGoalGroupPanel() {
        // These Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-07-19T06:44:39.018+04:00[Europe/Samara]";

        // Load the Panel under test.
        GoalGroupPanel noteGroupPanel = new GoalGroupPanel("Retire");

        // Get handle to the Panel's NoteGroup.
        NoteGroup theNoteGroup = noteGroupPanel.myNoteGroup;

        // Emulating editor actions
        theNoteGroup.setGroupChanged(true);

        // Scrape the data back out of the Panel, persist the changed Group.
        noteGroupPanel.preClosePanel();

        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        System.out.println("Group LMD: " + groupLastMod);

        // Compare the expected info with the updated info.
        Assertions.assertNotEquals(zdtLastModGroup, groupLastMod);
    }

    // EventNoteGroupPanel
    @Test
    void testEventNoteGroupPanel() {
        // These Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-10-18T16:39:47.296+04:00[Europe/Samara]";
        String zdtLastModNote0 = "2020-10-17T11:12:45.718+04:00[Europe/Samara]";
        String zdtLastModNote1 = "2020-10-18T08:11:21.324+04:00[Europe/Samara]";

        // Load the Panel under test.
        EventNoteGroupPanel noteGroupPanel = new EventNoteGroupPanel("Reverse Links");

        // Get handles to the Panel's NoteGroup and first two notes.
        NoteGroup theNoteGroup = noteGroupPanel.myNoteGroup;
        EventNoteData theNote0Data = (EventNoteData) theNoteGroup.noteGroupDataVector.elementAt(0);
        EventNoteData theNote1Data = (EventNoteData) theNoteGroup.noteGroupDataVector.elementAt(1);

        // Emulating editor actions
        theNote0Data.setLocation("Right here");
        theNoteGroup.setGroupChanged(true);

        // Scrape the data back out of the Panel, persist the changed Group.
        noteGroupPanel.preClosePanel();

        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        System.out.println("Group LMD: " + groupLastMod);

        String note0LastMod = theNote0Data.zdtLastModString;
        System.out.println("Note 0 LMD: " + note0LastMod);

        String note1LastMod = theNote1Data.zdtLastModString;
        System.out.println("Note 1 LMD: " + note1LastMod);

        // Compare the expected info with the updated info.
        Assertions.assertNotEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertNotEquals(zdtLastModNote0, note0LastMod);
        Assertions.assertEquals(zdtLastModNote1, note1LastMod);
    }

    // DayNoteGroupPanel
    @Test
    void testDayNoteGroupPanel() {
        // These Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-10-18T16:41:05.010+04:00[Europe/Samara]";
        String zdtLastModNote0 = "2020-10-17T11:12:47.718+04:00[Europe/Samara]";
        String zdtLastModNote1 = "2020-10-17T11:14:28.743+04:00[Europe/Samara]";

        // Load the Panel under test.
        DayNoteGroupPanel noteGroupPanel = new DayNoteGroupPanel();
        LocalDate theDate = LocalDate.of(2020, 10, 18);
        noteGroupPanel.setDate(theDate);

        // Get handles to the Panel's NoteGroup and first two notes.
        NoteGroup theNoteGroup = noteGroupPanel.myNoteGroup;
        DayNoteData theNote0Data = (DayNoteData) theNoteGroup.noteGroupDataVector.elementAt(0);
        DayNoteData theNote1Data = (DayNoteData) theNoteGroup.noteGroupDataVector.elementAt(1);

        // Emulating editor actions
        theNote0Data.setNoteString("A brand new day.");
        theNoteGroup.setGroupChanged(true);

        // Scrape the data back out of the Panel, persist the changed Group.
        noteGroupPanel.preClosePanel();

        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        System.out.println("Group LMD: " + groupLastMod);

        String note0LastMod = theNote0Data.zdtLastModString;
        System.out.println("Note 0 LMD: " + note0LastMod);

        String note1LastMod = theNote1Data.zdtLastModString;
        System.out.println("Note 1 LMD: " + note1LastMod);

        // Compare the expected info with the updated info.
        Assertions.assertNotEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertNotEquals(zdtLastModNote0, note0LastMod);
        Assertions.assertEquals(zdtLastModNote1, note1LastMod);
    }

    // MonthNoteGroupPanel
    @Test
    void testMonthNoteGroupPanel() {
        // These Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-10-18T16:44:08.260+04:00[Europe/Samara]";
        String zdtLastModNote0 = "2020-10-16T10:12:45.718+04:00[Europe/Samara]";
        String zdtLastModNote1 = "2020-10-17T11:14:28.743+04:00[Europe/Samara]";

        // Load the Panel under test.
        MonthNoteGroupPanel noteGroupPanel = new MonthNoteGroupPanel();
        LocalDate theDate = LocalDate.of(2020, 10, 18);
        noteGroupPanel.setDate(theDate);

        // Get handles to the Panel's NoteGroup and first two notes.
        NoteGroup theNoteGroup = noteGroupPanel.myNoteGroup;
        NoteData theNote0Data = (NoteData) theNoteGroup.noteGroupDataVector.elementAt(0);
        NoteData theNote1Data = (NoteData) theNoteGroup.noteGroupDataVector.elementAt(1);

        // Emulating editor actions
        theNote0Data.setExtendedNoteString("Fantastic Extended Note");
        theNoteGroup.setGroupChanged(true);

        // Scrape the data back out of the Panel, persist the changed Group.
        noteGroupPanel.preClosePanel();

        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        System.out.println("Group LMD: " + groupLastMod);

        String note0LastMod = theNote0Data.zdtLastModString;
        System.out.println("Note 0 LMD: " + note0LastMod);

        String note1LastMod = theNote1Data.zdtLastModString;
        System.out.println("Note 1 LMD: " + note1LastMod);

        // Compare the expected info with the updated info.
        Assertions.assertNotEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertNotEquals(zdtLastModNote0, note0LastMod);
        Assertions.assertEquals(zdtLastModNote1, note1LastMod);
    }

    // YearNoteGroupPanel
    @Test
    void testYearNoteGroupPanel() {
        // These Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-10-18T16:41:33.337+04:00[Europe/Samara]";
        String zdtLastModNote0 = "2020-10-15T09:12:45.718+04:00[Europe/Samara]";
        String zdtLastModNote1 = "2020-10-17T11:14:28.743+04:00[Europe/Samara]";

        // Load the Panel under test.
        YearNoteGroupPanel noteGroupPanel = new YearNoteGroupPanel();
        LocalDate theDate = LocalDate.of(2020, 10, 18);
        noteGroupPanel.setDate(theDate);

        // Get handles to the Panel's NoteGroup and first two notes.
        NoteGroup theNoteGroup = noteGroupPanel.myNoteGroup;
        NoteData theNote0Data = (NoteData) theNoteGroup.noteGroupDataVector.elementAt(0);
        NoteData theNote1Data = (NoteData) theNoteGroup.noteGroupDataVector.elementAt(1);

        // Emulating editor actions
        theNote0Data.setSubjectString("Amazing new Subject");
        theNoteGroup.setGroupChanged(true);

        // Scrape the data back out of the Panel, persist the changed Group.
        noteGroupPanel.preClosePanel();

        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        System.out.println("Group LMD: " + groupLastMod);

        String note0LastMod = theNote0Data.zdtLastModString;
        System.out.println("Note 0 LMD: " + note0LastMod);

        String note1LastMod = theNote1Data.zdtLastModString;
        System.out.println("Note 1 LMD: " + note1LastMod);


        // Compare the expected info with the updated info.
        Assertions.assertNotEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertNotEquals(zdtLastModNote0, note0LastMod);
        Assertions.assertEquals(zdtLastModNote1, note1LastMod);
    }

    // TodoNoteGroupPanel
    @Test
    void testTodoNoteGroupPanel() {
        // These Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-10-17T10:47:28.590+04:00[Europe/Samara]";
        String zdtLastModNote0 = "2020-10-17T10:47:50.533+04:00[Europe/Samara]";
        String zdtLastModNote1 = "2020-10-17T10:47:55.207+04:00[Europe/Samara]";

        // Load the Panel under test.
        TodoNoteGroupPanel noteGroupPanel = new TodoNoteGroupPanel("Preparations");

        // Get handles to the Panel's NoteGroup and first two notes.
        NoteGroup theNoteGroup = noteGroupPanel.myNoteGroup;
        TodoNoteData theNote0Data = (TodoNoteData) theNoteGroup.noteGroupDataVector.elementAt(0);
        TodoNoteData theNote1Data = (TodoNoteData) theNoteGroup.noteGroupDataVector.elementAt(1);

        // Emulating editor actions
        theNote0Data.setPriority(5);
        theNoteGroup.setGroupChanged(true);

        // Scrape the data back out of the Panel, persist the changed Group.
        noteGroupPanel.preClosePanel();

        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        System.out.println("Group LMD: " + groupLastMod);

        String note0LastMod = theNote0Data.zdtLastModString;
        System.out.println("Note 0 LMD: " + note0LastMod);

        String note1LastMod = theNote1Data.zdtLastModString;
        System.out.println("Note 1 LMD: " + note1LastMod);

        // Compare the expected info with the updated info.
        Assertions.assertNotEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertNotEquals(zdtLastModNote0, note0LastMod);
        Assertions.assertEquals(zdtLastModNote1, note1LastMod);
    }

    // SearchResultGroupPanel
    // This one is 'special', in that we don't really care about the LMD of a SearchResult.
    // We do still care about the overall group, however.
    @Test
    void testSearchResultGroupPanel() {
        // These Strings came from the known persisted test data.
        String zdtLastModGroup = "2020-11-18T05:41:04.617+04:00[Europe/Samara]";
        String zdtLastModNote1 = "2020-10-18T08:11:15.931+04:00[Europe/Samara]";

        // Load the Panel under test.
        SearchResultGroupPanel noteGroupPanel = new SearchResultGroupPanel("day");

        // Get handles to the Panel's NoteGroup and the second note.
        NoteGroup theNoteGroup = noteGroupPanel.myNoteGroup;
        SearchResultData theNote1Data = (SearchResultData) theNoteGroup.noteGroupDataVector.elementAt(1);

        // Emulating editor actions (like a reordering note swap, maybe)
        theNoteGroup.setGroupChanged(true);

        // Scrape the data back out of the Panel, persist the changed Group.
        noteGroupPanel.preClosePanel();

        String groupLastMod = theNoteGroup.getGroupProperties().zdtLastModString;
        System.out.println("Group LMD: " + groupLastMod);

        String note1LastMod = theNote1Data.zdtLastModString;
        System.out.println("Note 1 LMD: " + note1LastMod);

        // Compare the expected info with the updated info.
        Assertions.assertNotEquals(zdtLastModGroup, groupLastMod);
        Assertions.assertEquals(zdtLastModNote1, note1LastMod);
    }

}