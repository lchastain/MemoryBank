import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.tree.TreePath;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

public class ArchiveTreePanelTest {
    private static ArchiveTreePanel archiveTreePanel;
    private static JTree theTree;
    static String archiveName;

    @BeforeAll
    static void meFirst() throws IOException {
        System.out.println("ArchiveTreePanel Test");
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.userEmail = "test.user@lcware.net";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(FileDataAccessor.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception ignore){
            System.out.println("Was unable to clean the test.user@lcware.net directory!");
        }

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(ArchiveTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();

        // We need an AppTreePanel instance -
        TestUtil.getTheAppTreePanel();

        // If this is not a 'good' archive name, we will see a failure to parse it into a LocalDate.
        // If it is just not available for some reason, the options load will fail.
        // Having a good archive is a prerequisite to the tests here; we are not testing the 'bad archive'
        //   cases in this class.
        archiveName = "2021-01-27  8:24:33 AM";
        archiveTreePanel = new ArchiveTreePanel(archiveName);

        archiveTreePanel.optionPane = new TestUtil();
        theTree = archiveTreePanel.getTree(); // Usage here means no united test needed for getTree().

    } // end meFirst

    @AfterEach
    void tearDown() throws InterruptedException {
        // These tests drive the app faster than it would go if it was only under user control.
        Thread.sleep(500); // Otherwise we see NullPointerExceptions after tests pass.
    }

    @AfterAll
    static void meLast() {
        theTree = null;
        archiveTreePanel = null;
        AppTreePanel.theInstance.closeArchive(archiveName);
    }

    @Test
    void testShowDay() {
        archiveTreePanel.showDay();
        TreePath tp = theTree.getSelectionPath();
        Assertions.assertNotNull(tp);
        assert tp.getLastPathComponent().toString().equals("Day Notes");
    }

    // This is testing the cases where the data is NOT found.
    @Test
    void testShowFoundIn() throws InterruptedException {
        SearchResultData mySrd = new SearchResultData(new NoteData());
        mySrd.foundIn = new GroupInfo("2008", GroupType.YEAR_NOTES);
        archiveTreePanel.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.foundIn = new GroupInfo("February 2008", GroupType.MONTH_NOTES);
        archiveTreePanel.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.foundIn = new GroupInfo("Saturday, September 27, 2008", GroupType.DAY_NOTES);
        archiveTreePanel.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.foundIn = new GroupInfo("Long Term", GroupType.TODO_LIST);
        archiveTreePanel.showFoundIn(mySrd);
    }


    @Test
    void testShowMonthView() {
        // For the test user there is icon data in this month; needs to have
        //   icon data to get the coverage we're looking for here.
        LocalDate theMonthToShow = LocalDate.of(2019, 7, 15);
        archiveTreePanel.theMonthView = new MonthView(theMonthToShow);
        archiveTreePanel.showMonthView();
        TreePath tp = theTree.getSelectionPath();
        Assertions.assertNotNull(tp);
        assert tp.getLastPathComponent().toString().equals("Month View");
        System.out.println("End testShowMonthView");
    }

    @Test
    void testShowWeek() {
        archiveTreePanel.showWeek(LocalDate.now());
        TreePath tp = theTree.getSelectionPath();
        Assertions.assertNotNull(tp);
        String theSelection = tp.getLastPathComponent().toString();
        Assertions.assertEquals("Week View", theSelection);
        System.out.println("End testShowWeek");
    }

}