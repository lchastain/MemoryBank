import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class NoteGroupPanelFactoryTest {

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        TestUtil.getTheAppTreePanel();
    }

    @Test
    void testGetGroup() {
        GroupPanelFactory.loadNoteGroupPanel("Search Result", "blarg");
    }

    @Test
    void testGetOrMakeGroup() {
        GroupPanelFactory.loadOrMakePanel("Upcoming Event", "blarg");
        GroupPanelFactory.loadOrMakePanel("To Do List", "blarg");
        GroupPanelFactory.loadOrMakePanel("Search Result", "blarg");
    }
}