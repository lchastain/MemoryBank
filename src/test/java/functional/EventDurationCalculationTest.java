import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

// This class is for testing the recalcDuration method of an EventNoteData.
// There are 16 different major data variants to be considered, and since
// these tests need to fully exercise them, they are considered functional
// testing rather than Unit, even though we are testing a single class method.

// Covers testing needed to confirm parts of the resolution for SCR0089.
// References to 'the table' mean the table for Duration Calculation
// (first tab) in the SupportingData.xlsx spreadsheet in the Help folder.

class EventDurationCalculationTest {
    private EventNoteData eventNoteData;


    @BeforeEach
    void setUp() {
        eventNoteData = new EventNoteData();
        Assertions.assertNull(eventNoteData.getStartDate());
        Assertions.assertNull(eventNoteData.getStartTime());
        Assertions.assertNull(eventNoteData.getEndDate());
        Assertions.assertNull(eventNoteData.getEndTime());
    }

    @AfterAll
    static void tearDown() {
    }

    // Test that when no calculation can be made, the Duration
    // remains null.
    @Test
    void testTheUnknowns() {
        // Line 0 of the table.
        Assertions.assertNull(eventNoteData.getDurationValue());

        // Line 1
        eventNoteData.setEndTime(LocalTime.of(12,37));
        Assertions.assertNull(eventNoteData.getDurationValue());
        eventNoteData.setEndTime(null); // reset

        // Line 2
        eventNoteData.setEndDate(LocalDate.of(2018,8,8));
        Assertions.assertNull(eventNoteData.getDurationValue());
        eventNoteData.setEndDate(null); // reset

        // Line 3
        eventNoteData.setEndDate(LocalDate.of(2018,8,8));
        eventNoteData.setEndTime(LocalTime.of(12,37));
        Assertions.assertNull(eventNoteData.getDurationValue());
        eventNoteData.setEndDate(null); // reset
        eventNoteData.setEndTime(null); // reset

        // Line 4
        eventNoteData.setStartTime(LocalTime.of(12,37));
        Assertions.assertNull(eventNoteData.getDurationValue());
        eventNoteData.setStartTime(null); // reset

        // Line 6
        eventNoteData.setStartTime(LocalTime.of(12,37));
        eventNoteData.setEndDate(LocalDate.of(2018,8,8));
        Assertions.assertNull(eventNoteData.getDurationValue());
        eventNoteData.setStartTime(null); // reset
        eventNoteData.setEndDate(null); // reset

        // Line 7
        eventNoteData.setStartTime(LocalTime.of(12,37));
        eventNoteData.setEndDate(LocalDate.of(2018,8,8));
        eventNoteData.setEndTime(LocalTime.of(12,39));
        Assertions.assertNull(eventNoteData.getDurationValue());
        eventNoteData.setStartTime(null); // reset
        eventNoteData.setEndDate(null); // reset
        eventNoteData.setEndTime(null); // reset

        // Line 8
        eventNoteData.setStartDate(LocalDate.of(2017,10,12));
        Assertions.assertNull(eventNoteData.getDurationValue());
        eventNoteData.setStartDate(null); // reset

        // Line 9
        eventNoteData.setStartDate(LocalDate.of(2017,10,12));
        eventNoteData.setEndTime(LocalTime.of(12,37));
        Assertions.assertNull(eventNoteData.getDurationValue());
        eventNoteData.setStartDate(null); // reset
        eventNoteData.setEndTime(null); // reset

        // Line 'c' of the table
        eventNoteData.setStartDate(LocalDate.of(2017,10,12));
        eventNoteData.setStartTime(LocalTime.of(12,37));
        Assertions.assertNull(eventNoteData.getDurationValue());
        eventNoteData.setStartDate(null); // reset
        eventNoteData.setStartTime(null); // reset

        // Line 'd' of the table
        eventNoteData.setStartDate(LocalDate.of(2017,10,12));
        eventNoteData.setStartTime(LocalTime.of(12,37));
        eventNoteData.setEndTime(LocalTime.of(12,41));
        Assertions.assertNull(eventNoteData.getDurationValue());
    }

