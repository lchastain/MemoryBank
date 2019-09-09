import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Date;

class EventNoteGroupTest {
    private static EventNoteGroup eventNoteGroup;

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("jondo.nonamus@lcware.net");

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

        // We have chosen a known search result (keyword 'food') with 8 hits, so the
        // tests below will know the limitation of indices and text content.
        String theNodeName = "S20170527123819";
        String theFileName = MemoryBank.userDataHome + File.separatorChar + theNodeName + ".sresults";
        eventNoteGroup = new EventNoteGroup();
        TestUtil testUtil = new TestUtil();
        eventNoteGroup.setNotifier(testUtil);
    }


    @Test
    void testDateSelected() {
        eventNoteGroup.dateSelected(new Date());
    }

    @Test
    void testDefaultIcon() {
        AppIcon theDefault = eventNoteGroup.getDefaultIcon();
        Assertions.assertNotNull(theDefault);

        // We are setting the one it already has, but this
        // still exercises the code and gets the coverage.
        eventNoteGroup.setDefaultIcon(theDefault);
    }

    @Test
    void testGetGroupFilename() {
        String theFileName = eventNoteGroup.getGroupFilename();
        Assertions.assertNotNull(theFileName);
    }

    @Test
    void testEditExtendedNoteComponent() {
        EventNoteComponent eventNoteComponent = (EventNoteComponent) eventNoteGroup.getNoteComponent(2);
        EventNoteData eventNoteData = (EventNoteData) eventNoteComponent.getNoteData();
        eventNoteGroup.editExtendedNoteComponent(eventNoteData);
    }

    @Test
    void testRefresh() {
        eventNoteGroup.refresh();
    }

}