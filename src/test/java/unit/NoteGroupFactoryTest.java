import org.junit.jupiter.api.Test;

class NoteGroupFactoryTest {

    @Test
    void testGetGroup() {
        NoteGroupFactory.getGroup("Search Result", "blarg");
    }

    @Test
    void testGetOrMakeGroup() {
        NoteGroupFactory.getOrMakeGroup("Upcoming Event", "blarg");
        NoteGroupFactory.getOrMakeGroup("To Do List", "blarg");
        NoteGroupFactory.getOrMakeGroup("Search Result", "blarg");
    }
}