import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

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
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
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
        AppTreePanel appTreePanel = TestUtil.getTheAppTreePanel();

        // If this is not a 'good' archive name, we will see a failure to parse it into a LocalDate.
        // If it is just not available for some reason, the options load will fail.
        // Having a good archive is a prerequisite to the tests here; we are not testing the 'bad archive'
        //   cases in this class.
        archiveName = "2021-01-27  8:24:33 AM";
        archiveTreePanel = new ArchiveTreePanel(archiveName);

        archiveTreePanel.optionPane = new TestUtil();
        theTree = archiveTreePanel.getTree(); // Usage here means no unit test needed for getTree().

    } // end meFirst

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

    @Test
    void testShowFoundIn() throws Exception {
        SearchResultData mySrd = new SearchResultData(new NoteData());
        mySrd.setFileFoundIn(new File("2008/Y_20190301095623"));
        archiveTreePanel.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.setFileFoundIn(new File("2008/M02_20190208182959"));
        archiveTreePanel.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.setFileFoundIn(new File("2008/D0927_20170927175850"));
        archiveTreePanel.showFoundIn(mySrd);
        Thread.sleep(300); // The test framework can drive the app too fast.
        mySrd.setFileFoundIn(new File("todo_Long Term.json"));
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