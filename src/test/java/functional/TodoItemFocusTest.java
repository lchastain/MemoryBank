import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

// This test is to verify that a given Todo item does not lose 'focus' when a
// date for it is selected on the Three Month Column.  That was the problem
// that was originally reported via SCR0001.

class TodoItemFocusTest {
    private static TodoNoteGroup todoNoteGroup;

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

        // Load up this Test user's application options
        MemoryBank.loadOpts();

        todoNoteGroup = new TodoNoteGroup("Get New Job");
        JFrame theFrame = new JFrame("ItemFocusTest");
        theFrame.setContentPane(todoNoteGroup);
        theFrame.pack();
        theFrame.setSize(new Dimension(800, 600));
        theFrame.setVisible(true);
        theFrame.setLocationRelativeTo(null); // Center
    }

    @Test
    void testItemFocus() {
        // Getting initial states
        TodoNoteComponent todoNoteComponent3 = todoNoteGroup.getNoteComponent(3);
        TodoNoteComponent todoNoteComponent4 = todoNoteGroup.getNoteComponent(4);
        LocalDate originalDateSelected = ((TodoNoteData) todoNoteComponent4.getNoteData()).getTodoDate();

        // Allow some time to display the JFrame.
        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Put the focus on a note -
        // Note that selecting this one WILL have an effect on the TMC, placing it's
        // previously selected month in the center.
        todoNoteComponent4.setActive();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Now we check two items, just to be sure that all NoteComponents don't just
        // say they have Focus whether they actually do or not.  We want to see one
        // false and (the right) one true.  This is our 'starting' point.
        Assertions.assertFalse(todoNoteComponent3.hasFocus());
        Assertions.assertTrue(todoNoteComponent4.hasFocus());

        // Now, make a new Date selection on the Three Month Column.
        // Note that if the new selection month is not already visible you will probably not
        // see any change here, since TMC scrolling while an item is selected is only done
        // by the 'arrow' controls.  Programmatic setting of the Date like we do here is allowed
        // but there is no need for a headless driver to 'see' the control scrolled to show
        // the new month; just that the new selection was accepted.

        // NOTE - I don't like the above comment.  It may be true but it would still be better to
        // drive this test as closely as possible to how a user would take action and SEE the results.
        // So I don't trust that the test methodology is capturing the true need, here. ie,
        // the test SHOULD use the arrows, because those arrows could also take focus away from
        //                                         ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        // the todoItem (except that I have coded them NOT to).
        // For that, we need to set Dates that are a certain known and constant distance apart, cannot
        // just lazily use 'today' as the new selection date.

        // For now, this test works and --appears-- to fulfill the test requirement but the concept
        // of 'focus' is difficult to ensure that it's really being exercised if we cannot also see the
        // results, so to do that - ?? - maybe have known mouse coordinates, and use MouseEvents?
        // Maybe - refactor the TMC, to expose 'adjustment' methods that the mouse actions also call.
        //
        LocalDate today = LocalDate.now();
        todoNoteGroup.getThreeMonthColumn().setBaseDate(today); // Normally done by the TodoNoteGroup.
        todoNoteGroup.getThreeMonthColumn().setChoice(today); // Normally done by a mouse press.
        todoNoteGroup.dateSelected(today); // Normally the mousePressed would have done this for us.

        // Enable this when you want the result to stay longer.
//        try {
//            Thread.sleep(2000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        // Verify that we now have selected a different day on the TMC, but we still
        // have focus on the selected Todo item.
        LocalDate currentDateSelected = ((TodoNoteData) todoNoteComponent4.getNoteData()).getTodoDate();
        Assertions.assertNotSame(originalDateSelected, currentDateSelected);
        Assertions.assertSame(today, currentDateSelected);
        Assertions.assertTrue(todoNoteComponent4.hasFocus());
    }

}