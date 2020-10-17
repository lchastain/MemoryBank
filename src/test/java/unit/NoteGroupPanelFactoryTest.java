import org.junit.jupiter.api.Test;

class NoteGroupPanelFactoryTest {

    @Test
    void testGetGroup() {
        GroupPanelFactory.loadNoteGroup("Search Result", "blarg");
    }

    @Test
    void testGetOrMakeGroup() {
        GroupPanelFactory.loadOrMakePanel("Upcoming Event", "blarg");
        GroupPanelFactory.loadOrMakePanel("To Do List", "blarg");
        GroupPanelFactory.loadOrMakePanel("Search Result", "blarg");
    }
}