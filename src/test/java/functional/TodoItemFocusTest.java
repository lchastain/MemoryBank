import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.Date;

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
        Date originalDateSelected = todoNoteComponent4.getNoteData().getNoteDate();
        todoNoteComponent4.setActive();

        // Give the 'show' thread time to display the JFrame.
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        // We check two items, just to be sure that all NoteComponents don't just say
        // they have Focus whether they actually do or not.  We want to see one
        // false and (the right) one true.  This is our 'starting' point.
        Assertions.assertFalse(todoNoteComponent3.hasFocus());
        Assertions.assertTrue(todoNoteComponent4.hasFocus());

        // Now, make a Date selection on the Three Month Column.
        // Note that if the new selection month is not already visible you will probably not
        // see any change here, since TMC scrolling is done upon selection of a new item (or
        // by the 'arrow' controls).  External setting of the Date like we do here is allowed
        // but there is no need for a headless driver to 'see' the control scrolled to show
        // the new month; just that the new selection was accepted.

        // NOTE - I don't like the above comment.  It may be true but it would still be better to
        // drive this test as closely
        // as possible to how a user would take action and see the results.  So I don't
        // trust that the test methodology is capturing the true need, here.
        //
        // One slightly more easily done variant is to set the orig selection and new selection so
        // that both are on the same month or at most one month away.  Then TMC scrolling is out of
        // the picture and you would at least see the highlighting move from the old day to the new one.
        Date today = new Date();
        todoNoteGroup.getThreeMonthColumn().setChoice(today); // Normally done by a mouse press.
        todoNoteGroup.dateSelected(today); // Normally the mousePressed would have done this for us.

        // Enable this when you want to see the result - currently no result IS seen; working on that...
        // Ok, not working on it.  Thinking on it.  9/11/2019
        //
        // ... and what I'm thinking is that it might be better to get my Date usages throughout the entire
        // app updated to Java 8 requirements, before trying harder here and in many other places that need
        // testing.  For now, this test works and --appears-- to fulfill the test requirement but the concept
        // of 'focus' is difficult to ensure that it's really being exercised if we cannot also see the
        // results, so making the entire app visually driveable is yet another maybe-prerequisite to doing a
        // better job here.
        //
        //
//        try {
//            Thread.sleep(4000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }

        // Verify that we now have selected a different day on the TMC, but we still
        // have focus on the selected Todo item.
        Date currentDateSelected = todoNoteComponent4.getNoteData().getNoteDate();
        Assertions.assertNotSame(originalDateSelected, currentDateSelected);
        Assertions.assertSame(today, currentDateSelected);
        Assertions.assertTrue(todoNoteComponent4.hasFocus());
    }

}