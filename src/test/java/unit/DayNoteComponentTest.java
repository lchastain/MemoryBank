import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.io.File;

class DayNoteComponentTest {
    private DayNoteComponent dayNoteComponent;
    DayNoteGroupPanel dayNoteGroup;
    Notifier theNotifier;

    @BeforeEach
    void setUp() {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("test.user@lcware.net");
        dayNoteGroup = new DayNoteGroupPanel();
        dayNoteComponent = new DayNoteComponent(dayNoteGroup, 0);
        dayNoteComponent.initialize();
    }

    @AfterEach
    void tearDown() throws Exception {
        dayNoteComponent = null;
        Thread.sleep(100); // allow some time for GC
    }

    @Test
    void testActionPerformed() {
        DayNoteComponent.NoteTimeLabel noteTimeLabel = dayNoteComponent.getNoteTimeLabel();
        JMenuItem jMenuItem = new JMenuItem("Clear Line");
        ActionEvent actionEvent = new ActionEvent(jMenuItem, ActionEvent.ACTION_PERFORMED, "test");
        noteTimeLabel.actionPerformed(actionEvent);

        jMenuItem = new JMenuItem("Clear Time");
        actionEvent = new ActionEvent(jMenuItem, ActionEvent.ACTION_PERFORMED, "test");
        noteTimeLabel.actionPerformed(actionEvent);

        theNotifier = new TestUtil();
        DayNoteComponent.optionPane = theNotifier;
        dayNoteComponent.setNoteData(new DayNoteData());
        jMenuItem = new JMenuItem("Set Time");
        actionEvent = new ActionEvent(jMenuItem, ActionEvent.ACTION_PERFORMED, "test");
        noteTimeLabel.actionPerformed(actionEvent);

        jMenuItem = new JMenuItem("blarg");
        actionEvent = new ActionEvent(jMenuItem, ActionEvent.ACTION_PERFORMED, "test");
        noteTimeLabel.actionPerformed(actionEvent);
    }

    @Test
    void testClear() {
        dayNoteComponent.clear();
    }

    // This is actually a test of the abstract IconNoteComponent.
    @Test
    void testHandleIconPopup() {
        ActionEvent ae1 = new ActionEvent(IconNoteComponent.sadMi, 0, "");
        ActionEvent ae2 = new ActionEvent(IconNoteComponent.blankMi, 0, "");
        dayNoteComponent.noteIcon.actionPerformed(ae1);
        dayNoteComponent.noteIcon.actionPerformed(ae2);
    }

    @Test
    void testMouseListener() {
        DayNoteComponent.NoteTimeLabel noteTimeLabel = dayNoteComponent.getNoteTimeLabel();
        MouseEvent me = new MouseEvent(noteTimeLabel, MouseEvent.MOUSE_PRESSED, 0, 0, 18, 23, 0, false);
        noteTimeLabel.mousePressed(me);

        me = new MouseEvent(noteTimeLabel, MouseEvent.MOUSE_ENTERED, 0, 0, 18, 23, 0, false);
        noteTimeLabel.mouseEntered(me);

        me = new MouseEvent(noteTimeLabel, MouseEvent.MOUSE_EXITED, 0, 0, 18, 23, 0, false);
        noteTimeLabel.mouseExited(me);

        me = new MouseEvent(noteTimeLabel, MouseEvent.MOUSE_RELEASED, 0, 0, 18, 23, 0, false);
        noteTimeLabel.mouseReleased(me);

        me = new MouseEvent(noteTimeLabel, MouseEvent.MOUSE_CLICKED, 0, 0, 18, 23, 0, false);
        noteTimeLabel.mouseClicked(me);

    }

    @Test
    void testNoteActivated() {
        dayNoteComponent.noteActivated(true);
        dayNoteComponent.noteActivated(false);
    }


    // This is actually a test of the abstract IconNoteComponent.
    @Test
    void testShowIconPopup() {
        JButton pressMe = new JButton("Press Me");
        JFrame testFrame = new JFrame();
        testFrame.add(pressMe);
        testFrame.getContentPane().add(pressMe, "Center");
        testFrame.pack();
        testFrame.setLocationRelativeTo(null);
        testFrame.setVisible(true);
        MouseEvent me = new MouseEvent(pressMe, 1,1,1,1,1,1,true, 1);
        dayNoteComponent.noteIcon.showIconPopup(me);
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignore) {}
        testFrame.setVisible(false);
    }

    @Test
    void setIcon() {
        String fileName = "IconFileViewTest/specs.ico";
        File testFile = FileUtils.toFile(getClass().getResource(fileName));

        dayNoteComponent.setIcon(new AppIcon(testFile.getPath()));
    }

    // Gettin the coverage...
    @Test
    void testSetNoteData() {
        dayNoteComponent.setNoteData(new NoteData());
        dayNoteComponent.setNoteData(new DayNoteData());
        dayNoteComponent.setNoteData(new TodoNoteData());
        dayNoteComponent.setNoteData(new EventNoteData());
    }

}