import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NoteGroupTest {
    NoteGroup noteGroup;

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