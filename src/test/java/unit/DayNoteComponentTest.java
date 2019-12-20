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
    DayNoteGroup dayNoteGroup;

    @BeforeEach
    void setUp() {
        MemoryBank.setUserDataHome("test.user@lcware.net");
        dayNoteGroup = new DayNoteGroup() {
            @Override
            public String getGroupFilename() {
                return "2019\\D1220_20191220163347.json";
            }
        };
        dayNoteComponent = new DayNoteComponent(dayNoteGroup, 0);
        dayNoteComponent.initialize();
    }

    @AfterEach
    void tearDown() throws Exception {
        dayNoteComponent = null;
        Thread.sleep(100); // allow some time for GC
    }

    @Test
    void clear() {
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
    }

    @Test
    void setIcon() {
        String fileName = "IconFileViewTest/specs.ico";
        File testFile = FileUtils.toFile(getClass().getResource(fileName));

        dayNoteComponent.setIcon(new AppIcon(testFile.getPath()));
    }

//    @Test
//    void resetTimeLabel() {
//    }
//
//    @Test
//    void setNoteData() {
//    }
//
//    @Test
//    void testSetNoteData() {
//    }
//
//    @Test
//    void shiftDown() {
//    }
//
//    @Test
//    void shiftUp() {
//    }
//
//    @Test
//    void swap() {
//    }
}