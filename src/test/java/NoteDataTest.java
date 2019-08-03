import junit.framework.TestCase;
import org.junit.Before;

import java.util.Date;

/**
 * Created by Lee on 8/2/2019.
 */
public class NoteDataTest extends TestCase {
    Date started = new Date();
    NoteData nd;

    @Before
    public void setUp() throws Exception {
        nd = new NoteData();
        nd.setNoteString("This is it");
        nd.setSubjectString("tcejbus");
        nd.setExtendedNoteString("This too");
        nd.setExtendedNoteHeightInt(300);
        nd.setExtendedNoteWidthInt(400);
        nd = new NoteData(nd); // Get the copy constructor involved too.
    }


    public void testClear() throws Exception {
        nd.clear();
        assertEquals(nd.getNoteString(), "");
        assertEquals(nd.getExtendedNoteHeightInt(), 200);
    }

    public void testGetExtendedNoteHeightInt() throws Exception {
        int h = nd.getExtendedNoteHeightInt();
        assertEquals(h, 300);
    }

    public void testGetExtendedNoteString() throws Exception {
        String s = nd.getExtendedNoteString();
        assertEquals(s, "This too");
    }

    public void testGetExtendedNoteWidthInt() throws Exception {
        int w = nd.getExtendedNoteWidthInt();
        assertEquals(w, 400);
    }

    public void testGetNoteDate() throws Exception {
        assertNull(nd.getNoteDate());
    }

    public void testGetNoteString() throws Exception {
        String s = nd.getNoteString();
        assertEquals(s, "This is it");
    }

    public void testGetLastModDate() throws Exception {
        Date d = nd.getLastModDate();
        assertTrue(started.before(d));
    }

    public void testGetSubjectString() throws Exception {
        String s = nd.getSubjectString();
        assertEquals(s, "tcejbus");
    }

    public void testHasText() throws Exception {
        assertTrue(nd.hasText());
        nd.clear();
        assertFalse(nd.hasText());
    }
}