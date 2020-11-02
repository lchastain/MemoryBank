import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import java.io.File;
import java.time.LocalDate;
import java.time.Month;
import java.time.temporal.ChronoUnit;

// Test the ability of the AppTreePanel to correctly manage the Selected Date and the Viewed Date
//   in the face of panel constructions and revisitations, date selections, and date viewings in
//   several different combinations.

@SuppressWarnings("BusyWait")
public class DateTrackingTests {
    private static AppTreePanel appTreePanel;
    private static JTree theTree;
    private static AppMenuBar amb;
    private LocalDate today = LocalDate.now();

    @BeforeAll
    static void meFirst() {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");

        // Remove any pre-existing Test data
        File testDataLoc = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testDataLoc);
        } catch (Exception e) {
            System.out.println("ignored Exception: " + e.getMessage());
        }

        // The problem with just having this in the BeforeEach was that we started
        // multiple JMenuItem listeners each time, and each test ran so
        // fast that not all of the listeners would have gone
        // away before they were activated by other tests, causing some confusion.
        appTreePanel = new AppTreePanel(new JFrame(), MemoryBank.appOpts);

        appTreePanel.restoringPreviousSelection = true; // This should stop the multi-threading.
        // Note that it does not (currently) ever get un-set by the AppTreePanel itself.

        appTreePanel.optionPane = new TestUtil();
        theTree = appTreePanel.getTree(); // Usage here means no unit test needed for getTree().
        amb = AppTreePanel.appMenuBar;
    }

    @AfterAll
    static void meLast() {
        theTree = null;
        amb = null;
        //appTreePanel.restoringPreviousSelection = false;
        appTreePanel = null;
    }

    // This test comes close to the one that is written (textually) in SCR0106.  But this one starts
    // with a known date which is not going to be the 'current' date.  This is better because the
    // default of new panels is to use current date, and we want to see proper handling of selected
    // and viewed dates without getting lucky by having one or both be the default and therefore already
    // properly set.  But one shortfall with this version is that we lack the test for 'looking' at
    // the day highlighted on the MonthView and YearView.  This seems acceptable given that we do
    // consider the text of the reported 'choice', which in code is set at/near the same place as the
    // highlighting of the specific day and based on that same value.
    @Test
    void testDateManagement1() throws InterruptedException {
        LocalDate theChoice;

        // 1.  Start up a YearView with a known date.  This will cause it to be constructed
        //     and use the known date as both the selected date and the viewed date.
        LocalDate knownDate = LocalDate.of(today.getYear(), Month.JULY, 15);
        appTreePanel.setSelectedDate(knownDate);
        theTree.setSelectionPath(appTreePanel.yearViewPath);

        // Give the AppTreePanel thread time to create the YearView
        while(appTreePanel.theYearView == null) {
            Thread.sleep(500);
        }

        // Verify that the YearView's displayed year (Viewed Date) is correct.
        // In this case there is limited (no ?) value added for this assertion since
        // the two dates have not diverged in this test, but checking it anyway for
        // completeness and to match the same steps for the MonthView, where we do
        // expect to see a difference.
        Assertions.assertEquals(appTreePanel.theYearView.getYear(), knownDate.getYear());

        // Verify that the YearView's selection and our known date are the same.
        theChoice = appTreePanel.theYearView.getChoice();
        Assertions.assertTrue(theChoice.isEqual(knownDate));

        // Now look at the Label for the selected date
        String whatItShouldBe = YearView.dtf.format(knownDate);
        String whatItIs = appTreePanel.theYearView.getChoiceLabelText();
        Assertions.assertEquals(whatItShouldBe, whatItIs);

        // 2.  Switch to a Month View of a different month in this same year, verify that it
        //     displays the correct (viewed) month while retaining the selected date.
        LocalDate viewedDate = LocalDate.of(today.getYear(), Month.MARCH, 1);
        // These two steps replicate the result of a user having clicked on 'March' on the YearView
        appTreePanel.setViewedDate(viewedDate, ChronoUnit.MONTHS);
        appTreePanel.showMonthView();

        // Give the AppTreePanel thread time to create the MonthView
        while(appTreePanel.theMonthView == null) {
            Thread.sleep(500);
        }

        // Verify that the MonthView's displayed month (Viewed Date) is correct.
        Assertions.assertTrue(MonthView.displayedMonth.isEqual(viewedDate));

        // Verify that the MonthView's selection and our known date are the same.
        theChoice = appTreePanel.theMonthView.getChoice();
        Assertions.assertTrue(theChoice.isEqual(knownDate)); // knownDate was set in Step 1.

        // Now look at the Label for the selected date
        whatItShouldBe = MonthView.dtf.format(knownDate);
        whatItIs = appTreePanel.theMonthView.getChoiceLabelText();
        Assertions.assertEquals(whatItShouldBe, whatItIs);

        // 3.  Go back to the YearView
        theTree.setSelectionPath(appTreePanel.yearViewPath);

        // Verify that the YearView didn't pick up any info from the MonthView's displayedMonth date.
        theChoice = appTreePanel.theYearView.getChoice();
        Assertions.assertTrue(theChoice.isEqual(knownDate));
        whatItShouldBe = YearView.dtf.format(knownDate);
        whatItIs = appTreePanel.theYearView.getChoiceLabelText();
        Assertions.assertEquals(whatItShouldBe, whatItIs);

// 12/17/2019 - Disabled the rest of this test, because a mismatch was discovered between initial implementation
//   and the intended design, and it was fixed in AppTreePanel.  Now that any YearView will change granularity to YEARS,
//   the remaining steps here have improper expectations.
//   Need to revise these steps and/or just end here and setNotes different tests.

//        // 4.  Go to MonthNotes.  Verify that it shows Notes for the viewed date.
//        theTree.setSelectionPath(appTreePanel.monthNotesPath);
//        while(appTreePanel.theAppMonths == null) {
//            // Give the AppTreePanel thread time to create the MonthView
//            Thread.sleep(500);
//        }
//        theChoice = appTreePanel.theAppMonths.getChoice();
//        Assertions.assertTrue(theChoice.isEqual(viewedDate));
//
//        // 5.  Go back to the Month View.  Verify it still shows the view for the month
//        //     you previously viewed (the Viewed Date).
//
//        // This time we get there via the Tree selection and not a YearView-month click
//        theTree.setSelectionPath(appTreePanel.monthViewPath);
//
//        // The same tests as we did earlier, with fewer comments.
//        Assertions.assertTrue(MonthView.displayedMonth.isEqual(viewedDate));
//        theChoice = appTreePanel.theMonthView.getChoice();
//        Assertions.assertTrue(theChoice.isEqual(knownDate)); // knownDate was set in Step 1.
//        whatItShouldBe = MonthView.dtf.format(knownDate);
//        whatItIs = appTreePanel.theMonthView.getChoiceLabelText();
//        Assertions.assertEquals(whatItShouldBe, whatItIs);
//
//        // 6.  Go to Day Notes.  Verify it shows the notes for the Selected Date.
//        theTree.setSelectionPath(appTreePanel.dayNotesPath);
//
//        // Give the AppTreePanel thread time to create the DayNoteGroup
//        while(appTreePanel.theAppDays == null) {
//            Thread.sleep(500);
//        }
//
//        // Verify the date shown is the Selected Date.
//        theChoice = appTreePanel.theAppDays.getChoice();
//        Assertions.assertTrue(theChoice.isEqual(knownDate)); // knownDate was set in Step 1.
//
//        // Increase the day by two
//        knownDate = knownDate.plusDays(2);
//        appTreePanel.theAppDays.setOneForward();
//        appTreePanel.theAppDays.setOneForward();
//        theChoice = appTreePanel.theAppDays.getChoice();
//        Assertions.assertTrue(theChoice.isEqual(knownDate));
//        // (with DayNotes, the choice is both the Selected and the Viewed Date).
//
//        // 7.  Go to Month View.  Verify it shows the selected month, with the new selected date.
//        theTree.setSelectionPath(appTreePanel.monthViewPath);
//        theChoice = appTreePanel.theMonthView.getChoice();
//        Assertions.assertTrue(theChoice.isEqual(knownDate));  // knownDate was set in step 6.
//        whatItShouldBe = MonthView.dtf.format(knownDate);
//        whatItIs = appTreePanel.theMonthView.getChoiceLabelText();
//        Assertions.assertEquals(whatItShouldBe, whatItIs);
//
//        // 8.  Go to the Year View.  Verify it shows the selected month, with the new selected date.
//        theTree.setSelectionPath(appTreePanel.yearViewPath);
//        theChoice = appTreePanel.theYearView.getChoice();
//        Assertions.assertTrue(theChoice.isEqual(knownDate));  // knownDate was set in step 6.
//        whatItShouldBe = YearView.dtf.format(knownDate);
//        whatItIs = appTreePanel.theYearView.getChoiceLabelText();
//        Assertions.assertEquals(whatItShouldBe, whatItIs);
    }

}
