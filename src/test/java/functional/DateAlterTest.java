import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

// The purpose of this testing is to verify that data and Panels for Calendar-type Groups are cleared out
//   when the date changes, and the appropriate new data is loaded in, when found.
//
// For this test the 'setup' area has three consecutive notes for each Calendar type.
//  (although upon coding the tests, discovered that apparently only two consecutive notes would have sufficed).
//
// And upon examination of the areas covered, do not see any need to continue with adding tests for the
// Month or Year, since the relevant code under test is all in common areas.  We are already at point 'overkill'.

class DateAlterTest {
    CalendarNoteGroupPanel noteGroupPanel;

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
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);
    }

    // DayNoteGroupPanel Backward       Test steps:
    // 1.  Change from Panel without data to one with data:  15 Mar 2018 --> 14 Mar 2018
    // 2.  Change from Panel with data to another with data: 13 Mar 2018 --> 12 Mar 2018
    // 3.  Change from Panel with data to one without data:  12 Mar 2018 --> 11 Mar 2018
    // 4.  Change from Panel without data to another without data: 11 Mar 2018 --> 10 Mar 2018
    @Test
    void testDayNoteGroupPanelBackward() {
        DayNoteGroup theNoteGroup;
        int groupSize;

        // 0.  Load the Panel under test with the empty day and verify that it is empty.
        noteGroupPanel = new DayNoteGroupPanel();
        LocalDate theDate = LocalDate.of(2018, 3, 15);
        noteGroupPanel.setDate(theDate);
        theNoteGroup = (DayNoteGroup) noteGroupPanel.myNoteGroup;
        groupSize = theNoteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(0, groupSize);
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Thursday, March 15, 2018", noteGroupPanel.getTitle());

        // 1.  Change from Panel without data to one with data:  15 Mar 2018 --> 14 Mar 2018
        noteGroupPanel.setOneBack(ChronoUnit.DAYS);
        noteGroupPanel.updateGroup();
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Wednesday, March 14, 2018", noteGroupPanel.getTitle());
        // Access the group verify that we have loaded the expected data.
        groupSize = theNoteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(3, groupSize);
        NoteData theNoteData = (DayNoteData) theNoteGroup.noteGroupDataVector.elementAt(1);
        //System.out.println("[" + theNoteData.noteString + "]");
        Assertions.assertEquals("date changes", theNoteData.noteString);

        // Now we skip one (also has data but that shouldn't matter)
        noteGroupPanel.setOneBack(ChronoUnit.DAYS);
        noteGroupPanel.updateGroup();
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Tuesday, March 13, 2018", noteGroupPanel.getTitle());

        // 2.  Change from Panel with data to another with data: 13 Mar 2018 --> 12 Mar 2018
        noteGroupPanel.setOneBack(ChronoUnit.DAYS);
        noteGroupPanel.updateGroup();
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Monday, March 12, 2018", noteGroupPanel.getTitle());
        // Re-acquire the NoteData for the element we are examining.
        theNoteData = (DayNoteData) theNoteGroup.noteGroupDataVector.elementAt(1);
        Assertions.assertEquals("increases and decreases", theNoteData.noteString);
        //System.out.println("[" + theNoteData.noteString + "]");

        // 3.  Change from Panel with data to one without data:  12 Mar 2018 --> 11 Mar 2018
        noteGroupPanel.setOneBack(ChronoUnit.DAYS);
        noteGroupPanel.updateGroup();
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Sunday, March 11, 2018", noteGroupPanel.getTitle());
        groupSize = theNoteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(0, groupSize);

        // 4.  Change from Panel without data to another without data: 11 Mar 2018 --> 10 Mar 2018
        noteGroupPanel.setOneBack(ChronoUnit.DAYS);
        noteGroupPanel.updateGroup();
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Saturday, March 10, 2018", noteGroupPanel.getTitle());
        groupSize = theNoteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(0, groupSize);
    }

    // DayNoteGroupPanel Forward       Test steps:
    // 1.  Change from Panel without data to one with data:  11 Mar 2018 --> 12 Mar 2018
    // 2.  Change from Panel with data to another with data: 13 Mar 2018 --> 14 Mar 2018
    // 3.  Change from Panel with data to one without data:  14 Mar 2018 --> 15 Mar 2018
    // 4.  Change from Panel without data to another without data: 15 Mar 2018 --> 16 Mar 2018
    @Test
    void testDayNoteGroupPanelForward() {
        DayNoteGroup theNoteGroup;
        int groupSize;

        // 0.  Load the Panel under test with the empty day and verify that it is empty.
        noteGroupPanel = new DayNoteGroupPanel();
        LocalDate theDate = LocalDate.of(2018, 3, 11);
        noteGroupPanel.setDate(theDate);
        theNoteGroup = (DayNoteGroup) noteGroupPanel.myNoteGroup;
        groupSize = theNoteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(0, groupSize);
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Sunday, March 11, 2018", noteGroupPanel.getTitle());

        // 1.  Change from Panel without data to one with data:  15 Mar 2018 --> 14 Mar 2018
        noteGroupPanel.setOneForward(ChronoUnit.DAYS);
        noteGroupPanel.updateGroup();
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Monday, March 12, 2018", noteGroupPanel.getTitle());
        // Access the group verify that we have loaded the expected data.
        groupSize = theNoteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(3, groupSize);
        NoteData theNoteData = (DayNoteData) theNoteGroup.noteGroupDataVector.elementAt(1);
        //System.out.println("[" + theNoteData.noteString + "]");
        Assertions.assertEquals("increases and decreases", theNoteData.noteString);

        // Now we skip one (also has data but that shouldn't matter)
        noteGroupPanel.setOneForward(ChronoUnit.DAYS);
        noteGroupPanel.updateGroup();
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Tuesday, March 13, 2018", noteGroupPanel.getTitle());

        // 2.  Change from Panel with data to another with data: 13 Mar 2018 --> 12 Mar 2018
        noteGroupPanel.setOneForward(ChronoUnit.DAYS);
        noteGroupPanel.updateGroup();
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Wednesday, March 14, 2018", noteGroupPanel.getTitle());
        // Re-acquire the NoteData for the element we are examining.
        theNoteData = (DayNoteData) theNoteGroup.noteGroupDataVector.elementAt(1);
        Assertions.assertEquals("date changes", theNoteData.noteString);
        //System.out.println("[" + theNoteData.noteString + "]");

        // 3.  Change from Panel with data to one without data:  12 Mar 2018 --> 11 Mar 2018
        noteGroupPanel.setOneForward(ChronoUnit.DAYS);
        noteGroupPanel.updateGroup();
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Thursday, March 15, 2018", noteGroupPanel.getTitle());
        groupSize = theNoteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(0, groupSize);

        // 4.  Change from Panel without data to another without data: 15 Mar 2018 --> 16 Mar 2018
        noteGroupPanel.setOneForward(ChronoUnit.DAYS);
        noteGroupPanel.updateGroup();
        System.out.println(noteGroupPanel.getTitle());
        Assertions.assertEquals("Friday, March 16, 2018", noteGroupPanel.getTitle());
        groupSize = theNoteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(0, groupSize);

    }

}