import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;


// Lots of work needed here but not sure where to start, and
// some/all might be done better via functional tests, if I
// had written any - those might be coming soon...

class RecurrencePanelTest {
    private static RecurrencePanel theRecurrencePanel;

    @BeforeAll
    static void meFirst() {
        theRecurrencePanel = new RecurrencePanel();
    }


    @BeforeEach
    void setUp() {
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testActionPerformed() {
    }

    @Test
    void testFocusGained() {
    }

    @Test
    void testFocusLost() {
    }

    @Test
    void testGetContentPane() {
    }

    @Test
    void testGetMinimumSize() {
    }

    @Test
    void testGetPreferredSize() {
    }

    @Test
    void testGetRecurrenceSetting() {
        theRecurrencePanel.getRecurrenceSetting();
    }

    @Test
    void testIsRecurrenceValid() {
    }

    @Test
    void testIsWeekday() {
    }

    @Test
    void testItemStateChanged() {
    }

    @Test
    void testShowTheData() {
    }
}