    // Line 5 of the table
    @Test
    void testStartTimeAndEndTimeKnown() {
        // Set the Times for a 12-minute difference
        eventNoteData.setStartTime(LocalTime.of(8, 35));
        eventNoteData.setEndTime(LocalTime.of(8, 47));

        // Verify a positive (12 minutes) duration.
        Assertions.assertEquals(Integer.valueOf(12), eventNoteData.getDurationValue());
        Assertions.assertEquals("Minutes", eventNoteData.getDurationUnits());

        // Set the End time earlier than the start (indicates a > 1 day difference)
        eventNoteData.setEndTime(LocalTime.of(8, 27));

        // And verify that it was 'normalized' to less than one day difference.
        Assertions.assertEquals(Integer.valueOf(1432), eventNoteData.getDurationValue());
        Assertions.assertEquals("Minutes", eventNoteData.getDurationUnits());

        // Now set the End Time for a difference in whole hours.
        eventNoteData.setEndTime(LocalTime.of(11, 35));

        // And verify correct value and units.
        Assertions.assertEquals(Integer.valueOf(3), eventNoteData.getDurationValue());
        Assertions.assertEquals("Hours", eventNoteData.getDurationUnits());
    }

    // Lines a,b,e  of the table
    @Test
    void testStartDateAndEndDateKnown() {
        // Verify a 31 day difference, with no times
        eventNoteData.setStartDate(LocalDate.of(2017,6,6));
        eventNoteData.setEndDate(LocalDate.of(2017, 7,7));
        Assertions.assertEquals(Integer.valueOf(31), eventNoteData.getDurationValue());
        Assertions.assertEquals("Days", eventNoteData.getDurationUnits());

        // Verify a 33 day difference, with both Dates and the End Time
        eventNoteData.setEndDate(LocalDate.of(2017, 7,9));
        eventNoteData.setEndTime(LocalTime.of(20,20));
        Assertions.assertEquals(Integer.valueOf(33), eventNoteData.getDurationValue());
        Assertions.assertEquals("Days", eventNoteData.getDurationUnits());
        eventNoteData.setEndTime(null); // reset

        // Verify a 35 day difference --> 5 weeks, with both Dates and the Start Time
        eventNoteData.setEndDate(LocalDate.of(2017, 7,11));
        eventNoteData.setStartTime(LocalTime.of(20,20));
        Assertions.assertEquals(Integer.valueOf(5), eventNoteData.getDurationValue());
        Assertions.assertEquals("Weeks", eventNoteData.getDurationUnits());
        eventNoteData.setStartTime(null); // reset

        // Test a HUGE difference -
        eventNoteData.setStartDate(LocalDate.of(1419,2,8));
        eventNoteData.setEndDate(LocalDate.of(2019, 10,20));
        Assertions.assertEquals(Integer.valueOf(31342), eventNoteData.getDurationValue());
        Assertions.assertEquals("Weeks", eventNoteData.getDurationUnits());

        // Test TOO huge of a difference -
        eventNoteData.setEndDate(LocalDate.of(3919, 3,19));
        Assertions.assertNull(eventNoteData.getDurationValue());
        Assertions.assertNull(eventNoteData.getDurationUnits());
    }

    // Line f of the table
    @Test
    void testDatesAndTimesKnown() {
        // Minutes
        eventNoteData.setStartDate(LocalDate.of(2019,2,8));
        eventNoteData.setStartTime(LocalTime.of(8, 35));
        eventNoteData.setEndDate(LocalDate.of(2019, 3,5));
        eventNoteData.setEndTime(LocalTime.of(8, 47));
        Assertions.assertEquals(Integer.valueOf(36012), eventNoteData.getDurationValue());
        Assertions.assertEquals("Minutes", eventNoteData.getDurationUnits());

        // Hours
        eventNoteData.setEndDate(LocalDate.of(2019, 2,25));
        eventNoteData.setEndTime(LocalTime.of(10, 35));
        Assertions.assertEquals(Integer.valueOf(410), eventNoteData.getDurationValue());
        Assertions.assertEquals("Hours", eventNoteData.getDurationUnits());

        // Days
        eventNoteData.setEndDate(LocalDate.of(2019, 2,25));
        eventNoteData.setEndTime(LocalTime.of(8, 35));
        Assertions.assertEquals(Integer.valueOf(17), eventNoteData.getDurationValue());
        Assertions.assertEquals("Days", eventNoteData.getDurationUnits());

        // Weeks
        eventNoteData.setEndDate(LocalDate.of(2019, 3,29));
        eventNoteData.setEndTime(LocalTime.of(8, 35));
        Assertions.assertEquals(Integer.valueOf(7), eventNoteData.getDurationValue());
        Assertions.assertEquals("Weeks", eventNoteData.getDurationUnits());
    }
}
