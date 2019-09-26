import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

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
    void testGetLastModDate() {
        nd.setNoteString("I got updated!");
        Date d = Date.from(nd.getLastModDate().toInstant());
        assertEquals(d.toString(), new Date().toString());
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