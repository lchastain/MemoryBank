import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

class EventNoteDataTest {
    private EventNoteData testEventNoteData;

    @BeforeEach
    void setUp() {
        testEventNoteData = new EventNoteData();
        testEventNoteData.setStartDate(LocalDate.of(2018, 10, 31));
        testEventNoteData.setNoteString("Halloween");
        testEventNoteData.setRecurrence("Y_the 31st of October_");
        testEventNoteData.setRetainNote(true);
        testEventNoteData.setIconFileString("icons/smiley2_pumpkin.gif");

    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testClear() {
        Assertions.assertEquals("Halloween", testEventNoteData.getNoteString());
        testEventNoteData.clear();
        Assertions.assertEquals("", testEventNoteData.getNoteString());
    }

    @Test
    void testCopy() {
        EventNoteData end = (EventNoteData) testEventNoteData.copy();
        Assertions.assertEquals(end.getStartDate(), testEventNoteData.getStartDate());
    }

    @Test
    void testGetDurationValue() {
        Long theDuration = testEventNoteData.getDurationValue();
        Assertions.assertNull(theDuration); // Because we have no End Date
    }

    @Test
    void testGetDurationUnits() {
        String theUnits = testEventNoteData.getDurationUnits();
        Assertions.assertEquals(theUnits, "unknown"); // Because we have no End Date
    }

    @Test
    void testGetEndDate() {
    }

    @Test
    void testGetEndTime() {
    }

    @Test
    void testGetEventEnd() {
    }

    @Test
    void testGetEventStart() {
    }

    @Test
    void testGetEventStartDateTime() {
    }

    @Test
    void testGetEventEndDateTime() {
    }

    @Test
    void testGetLocation() {
    }

    @Test
    void testGetRecurrence() {
    }

    @Test
    void testGetRetainNote() {
    }

    @Test
    void testGetRecurrenceSummary() {
    }

    @Test
    void testGetStartDate() {
    }

    @Test
    void testGetStartTime() {
    }

    @Test
    void testGetSummary() {
    }

    @Test
    void testGoForward() {
    }

    @Test
    void testGetDayNoteData() {
    }

    @Test
    void testHasStarted() {
    }

    @Test
    void testIsAnyKnown() {
    }

    @Test
    void testIsAnyUnknown() {
    }

    @Test
    void testIsEndDateKnown() {
    }

    @Test
    void testIsEndTimeKnown() {
    }

    @Test
    void testIsStartDateKnown() {
    }

    @Test
    void testIsStartTimeKnown() {
    }

    @Test
    void testIsTimesKnown() {
    }

    @Test
    void testSetDateFormat() {
    }

    @Test
    void testSetDuration() {
    }

    @Test
    void testSetEndDate() {
    }

    @Test
    void testSetEndTime() {
    }

    @Test
    void testSetLocation() {
    }

    @Test
    void testSetRecurrence() {
    }

    @Test
    void testSetRetainNote() {
    }

    @Test
    void testSetStartDate() {
    }

    @Test
    void testSetStartTime() {
    }
}