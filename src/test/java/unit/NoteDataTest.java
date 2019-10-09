import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

import static org.junit.jupiter.api.Assertions.*;

class NoteDataTest {
    private NoteData nd;

    @BeforeEach
    void setUp() {
        nd = new NoteData();
        nd.setNoteString("This is it");
        nd.setSubjectString("tcejbus");
        nd.setExtendedNoteString("This too");
        nd.setExtendedNoteHeightInt(300);
        nd.setExtendedNoteWidthInt(400);
        nd = new NoteData(nd); // Get the copy constructor involved too.
    }

    @Test
    void testClear() {
        nd.clear();
        assertEquals(nd.getNoteString(), "");
        assertEquals(nd.getExtendedNoteHeightInt(), 200);
    }

    @Test
    void testGetExtendedNoteHeightInt() {
        int h = nd.getExtendedNoteHeightInt();
        assertEquals(h, 300);
    }

    @Test
    void testGetExtendedNoteString() {
        String s = nd.getExtendedNoteString();
        assertEquals(s, "This too");
    }

    @Test
    void testGetExtendedNoteWidthInt() {
        int w = nd.getExtendedNoteWidthInt();
        assertEquals(w, 400);
    }

    @Test
    void testGetNoteString() {
        String s = nd.getNoteString();
        assertEquals(s, "This is it");
    }

    @Test
    // Here we verify that the Last Mod Date is updated to current time when a
    // change is made to the NoteData.
    void testLastModDateSetting() {
        ZonedDateTime zdt;
        LocalDateTime ldt;

        // Synchronized, for some insurance against minimal time differences that
        // might result in a rollover that would cause one or more assertions
        // below to fail.  But it could still happen; 'synchronized' is more for
        // multithreaded apps and less for making an atomic block of code.
        synchronized (this) {
            nd.setNoteString("I got updated!");
            zdt = nd.getLastModDate();
            ldt = LocalDateTime.now();
        }

        // If there is still a rollover on zdt or ldt, that the other var did not
        // also experience, then one or more of these assertions could fail.  In
        // that very rare case, just run the test again and it should pass.  If
        // not then a mismatched rollover is probably not the problem.
        Assertions.assertEquals(zdt.getYear(), ldt.getYear());
        Assertions.assertEquals(zdt.getMonth(), ldt.getMonth());
        Assertions.assertEquals(zdt.getDayOfYear(), ldt.getDayOfYear());
        Assertions.assertEquals(zdt.getHour(), ldt.getHour());
        Assertions.assertEquals(zdt.getMinute(), ldt.getMinute());
    }

    @Test
    void testGetSubjectString() {
        String s = nd.getSubjectString();
        assertEquals(s, "tcejbus");
    }

    @Test
    void testHasText() {
        assertTrue(nd.hasText());
        nd.clear();
        assertFalse(nd.hasText());
    }
}