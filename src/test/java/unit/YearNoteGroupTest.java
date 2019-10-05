import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class YearNoteGroupTest {
    private YearNoteGroup yng;

    @BeforeEach
    void setUp() {
        yng = new YearNoteGroup();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testRecalc() {
        yng.recalc();
    }

}