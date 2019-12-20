import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;


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
    void testIsWeekday() {
    }

    @Test
    void testItemStateChanged() {
    }

    // Intellij reports 4 'class' methods for RecurrencePanel; 3 are shown in the Structure view for the class
    // but one is 'hiding' - it is known as a 'Synthetic' class, anonymously created by a switch statement that
    // makes a call to a date-related class that might throw an exception.  This test exercises that anonymous
    // class and gets us the coverage needed there.
    @Test
    void testRecalcEnd() {
        // showTheData is needed to set the Start Date, used later.
        String theString = "Y_the 14th of February_";
        theRecurrencePanel.showTheData(theString, LocalDate.now());

        LocalDate theEndDate = LocalDate.of(LocalDate.now().plusYears(5).getYear(),7,15);
        theRecurrencePanel.setPeriodicity(ChronoUnit.WEEKS);
        theRecurrencePanel.setStopBy(theEndDate);
    }

}