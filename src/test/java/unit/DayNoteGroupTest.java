
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

class DayNoteGroupTest {
    private DayNoteGroup dng;

    @BeforeEach
    void setUp() {
        MemoryBank.setUserDataHome("lex@doughmain.net");
        dng = new DayNoteGroup();
    }

    @AfterEach
    void tearDown() {
        dng = null;
    }

//    @Test
//    void testGetChoiceString() {
//    }
//
//    @Test
//    void testGetDefaultIcon() {
//    }
//
//    @Test
//    void testGetNoteComponent() {
//    }
//
//    @Test
//    void testMakeNewNote() {
//    }
//
//    @Test
//    void testMouseClicked() {
//    }
//
//    @Test
//    void testMouseEntered() {
//    }
//
//    @Test
//    void testMouseExited() {
//    }
//
//    @Test
//    void testMousePressed() {
//    }
//
//    @Test
//    void testMouseReleased() {
//    }
//
//    @Test
//    void testRecalc() {
//    }

    @Test
    void testSetChoice() {
        Date d = new Date();
        dng.setChoice(d);
    }

//    @Test
//    void testSetDefaultIcon() {
//    }
//
//    @Test
//    void testShiftDown() {
//    }
//
//    @Test
//    void testShiftUp() {
//    }
//
//    @Test
//    void testToggleMilitary() {
//    }
//
//    @Test
//    void testUpdateHeader() {
//    }
}