import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

class EventNoteGroupPanelTest {
    private static EventNoteGroupPanel eventNoteGroupPanel;

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.userEmail = "test.user@lcware.net";
        MemoryBank.appEnvironment = "ide";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(FileDataAccessor.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        TestUtil.getTheAppTreePanel();
        eventNoteGroupPanel = new EventNoteGroupPanel("holidays");

    }

    @AfterAll
    static void afterAll() throws InterruptedException {
        Thread.sleep(1000);  // Allow for after-test GC
    }


    @Test
    void testDateSelected() {
        eventNoteGroupPanel.dateSelected(LocalDate.now());
    }

    @Test
    void testDefaultIcon() {
        ImageIcon theDefault = eventNoteGroupPanel.getDefaultIcon();
        Assertions.assertNotNull(theDefault);

        // We are setting the one it already has, but this
        // still exercises the code and gets the coverage.
        eventNoteGroupPanel.setDefaultIcon(theDefault);
    }

    @Test
    void testEditExtendedNoteComponent() {
        EventNoteComponent eventNoteComponent = (EventNoteComponent) eventNoteGroupPanel.getNoteComponent(2);
        EventNoteData eventNoteData = (EventNoteData) eventNoteComponent.getNoteData();
        eventNoteGroupPanel.editNoteData(eventNoteData);
    }

}