import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TimeChooserTest {
    private static TimeChooser timeChooser;

    @BeforeAll
    static void meFirst() {
        timeChooser = new TimeChooser();
    }

    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testGetClearBoolean() {
        boolean b = timeChooser.getClearBoolean();
        assert !b;
    }

    @Test
    void testGetChoice() {
    }

    @Test
    void testRecalc() {
    }

    @Test
    void testSetShowSeconds() {
    }
}