import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public class MoveToDayNoteTest {
    JFrame jFrame;
    AppTreePanel appTreePanel;
    LocalDate theOtherDate;
    DayNoteData unsavedNoteData;

//  There are two tests here (move to today, move to a selected date), five scenarios each -
//    In order, the scenarios are:
//      Move while AppTreePanel has not yet displayed appDays
//      Move while the target date (without unsaved changes) is in appDays
//      Move while the target date (with unsaved changes) is in appDays
//      Move while appDays is showing some other day (without unsaved changes) in AppTreePanel
//      Move while appDays is showing some other day (with unsaved changes) in AppTreePanel
//
// The first test uses the 'Get New Job' todo list, the second one uses 'Ten Things To Do'.
//   Both lists have more than enough items so that a different note may be moved for each scenario.
//
// And moving the notes out is not the only concern; we need to also load in the target groups
//   and verify that they load properly and have the expected content.  In this case since the
//   test user has no pre-existing Day notes at all, it will suffice to verify a successful load
//   and the expected correct count of notes.


    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve the test data for this class from test resources.
        String fileName = "MoveToDayNoteTest";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

    }

    @BeforeEach
    void setUp() {
        jFrame = new JFrame("MoveToDayNoteTest");
        appTreePanel = new AppTreePanel(jFrame, MemoryBank.appOpts);
        unsavedNoteData = new DayNoteData();
        unsavedNoteData.setNoteString("Look at me!  I am unsaved!");
    }

    @AfterEach
    void tearDown() {
        appTreePanel = null;
        jFrame = null;
    }

    @Test
    //    Tests for 'Move to SelectedDate':
    //  1. Move to selected date while AppTreePanel has not yet displayed appDays
    //  2. Move to selected date while that date (without unsaved changes) is in appDays
    //  3. Move to selected date while that date (with unsaved changes) is in appDays
    //  4. Move to selected date while appDays is showing some other day (without unsaved changes) in AppTreePanel
    //  5. Move to selected date while appDays is showing some other day (with unsaved changes) in AppTreePanel
    void testMoveToSelectedDate() {
        theOtherDate = LocalDate.of(2020, 8, 30);
        LocalDate selectedDate = LocalDate.of(2019, 7, 15);

        // Get the Todo List we will be moving 'from', and get the menu item so we can click it.
        TodoNoteGroupPanel todoNoteGroupPanel = new TodoNoteGroupPanel("Ten Things To Do");
        JMenuItem jMenuItem = TodoNoteComponent.miMoveToSelectedDate;
        TodoNoteComponent todoNoteComponent;

        //  1. Move to selected date while AppTreePanel has not yet displayed appDays
        todoNoteComponent = todoNoteGroupPanel.getNoteComponent(1);
        TodoNoteData todoNoteData1 = (TodoNoteData) todoNoteComponent.getNoteData();
        todoNoteData1.setTodoDate(selectedDate);
        NoteComponent.theNoteComponent = todoNoteComponent;
        jMenuItem.doClick();

        // Construct theAppDays, give it to appTreePanel and set it to the selected date.
        appTreePanel.theAppDays = new DayNoteGroupPanel(); // it will default to 'today'.
        appTreePanel.theAppDays.setDate(selectedDate); // This sets the right day and loads up the one note we've added.

        //  2. Move to selected date while that date (without unsaved changes) is in appDays
        todoNoteComponent = todoNoteGroupPanel.getNoteComponent(2);
        TodoNoteData todoNoteData2 = (TodoNoteData) todoNoteComponent.getNoteData();
        todoNoteData2.setTodoDate(selectedDate);
        NoteComponent.theNoteComponent = todoNoteComponent;
        jMenuItem.doClick();

        // Get theAppDays to reload its day data, to pick up the latest new note.
        appTreePanel.theAppDays.setDate(selectedDate);

        //  3. Move to selected date while that date (with unsaved changes) is in appDays
        todoNoteComponent = todoNoteGroupPanel.getNoteComponent(3);
        TodoNoteData todoNoteData3 = (TodoNoteData) todoNoteComponent.getNoteData();
        todoNoteData3.setTodoDate(selectedDate);
        NoteComponent.theNoteComponent = todoNoteComponent;
        appTreePanel.theAppDays.appendNote(unsavedNoteData); // Add a note to theAppDays (without saving the group)
        jMenuItem.doClick(); // This saves 'selectedDate' before adding our latest note to it, then saves it again.

        //  4. Move to selected date while appDays is showing some other day (without unsaved changes) in AppTreePanel
        todoNoteComponent = todoNoteGroupPanel.getNoteComponent(4);
        TodoNoteData todoNoteData4 = (TodoNoteData) todoNoteComponent.getNoteData();
        todoNoteData4.setTodoDate(selectedDate);
        NoteComponent.theNoteComponent = todoNoteComponent;
        appTreePanel.theAppDays.setDate(theOtherDate); // 'test' consequences here!  See below.
        jMenuItem.doClick();

        // The 'test' consequence noted above is that when a 'setDate' is called on a CNG type Panel, it does an
        // updateGroup() which first does a clearPage(), which clears the Notes on that page so they can be reused
        // by the new incoming data.  This clearing is 'felt' all the way back into source data, so the handle that
        // we have to one of them - 'unsavedNoteData' - is now empty.  It needs new text, or else it will not get
        // preserved at save time.
        unsavedNoteData.setNoteString("I have come back thanks to testMoveToSelectedDate!");
        // The reason that this is a 'test' consequence and not a production issue is that in all other operational
        // use cases the NoteData is preserved if needed prior to the call to update the Panel, and then the references
        // that were used to hold that data remain in the Panel and are reused when new data comes in.  Here in this
        // test we want to reuse the reference outside of the Panel and that's ok but we have to understand why it
        // got cleared, and reset its text before using it again.  It would have been simpler to just have a second
        // NoteData... why didn't I do that?  Stubborn.  Trying harder.  Round peg, square hole - made it work!

        //  5. Move to selected date while appDays is showing some other day (with unsaved changes) in AppTreePanel
        todoNoteComponent = todoNoteGroupPanel.getNoteComponent(5);
        TodoNoteData todoNoteData5 = (TodoNoteData) todoNoteComponent.getNoteData();
        todoNoteData5.setTodoDate(selectedDate);
        NoteComponent.theNoteComponent = todoNoteComponent;
        appTreePanel.theAppDays.appendNote(unsavedNoteData); // Add a note to theAppDays (without saving the group)
        jMenuItem.doClick();
        appTreePanel.theAppDays.preClosePanel(); // And now save the group for that other day.

        // If we got down to this point then it's already very good, with no Exceptions and quite
        //    a lot of functionality was covered.  And now here's what we need to look for, to go
        //    beyond coverage only and 'done without error' to 'done correctly'.
        // 1.  Verify that the data repo for 'today' has a list of six notes.
        // 2.  Verify that the data repo for 'theOtherDate' has a list of one note

        // Load the NoteGroups directly, do not get via a Panel.
        String theName = CalendarNoteGroup.getGroupNameForDate(selectedDate, GroupType.DAY_NOTES);
        GroupInfo groupInfo = new GroupInfo(theName, GroupType.DAY_NOTES);
        NoteGroup noteGroup = new DayNoteGroup(groupInfo);
        int noteCount = noteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(6, noteCount);

        theName = CalendarNoteGroup.getGroupNameForDate(theOtherDate, GroupType.DAY_NOTES);
        groupInfo = new GroupInfo(theName, GroupType.DAY_NOTES);
        noteGroup = new DayNoteGroup(groupInfo);
        noteCount = noteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(1, noteCount);
    }


    @Test
        //  1. Move to today while AppTreePanel has not yet displayed appDays
        //  2. Move to today while appDays is showing today (without unsaved changes) in AppTreePanel
        //  3. Move to today while appDays is showing today (with unsaved changes) in AppTreePanel
        //  4. Move to today while appDays is showing some other day (without unsaved changes) in AppTreePanel
        //  5. Move to today while appDays is showing some other day (with unsaved changes) in AppTreePanel
    void testMoveToToday() {
        theOtherDate = LocalDate.of(2020, 8, 31);

        // Get the Todo List we will be moving 'from', and get the menu item so we can click it.
        TodoNoteGroupPanel todoNoteGroupPanel = new TodoNoteGroupPanel("Get New Job");
        JMenuItem jMenuItem = TodoNoteComponent.miMoveToToday;
        TodoNoteComponent todoNoteComponent;

        //  1. Move to today while AppTreePanel has not yet displayed appDays
        todoNoteComponent = todoNoteGroupPanel.getNoteComponent(1);
        NoteComponent.theNoteComponent = todoNoteComponent;
        jMenuItem.doClick();

        // Construct theAppDays and give it to appTreePanel
        appTreePanel.theAppDays = new DayNoteGroupPanel(); // it will default to 'today'.

        //  2. Move to today while appDays is showing today (without unsaved changes) in AppTreePanel
        todoNoteComponent = todoNoteGroupPanel.getNoteComponent(2);
        NoteComponent.theNoteComponent = todoNoteComponent;
        jMenuItem.doClick();

        // Get theAppDays to reload its day data, to pick up the latest new note.
        appTreePanel.theAppDays.setDate(LocalDate.now());

        //  3. Move to today while appDays is showing today (with unsaved changes) in AppTreePanel
        todoNoteComponent = todoNoteGroupPanel.getNoteComponent(3);
        NoteComponent.theNoteComponent = todoNoteComponent;
        appTreePanel.theAppDays.appendNote(unsavedNoteData); // Add a note to theAppDays (without saving the group)
        jMenuItem.doClick(); // This saves 'today' before adding our latest note to it, then saves it again.

        //  4. Move to today while appDays is showing some other day (without unsaved changes) in AppTreePanel
        todoNoteComponent = todoNoteGroupPanel.getNoteComponent(4);
        NoteComponent.theNoteComponent = todoNoteComponent;
        appTreePanel.theAppDays.setDate(theOtherDate); // 'test' consequences here!  See below.
        jMenuItem.doClick();

        // The 'test' consequence noted above is that when a 'setDate' is called on a CNG type Panel, it does an
        // updateGroup() which first does a clearPage(), which clears the Notes on that page so they can be reused
        // by the new incoming data.  This clearing is 'felt' all the way back into source data, so the handle that
        // we have to one of them - 'unsavedNoteData' - is now empty.  It needs new text, or else it will not get
        // preserved at save time.
        unsavedNoteData.setNoteString("I have come back thanks to testMoveToToday!");
        // The reason that this is a 'test' consequence and not a production issue is that in all other operational
        // use cases the NoteData is preserved if needed prior to the call to update the Panel, and then the references
        // that were used to hold that data remain in the Panel and are reused when new data comes in.  Here in this
        // test we want to reuse the reference outside of the Panel and that's ok but we have to understand why it
        // got cleared, and reset its text before using it again.  It would have been simpler to just have a second
        // NoteData... why didn't I do that?  Stubborn.  Trying harder.  Round peg, square hole - made it work!

        //  5. Move to today while appDays is showing some other day (with unsaved changes) in AppTreePanel
        todoNoteComponent = todoNoteGroupPanel.getNoteComponent(5);
        NoteComponent.theNoteComponent = todoNoteComponent;
        appTreePanel.theAppDays.appendNote(unsavedNoteData); // Add a note to theAppDays (without saving the group)
        jMenuItem.doClick();
        appTreePanel.theAppDays.preClosePanel(); // And now save the group for that other day.

        // If we got down to this point then it's already very good, with no Exceptions and quite
        //    a lot of functionality was covered.  And now here's what we need to look for, to go
        //    beyond coverage only and 'done without error' to 'done correctly'.
        // 1.  Verify that the data repo for 'today' has a list of six notes.
        // 2.  Verify that the data repo for 'theOtherDate' has a list of one note

        // Load the NoteGroups directly, do not get via a Panel.
        String theName = CalendarNoteGroup.getGroupNameForDate(LocalDate.now(), GroupType.DAY_NOTES);
        GroupInfo groupInfo = new GroupInfo(theName, GroupType.DAY_NOTES);
        NoteGroup noteGroup = new DayNoteGroup(groupInfo);
        int noteCount = noteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(6, noteCount);

        theName = CalendarNoteGroup.getGroupNameForDate(theOtherDate, GroupType.DAY_NOTES);
        groupInfo = new GroupInfo(theName, GroupType.DAY_NOTES);
        noteGroup = new DayNoteGroup(groupInfo);
        noteCount = noteGroup.noteGroupDataVector.size();
        Assertions.assertEquals(1, noteCount);
    }

}
