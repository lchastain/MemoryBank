import org.junit.jupiter.api.Test;

class NoteGroupPanelFactoryTest {

    @Test
    void testGetGroup() {
        GroupPanelFactory.loadNoteGroup("Search Result", "blarg");
    }

    @Test
    void testGetOrMakeGroup() {
        GroupPanelFactory.loadOrMakeGroup("Upcoming Event", "blarg");
        GroupPanelFactory.loadOrMakeGroup("To Do List", "blarg");
        GroupPanelFactory.loadOrMakeGroup("Search Result", "blarg");
    }
}