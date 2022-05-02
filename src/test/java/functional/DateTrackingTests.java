import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

// Test the ability of the AppTreePanel to correctly manage and synchronize the displayed date of the five
//   date-related Panels in the face of panel constructions, reused panels, and panel-initiated date changes.
// See the 'Date Tracking.txt' doc for a full explanation of the Testing.


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

        AppTreePanel.theInstance = null; // We don't want any 'leftovers' from other test runs.
        appTreePanel = TestUtil.getTheAppTreePanel();
        appTreePanel.restoringPreviousSelection = true; // This should stop the multi-threading.
        appTreePanel.optionPane = new TestUtil();
        theTree = appTreePanel.getTree(); // Usage here means no coverage test needed for getTree().
    }

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

    // Tests with a known date (but not Today)
    @Test
    @Order(2)
    void testKnownDate() {
        // Reset the tree, to clear out previously constructed panels.
        AppTreePanel.theInstance = null; // Because we need to test new Panels.
        appTreePanel = TestUtil.getTheAppTreePanel();
        theTree = appTreePanel.getTree(); // Usage here means no coverage test needed for getTree().

        // Check constructions with an 'other than today' date.
        LocalDate knownDate = LocalDate.of(1937, Month.JULY, 15);

        // Making a direct call to the AppTreePanel's AlteredDateListener, in order to
        //   set its viewedDate the way it would happen if a panel had altered its date
        //   to the one being specified here.  Previously was able to call 'setViewedDate'
        //   but that TreePanel method has been deprecated.
        appTreePanel.dateChanged(DateRelatedDisplayType.YEAR_VIEW, knownDate);
       //appTreePanel.setViewedDate(knownDate);

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

        // a new known date for the AppTreePanel's viewedDate.
        knownDate = LocalDate.of(1943, Month.SEPTEMBER, 11);
        appTreePanel.theAppDays.setDate(knownDate);
        appTreePanel.theAppDays.setOneForward(ChronoUnit.DAYS);
        appTreePanel.theAppDays.setOneBack(ChronoUnit.DAYS);
        //appTreePanel.setViewedDate(knownDate);

        // Now check that previously constructed panels also use the new Date -

        // MonthView - new knownDate for a previously constructed panel
        theTree.setSelectionPath(appTreePanel.monthViewPath);
        thePanelDate = appTreePanel.theMonthView.displayedMonth;
        Assertions.assertEquals(knownDate.getMonthValue(), thePanelDate.getMonthValue());

        // Year Notes - new knownDate for a previously constructed panel
        theTree.setSelectionPath(appTreePanel.yearNotesPath);
        thePanelDate = appTreePanel.theAppYears.getDate();
        Assertions.assertEquals(knownDate, thePanelDate);
    }

    // Use Year Notes to add a Year to the viewedDate (vs direct setting in the AppTreePanel), then verify that the
    //   other previously constructed panels now have that same date (without first selecting them on the Tree).
    @Test
    @Order(3)
    void testLargeAlterDate() {
        // Get a date that is one year later than the one used in the last test.
        // This is for later comparison; will get there via a panel increment -
        LocalDate knownDate = LocalDate.of(1944, Month.SEPTEMBER, 11);

        // Move the Year Notes ahead by one year.  This change will be seen on ALL other panels.
        appTreePanel.theAppYears.setOneForward(ChronoUnit.YEARS);

        // Check the Day Notes
        thePanelDate = appTreePanel.theAppDays.getDate();
        Assertions.assertEquals(knownDate, thePanelDate);

        // Check the MonthView
        thePanelDate = appTreePanel.theMonthView.displayedMonth;
        Assertions.assertEquals(knownDate.getMonthValue(), thePanelDate.getMonthValue());
    }

    // Use Day Notes to subtract a Day from the viewedDate (vs direct setting in the AppTreePanel), then verify that
    //   the other previously constructed panels now have that same date (without first selecting them on the Tree).
    @Test
    @Order(4)
    void testSmallAlterDate() {
        // Get the date used in the previous test -
        LocalDate knownDate = LocalDate.of(1944, Month.SEPTEMBER, 11);

        // Move the Day Notes back by one day.
        // This change should be seen on all other panels, even though it will not cause
        //   a reload of MonthNotes or YearNotes data.
        appTreePanel.theAppDays.setOneBack(ChronoUnit.DAYS);

        // Check the Day Notes - verify the change
        thePanelDate = appTreePanel.theAppDays.getDate();
        Assertions.assertEquals(knownDate.minusDays(1), thePanelDate);

        // Check the Year Notes - verify the change
        thePanelDate = appTreePanel.theAppYears.getDate();
        Assertions.assertEquals(knownDate.minusDays(1), thePanelDate);

        // Check the YearView - verify the change
        thePanelDate = appTreePanel.theYearView.displayedYear;
        Assertions.assertEquals(knownDate.minusDays(1), thePanelDate);
    }
}
