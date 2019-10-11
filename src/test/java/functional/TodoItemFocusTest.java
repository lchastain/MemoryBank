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
        // Note that selecting this one will have an effect on the TMC, placing it's
        // previously selected month in the center.
        todoNoteComponent4.setActive();

        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Now we check two items, just to be sure that all NoteComponents don't just
        // say they have Focus whether they actually do or not.  We want to see one
        // false and (the right) one true.  This is our 'starting' point.
        Assertions.assertFalse(todoNoteComponent3.hasFocus());
        Assertions.assertTrue(todoNoteComponent4.hasFocus());

        // Now, make a new Date selection on the Three Month Column.
        // We are changing the base date of the TMC by setting a new choice, but
        // the test should probably use the arrows, because those arrows could also take focus away from
        //                                                  ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
        // the todoItem (except that I have coded them NOT to).
        // For that, we need to set Dates that are a certain known and constant distance apart, cannot
        // just lazily use 'today' as the new selection date.

        // For now, this test works and --appears-- to fulfill the test requirement but the concept
        // of 'focus' is difficult to ensure that it's really being exercised the same way a user would.
        LocalDate today = LocalDate.now();
        todoNoteGroup.getThreeMonthColumn().setChoice(today); // Normally done by a mouse press.
        todoNoteGroup.dateSelected(today); // Normally the mousePressed would have done this for us.

        // Enable this when you want the result to stay longer.
        try {
            Thread.sleep(800);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // Verify that we now have selected a different day on the TMC, but we still
        // have focus on the selected Todo item.
        LocalDate currentDateSelected = ((TodoNoteData) todoNoteComponent4.getNoteData()).getTodoDate();
        Assertions.assertNotSame(originalDateSelected, currentDateSelected);
        Assertions.assertTrue(currentDateSelected.isEqual(today));
        Assertions.assertTrue(todoNoteComponent4.hasFocus());
    }

}