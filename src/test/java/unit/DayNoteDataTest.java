import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.LocalTime;

class DayNoteDataTest {

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
    }

    // The assertions below are not actually needed; no constructor is going to
    // ever return a null.  But we assert anyway, so that the newly constructed
    // variables don't show as going unused.
    @Test
    void testAlternateConstructors() {
        DayNoteData dayNoteData;

        // Construct from a TodoNoteData
        TodoNoteData todoNoteData = new TodoNoteData();
        dayNoteData = new DayNoteData(todoNoteData);
        Assertions.assertNotNull(dayNoteData);
        // Again, but this time cover two alternate paths -
        todoNoteData.setPriority(8);
        todoNoteData.setStatus(TodoNoteData.TODO_COMPLETED);
        dayNoteData = new DayNoteData(todoNoteData);
        Assertions.assertNotNull(dayNoteData);

        // Construct from an EventNoteData
        EventNoteData eventNoteData = new EventNoteData();
        dayNoteData = new DayNoteData(eventNoteData);
        Assertions.assertNotNull(dayNoteData);
        // Again, but this time cover two alternate paths -
        eventNoteData.setStartTime(LocalTime.now());
        dayNoteData = new DayNoteData(eventNoteData);
        Assertions.assertNotNull(dayNoteData);

        // Construct from a NoteData
        NoteData noteData = new NoteData();
        dayNoteData = new DayNoteData(noteData);
        Assertions.assertNotNull(dayNoteData);

        // Cover both paths to the copy constructor
        DayNoteData aCopy = new DayNoteData(dayNoteData);
        Assertions.assertNotNull(aCopy);
        noteData = dayNoteData.copy();
        Assertions.assertNotNull(noteData);
    }


    @Test
    void testSetTimeOfDayString() {
        String theTime = "11:04";
        DayNoteData dayNoteData = new DayNoteData();
        dayNoteData.setTimeOfDayString(theTime);
        String todString = dayNoteData.getTimeOfDayString();
        Assertions.assertEquals(theTime, todString);
    }
}