import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

class LinkagesEditorPanelTest {
    LinkagesEditorPanel linkagesEditorPanel;

    GoalGroupPanel goalGroup;
    DayNoteGroupPanel dayNoteGroup;
    TodoNoteGroupPanel todoNoteGroup;

    LinkagesEditorPanelTest() {
        goalGroup = new GoalGroupPanel("Take Over The World");
        dayNoteGroup = new DayNoteGroupPanel();
        todoNoteGroup = new TodoNoteGroupPanel("Preparations");
    }

    @BeforeAll
    static void setup() {
        // Change the Notifier used by the LinkagesEditorPanel
        LinkagesEditorPanel.optionPane = new TestUtil();
    }


    // This test is for coverage only; it exercises the two panel constructors as well as the manual sorting
    //    functionality, and then the other methods that are called from those.  No assertions needed.
    @Test
    void testLinkagesEditorPanel() throws InterruptedException {
        // Make a new Todo note.
        TodoNoteData todoNoteData = new TodoNoteData();
        todoNoteData.noteString = "Links from a TodoNote.";

        // Add two (valid) links to the note.  This will exercise the 'filterLinkages'
        // method when the LinkagesEditorPanel is constructed.
        NoteData noteData1 = new NoteData();
        noteData1.noteString = "milestone note.";
        NoteData noteData2 = new DayNoteData();
        noteData2.noteString = "day note";
        GroupInfo goalGroupInfo = new GroupInfo(goalGroup.myProperties);
        NoteInfo goalNoteInfo = new NoteInfo(noteData1);
        LinkedEntityData led1 = new LinkedEntityData(goalGroupInfo, goalNoteInfo);
        led1.linkType = LinkedEntityData.LinkType.DEPENDING_ON;
        GroupInfo dayNoteGroupInfo = new GroupInfo(dayNoteGroup.getGroupProperties());
        NoteInfo dayNoteInfo = new NoteInfo(noteData2);
        LinkedEntityData led2 = new LinkedEntityData(dayNoteGroupInfo, dayNoteInfo);
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

        // These two are not used, but calling them to get the coverage for interface-required methods.
        linkagesEditorPanel.editExtendedNoteComponent(todoNoteData);
        linkagesEditorPanel.activateNextNote(0);

        // Our Notifier will just automatically give the dialog a 'yes'; we don't actually need to see it.
        linkagesEditorPanel.addLinkButton.doClick();
        // Instead of using the Notifier, you could do this -
        //        new Thread(new Runnable() {
        //            public void run() {
        //                linkagesEditorPanel.addLinkButton.doClick();
        //            }
        //        }).start();
        // ... but then the additional dialog would pop up, and it would just be dismissed after
        //   time and then take the 'cancelled' route without getting into the 'accepted' block of code.
        //   This way with the TestUtil trojan'd in as the Notifier, we test more of the code.

        Thread.sleep(500); // Just enough time for the linkagesEditorPanel to construct the additional dialog.
        testFrame.setVisible(false);

        // The lines below are (usually) commented out.  Enable them if needed and the Frame will remain
        // visible and responsive to user actions.  Then close the window yourself when ready to end.
//        while(testFrame.isVisible()) {
//            Thread.sleep(1000);
//        }

    } // end test


}