import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

class GoalGroupPanelTest {
    private static GoalGroupPanel goalGroup;
    static TestUtil testUtil;

    @BeforeAll
    static void beforeAll() throws IOException {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        FileUtils.cleanDirectory(testData);

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        goalGroup = new GoalGroupPanel("Graduate");
        testUtil = new TestUtil();
        GoalGroupPanel.optionPane = testUtil;
    }

    @AfterAll
    static void afterAll() throws InterruptedException {
        Thread.sleep(1000);  // Allow for after-test GC
    }


    @Test
    public void coverageTest() {
        goalGroup.preClosePanel();
        goalGroup.prependNote(new LogNoteData());
        goalGroup.clearAllNotes();
        goalGroup.renamePanel("Dominate");
        goalGroup.saveAs();

        // Off-the-rails tests, to get NoteGroup coverage in places that are
        //   otherwise inappropriate to this class test.
        goalGroup.myNoteGroup.setNotes(null);
        goalGroup.myNoteGroup.myProperties = null;
        goalGroup.myNoteGroup.myGroupInfo = new GroupInfo("badName", GroupType.DAY_NOTES);
        goalGroup.myNoteGroup.getGroupProperties();
        goalGroup.myNoteGroup.myProperties = null;
        goalGroup.myNoteGroup.myGroupInfo = new GroupInfo("badName", GroupType.MONTH_NOTES);
        goalGroup.myNoteGroup.getGroupProperties();
        goalGroup.myNoteGroup.myProperties = null;
        goalGroup.myNoteGroup.myGroupInfo = new GroupInfo("badName", GroupType.YEAR_NOTES);
        goalGroup.myNoteGroup.getGroupProperties();
    }

}