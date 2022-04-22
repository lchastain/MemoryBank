import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.io.File;
import java.time.LocalDate;
import java.time.Month;

// Test the ability of the AppTreePanel to correctly manage the Viewed Date
//   in the face of panel constructions and re-visitations, date selections, and date viewings in
//   several combinations.

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DateTrackingTests {
    private static AppTreePanel appTreePanel;
    private static JTree theTree;
    LocalDate thePanelDate;

    // Remember to clear the tree selection, if you want to try more than one viewedDate
    //   after another, on the same panel.

    @BeforeAll
    static void meFirst() {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testDataLoc = new File(MemoryBank.userDataHome);
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        try {
            FileUtils.cleanDirectory(testDataLoc);
        } catch (Exception e) {
            System.out.println("ignored Exception: " + e.getMessage());
        }

        // The problem with just having this in the BeforeEach was that we started
        // multiple JMenuItem listeners each time, and each test ran so fast that
        // not all of the listeners would have gone away before they were activated
        // by other tests in the suite, causing some confusion.
        appTreePanel = TestUtil.getTheAppTreePanel();

        appTreePanel.restoringPreviousSelection = true; // This should stop the multi-threading.
        appTreePanel.optionPane = new TestUtil();
        theTree = appTreePanel.getTree(); // Usage here means no coverage test needed for getTree().
    }

    @AfterAll
    static void meLast() {
        theTree = null;
        appTreePanel = null;
    }

    // See the 'Date Tracking.txt' doc for a full explanation of the Testing.

    // Start up each of the date-related panels, and verify all dates are 'Today'.
    //     (This is not that much of a test; more like baseline setting).
    @Test
    @Order(1)
    void testInitialDates() {
        LocalDate today = LocalDate.now();

        // YearView - today
        theTree.setSelectionPath(appTreePanel.yearViewPath);
        thePanelDate = appTreePanel.theYearView.displayedYear;
        Assertions.assertEquals(today.getYear(), thePanelDate.getYear());

        // MonthView - today
        theTree.setSelectionPath(appTreePanel.monthViewPath);
        thePanelDate = appTreePanel.theMonthView.displayedMonth;
        Assertions.assertEquals(today.getMonthValue(), thePanelDate.getMonthValue());

        // Day Notes - today
        theTree.setSelectionPath(appTreePanel.dayNotesPath);
        thePanelDate = appTreePanel.theAppDays.getDate();
        Assertions.assertEquals(today, thePanelDate);

        // Month Notes - today
        theTree.setSelectionPath(appTreePanel.monthNotesPath);
        thePanelDate = appTreePanel.theAppMonths.getDate();
        Assertions.assertEquals(today, thePanelDate);

        // Year Notes - today
        theTree.setSelectionPath(appTreePanel.yearNotesPath);
        thePanelDate = appTreePanel.theAppYears.getDate();
        Assertions.assertEquals(today, thePanelDate);
    }

    @Test
    @Order(2)
    void testKnownDate() {

        // a specific date for the AppTreePanel's viewedDate.
        LocalDate knownDate = LocalDate.of(1937, Month.JULY, 15);
        appTreePanel.setViewedDate(knownDate);

        // YearView - knownDate
        theTree.setSelectionPath(appTreePanel.yearViewPath);
        thePanelDate = appTreePanel.theYearView.displayedYear;
        Assertions.assertEquals(knownDate.getYear(), thePanelDate.getYear());

        // MonthView - knownDate
        theTree.setSelectionPath(appTreePanel.monthViewPath);
        thePanelDate = appTreePanel.theMonthView.displayedMonth;
        Assertions.assertEquals(knownDate.getMonthValue(), thePanelDate.getMonthValue());

        // Day Notes - knownDate
        theTree.setSelectionPath(appTreePanel.dayNotesPath);
        thePanelDate = appTreePanel.theAppDays.getDate();
        Assertions.assertEquals(knownDate, thePanelDate);

        // Month Notes - knownDate
        theTree.setSelectionPath(appTreePanel.monthNotesPath);
        thePanelDate = appTreePanel.theAppMonths.getDate();
        Assertions.assertEquals(knownDate, thePanelDate);

        // Year Notes - knownDate
        theTree.setSelectionPath(appTreePanel.yearNotesPath);
        thePanelDate = appTreePanel.theAppYears.getDate();
        Assertions.assertEquals(knownDate, thePanelDate);
    }

}
