import org.junit.jupiter.api.Test;

class NoteGroupFactoryTest {

    @Test
    void testGetGroup() {
        NoteGroupFactory.loadGroup("Search Result", "blarg");
    }

    @Test
    void testGetOrMakeGroup() {
        NoteGroupFactory.loadOrMakeGroup("Upcoming Event", "blarg");
        NoteGroupFactory.loadOrMakeGroup("To Do List", "blarg");
        NoteGroupFactory.loadOrMakeGroup("Search Result", "blarg");
    }
}