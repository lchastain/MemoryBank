import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventNoteComponentTest {
    EventNoteGroup myEventNoteGroup;
    EventNoteComponent theEventNoteComponent;

    @BeforeEach
    void setUp() {
        NoteComponent.isEditable = false;
        myEventNoteGroup = new EventNoteGroup("testEventGroup");
        theEventNoteComponent = new EventNoteComponent(myEventNoteGroup, 0);
    }

    @AfterEach
    void tearDown() {
        NoteComponent.isEditable = true;
    }

    @Test
    void testMakeDataObject() {
        theEventNoteComponent.makeDataObject();
    }

    @Test
    void testNoteActivated() {
        theEventNoteComponent.noteActivated(true);
    }

    @Test
    void testResetNoteStatusMessage() {
        theEventNoteComponent.resetNoteStatusMessage(NoteComponent.NEEDS_TEXT);
        NoteComponent.isEditable = true;
        theEventNoteComponent = new EventNoteComponent(myEventNoteGroup, 1);
        theEventNoteComponent.resetNoteStatusMessage(NoteComponent.NEEDS_TEXT);
        theEventNoteComponent.resetNoteStatusMessage(NoteComponent.HAS_BASE_TEXT);
        theEventNoteComponent.resetNoteStatusMessage(NoteComponent.HAS_EXT_TEXT);
    }
}