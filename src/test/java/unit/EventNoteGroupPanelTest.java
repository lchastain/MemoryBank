import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

class EventNoteGroupPanelTest {
    private static EventNoteGroupPanel eventNoteGroup;
    static TestUtil testUtil;

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

        eventNoteGroup = new EventNoteGroupPanel("holidays");
        testUtil = new TestUtil();
        EventNoteGroupPanel.optionPane = testUtil;
    }

    @AfterAll
    static void afterAll() throws InterruptedException {
        Thread.sleep(1000);  // Allow for after-test GC
    }


        @Test
    void testDateSelected() {
        eventNoteGroup.dateSelected(LocalDate.now());
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
    void testMerge() {
        // Just the coverage -
        testUtil.setTheAnswer("appointments");
        eventNoteGroup.merge();
    }

}