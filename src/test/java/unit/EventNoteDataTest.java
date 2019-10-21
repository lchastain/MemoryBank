import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

// Coverage for the methods for setting Dates, Times, and Duration - is taken care
// of by the functional tests for Duration setting and Duration calculation;
// no need to include trivial calls to them here.

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
    void testGetSummary() {
    }

    @Test
    void testGoForward() {
    }

    @Test
    void testHasStarted() {
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

}