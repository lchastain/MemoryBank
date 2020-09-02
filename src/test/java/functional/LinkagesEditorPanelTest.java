import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

class LinkagesEditorPanelTest {
    LinkagesEditorPanel linkagesEditorPanel;

    // One of each NoteGroup type (types that can link to, or be linked to)
    DayNoteGroupPanel dayNoteGroup;
    MonthNoteGroupPanel monthNoteGroup;
    YearNoteGroupPanel yearNoteGroup;
    GoalGroupPanel goalGroup;
    EventNoteGroupPanel eventNoteGroup;
    TodoNoteGroupPanel todoNoteGroup;
    // For a link to any one of the other groups types, including its own type, there are 6 possibilities.
    // And then another 6 for linking to a note from each one, = 12 tests per each of 6 classes.
    TodoNoteData todoNoteData = new TodoNoteData();

    LinkagesEditorPanelTest() {
        goalGroup = new GoalGroupPanel("Take Over The World");
        eventNoteGroup = new EventNoteGroupPanel("Elections");
        dayNoteGroup = new DayNoteGroupPanel();
        monthNoteGroup = new MonthNoteGroupPanel();
        yearNoteGroup = new YearNoteGroupPanel();
        todoNoteGroup = new TodoNoteGroupPanel("This Week");
    }

    @BeforeAll
    static void setup() throws IOException {
        // Set the test user's data location
        MemoryBank.setUserDataHome("test.user@lcware.net");

        LinkagesEditorPanel.optionPane = new TestUtil();

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources.
        // This test user has a rich set of data, includes Search Results and Todo Lists
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();
    }


    // This test is for coverage only; it exercises the two panel constructors as well as the manual sorting
    //    functionality, and then the other methods that are called from those.  No assertions needed.
    @Test
    void testLinkagesEditorPanel() throws InterruptedException {
        // Make a new Todo note and tell it that it belongs to the todoNoteGroup.
        TodoNoteData todoNoteData = new TodoNoteData();
        todoNoteData.noteString = "Links from a TodoNote.";
        todoNoteData.myNoteGroupPanel = todoNoteGroup; // Not actually true, but this works for our purposes.

        // Add two (valid) links to the note.  This will exercise the 'filterLinkages'
        // method when the LinkagesEditorPanel is constructed.
        NoteData noteData1 = new NoteData();
        noteData1.noteString = "milestone note.";
        NoteData noteData2 = new DayNoteData();
        noteData2.noteString = "day note";
        LinkedEntityData led1 = new LinkedEntityData(goalGroup.myProperties, noteData1);
        led1.linkType = LinkedEntityData.LinkType.DEPENDING_ON;
        LinkedEntityData led2 = new LinkedEntityData(dayNoteGroup.getGroupProperties(), noteData2);
        led2.linkType = LinkedEntityData.LinkType.AFTER;
        todoNoteData.linkTargets.add(led1);
        todoNoteData.linkTargets.add(led2);

        // Construct the editor panel for the new source Note -
        linkagesEditorPanel = new LinkagesEditorPanel(todoNoteGroup.myProperties, todoNoteData);
        linkagesEditorPanel.editExtendedNoteComponent(todoNoteData);

        JFrame testFrame = new JFrame("Link Editing Driver");
        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                testFrame.setVisible(false);
            }
        });

        testFrame.getContentPane().add(linkagesEditorPanel);
        testFrame.pack();
        testFrame.setSize(new Dimension(620, 540));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);

        Thread.sleep(500);
        linkagesEditorPanel.shiftDown(0);
        Thread.sleep(500);
        linkagesEditorPanel.shiftUp(1);

        Thread.sleep(500);

        // Our Notifier will just automatically give the dialog a 'yes'; we don't need to see it.
        linkagesEditorPanel.addLinkButton.doClick();
        // Without the Notifier, you could still do this -
        //        new Thread(new Runnable() {
        //            public void run() {
        //                linkagesEditorPanel.addLinkButton.doClick();
        //            }
        //        }).start();
        // ... but then the additional dialog would pop up, and it would just be dismissed after
        //   time, without getting into the 'accepted' block of code.

        Thread.sleep(500); // Just enough time to construct the additional dialog.
        testFrame.setVisible(false);

        // The lines below are (usually) commented out.  Enable them if needed and the Frame will remain
        // visible and responsive to user actions.  Then close the window yourself when ready to end.
//        while(testFrame.isVisible()) {
//            Thread.sleep(1000);
//        }

    } // end test


    @Test
    void testAddReverseLinks() {
        // Make a new Todo note and tell it that it belongs to the todoNoteGroup.
        TodoNoteData todoNoteData = new TodoNoteData();
        todoNoteData.noteString = "Links from a TodoNote.";
        todoNoteData.myNoteGroupPanel = todoNoteGroup; // Not actually true, but this works for our purposes.

        // Construct the editor panel for the new source Note -
        linkagesEditorPanel = new LinkagesEditorPanel(todoNoteGroup.myProperties, todoNoteData);

        // Add two (valid) links to the panel.  This will ensure that the loop for adding a reverse link will run twice.
        NoteData noteData1 = new NoteData();
        noteData1.noteString = "milestone note.";
        NoteData noteData2 = new DayNoteData();
        noteData2.noteString = "day note";
        LinkedEntityData led1 = new LinkedEntityData(goalGroup.myProperties, noteData1);
        led1.linkType = LinkedEntityData.LinkType.DEPENDING_ON;
        LinkedEntityData led2 = new LinkedEntityData(dayNoteGroup.getGroupProperties(), noteData2);
        led2.linkType = LinkedEntityData.LinkType.AFTER;

        // Add the two links to the panel -
        linkagesEditorPanel.linkTargets.add(led1);
        linkagesEditorPanel.linkTargets.add(led2);

        // T-crossing, for coverage.
        linkagesEditorPanel.editExtendedNoteComponent(todoNoteData);
        linkagesEditorPanel.activateNextNote(0);

        // Here it is -
        linkagesEditorPanel.addReverseLinks(linkagesEditorPanel.linkTargets);
    }

}