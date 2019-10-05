import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MonthNoteGroupTest {
    private MonthNoteGroup mng;

    @BeforeEach
    void setUp() {
        mng = new MonthNoteGroup();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testRecalc() {
        mng.recalc();
    }

}