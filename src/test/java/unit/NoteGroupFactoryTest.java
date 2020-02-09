import org.junit.jupiter.api.Test;

class NoteGroupFactoryTest {

    @Test
    void testGetGroup() {
        NoteGroupFactory.getGroup("Search Result", "blarg");
    }

    @Test
    void testGetOrMakeGroup() {
        NoteGroupFactory.getOrMakeLeaf("Upcoming Event", "blarg");
        NoteGroupFactory.getOrMakeLeaf("To Do List", "blarg");
        NoteGroupFactory.getOrMakeLeaf("Search Result", "blarg");
    }
}