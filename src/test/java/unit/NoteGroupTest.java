import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

class NoteGroupTest {
    NoteGroup noteGroup;

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        TestUtil.getTheAppTreePanel();
    }

    @BeforeEach
    void setUp() {
        noteGroup = new NoteGroup(new GroupInfo("aGroupName", GroupType.SEARCH_RESULTS));
    }

    @Test
    void testProperties() {
        GroupProperties groupProperties = noteGroup.getGroupProperties();
        Assertions.assertNotNull(groupProperties);
    }
}