import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

class NoteComponentTest {
    NoteComponent theNoteComponent;
    private static TodoNoteGroupPanel todoNoteGroup;

    @BeforeAll
    static void beforeAll() throws IOException {
        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.userEmail = "test.user@lcware.net";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        TestUtil.getTheAppTreePanel();

        // Remove any pre-existing Test data
        File testData = new File(FileDataAccessor.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        todoNoteGroup = new TodoNoteGroupPanel("Get New Job");
    }

    @BeforeEach
    void setUp() {
        theNoteComponent = todoNoteGroup.getNoteComponent(3);
    }

    @Test
    void testGetNoteTextField() {
        NoteComponent.NoteTextField noteTextField;
        noteTextField = (NoteComponent.NoteTextField) theNoteComponent.getNoteTextField();
        noteTextField.getPreferredSize();
        noteTextField.getToolTipLocation(new MouseEvent(noteTextField, MouseEvent.MOUSE_ENTERED, 0,0,0,0, 0, false ));
    }

    @Test
    void testGetTextStatus() {
        theNoteComponent.getTextStatus();
        theNoteComponent = todoNoteGroup.getNoteComponent(4);
        theNoteComponent.getTextStatus();
        NoteComponent.NoteTextField noteTextField;
        noteTextField = (NoteComponent.NoteTextField) theNoteComponent.getNoteTextField();
        noteTextField.setText(" ");
        theNoteComponent.getTextStatus();
        theNoteComponent = todoNoteGroup.getNoteComponent(7);
        theNoteComponent.getTextStatus();
    }

    @Test
    void testResetNoteStatusMessage() {
        MonthNoteGroupPanel monthNoteGroup = new MonthNoteGroupPanel();
        NoteComponent noteComponent = monthNoteGroup.getNoteComponent(0);
        noteComponent.resetPanelStatusMessage(NoteComponent.NEEDS_TEXT);
        noteComponent.resetPanelStatusMessage(NoteComponent.HAS_BASE_TEXT);
        noteComponent.resetPanelStatusMessage(NoteComponent.HAS_EXT_TEXT);
    }

    @Test
    void testResetPopup() {
        theNoteComponent.resetPopup();
        theNoteComponent = todoNoteGroup.getNoteComponent(7);
        theNoteComponent.resetPopup();
    }

}