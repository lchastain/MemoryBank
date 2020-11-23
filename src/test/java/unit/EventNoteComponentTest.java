import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class EventNoteComponentTest {
    EventNoteGroupPanel myEventNoteGroup;
    EventNoteComponent theEventNoteComponent;

    @BeforeEach
    void setUp() {
        myEventNoteGroup = new EventNoteGroupPanel("testEventGroup");
        myEventNoteGroup.setEditable(false);
        theEventNoteComponent = new EventNoteComponent(myEventNoteGroup, 0);
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
        theEventNoteComponent.resetPanelStatusMessage(NoteComponent.NEEDS_TEXT);
        theEventNoteComponent.setEditable(true);
        theEventNoteComponent = new EventNoteComponent(myEventNoteGroup, 1);
        theEventNoteComponent.resetPanelStatusMessage(NoteComponent.NEEDS_TEXT);
        theEventNoteComponent.resetPanelStatusMessage(NoteComponent.HAS_BASE_TEXT);
        theEventNoteComponent.resetPanelStatusMessage(NoteComponent.HAS_EXT_TEXT);
    }
}