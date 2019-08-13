import junit.framework.TestCase;

import java.util.Date;

public class DayNoteGroupTest extends TestCase {
    DayNoteGroup dng;

    public void setUp() throws Exception {
        super.setUp();
        MemoryBank.setUserDataHome("lex@doughmain.net");
        dng = new DayNoteGroup();
    }

    public void tearDown() throws Exception {
        dng = null;
    }

    public void testGetChoiceString() {
    }

    public void testGetDefaultIcon() {
    }

    public void testGetNoteComponent() {
    }

    public void testMakeNewNote() {
    }

    public void testMouseClicked() {
    }

    public void testMouseEntered() {
    }

    public void testMouseExited() {
    }

    public void testMousePressed() {
    }

    public void testMouseReleased() {
    }

    public void testRecalc() {
    }

    public void testSetChoice() {
        Date d = new Date();
        dng.setChoice(d);
    }

    public void testSetDefaultIcon() {
    }

    public void testShiftDown() {
    }

    public void testShiftUp() {
    }

    public void testToggleMilitary() {
    }

    public void testUpdateHeader() {
    }
}