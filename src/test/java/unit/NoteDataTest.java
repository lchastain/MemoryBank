import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class NoteDataTest {
    private NoteData nd;

    @BeforeEach
    void setUp() throws Exception {
        nd = new NoteData();
        nd.setNoteString("This is it");
        nd.setSubjectString("tcejbus");
        nd.setExtendedNoteString("This too");
        nd.setExtendedNoteHeightInt(300);
        nd.setExtendedNoteWidthInt(400);
        nd = new NoteData(nd); // Get the copy constructor involved too.
    }

    @Test
    void testClear() throws Exception {
        nd.clear();
        assertEquals(nd.getNoteString(), "");
        assertEquals(nd.getExtendedNoteHeightInt(), 200);
    }

    @Test
    void testGetExtendedNoteHeightInt() throws Exception {
        int h = nd.getExtendedNoteHeightInt();
        assertEquals(h, 300);
    }

    @Test
    void testGetExtendedNoteString() throws Exception {
        String s = nd.getExtendedNoteString();
        assertEquals(s, "This too");
    }

    @Test
    void testGetExtendedNoteWidthInt() throws Exception {
        int w = nd.getExtendedNoteWidthInt();
        assertEquals(w, 400);
    }

    @Test
    void testGetNoteDate() throws Exception {
        assertNull(nd.getNoteDate());
    }

    @Test
    void testGetNoteString() throws Exception {
        String s = nd.getNoteString();
        assertEquals(s, "This is it");
    }

    @Test
    void testGetLastModDate() throws Exception {
        nd.setNoteString("I got updated!");
        Date d = nd.getLastModDate();
        assertEquals(d, new Date());
    }

    @Test
    void testGetSubjectString() throws Exception {
        String s = nd.getSubjectString();
        assertEquals(s, "tcejbus");
    }

    @Test
    void testHasText() throws Exception {
        assertTrue(nd.hasText());
        nd.clear();
        assertFalse(nd.hasText());
    }
}