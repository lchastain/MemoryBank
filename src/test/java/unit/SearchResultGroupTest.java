import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

class SearchResultGroupTest {
    private static SearchResultGroup searchResultGroup;

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

        // We have chosen a known search result (keyword 'food') with 8 hits, so the
        // tests below will know the limitation of indices and text content.
        String theNodeName = "20170527123819";
        String theFileName = MemoryBank.userDataHome + File.separatorChar + "search_" + theNodeName + ".json";
        searchResultGroup = new SearchResultGroup(theFileName);

    }

    @Test
    // Needed after SCR0075
    void testLoadLastModDate() {
        // The other tests here muck too much with the data; need a fresh results list.
        String theNodeName = "20140527192458";
        String theFileName = MemoryBank.userDataHome + File.separatorChar + "search_" + theNodeName + ".json";
        SearchResultGroup srg = new SearchResultGroup(theFileName);

        // We 'know' the Last Mod Date of this one - 6/02/2011
        SearchResultComponent searchResultComponent2 = srg.getNoteComponent(2);
        Date lastModDate = searchResultComponent2.getNoteData().getLastModDate();
        Assertions.assertNotNull(lastModDate);

        // Set the boundaries that we expect to contain the LMD; not currently interested in time of day.
        Calendar cal1 = new GregorianCalendar(2011, Calendar.JUNE, 1);
        Calendar cal2 = new GregorianCalendar(2011, Calendar.JUNE, 3);

        // Verify that we have it right -
        Assertions.assertTrue(lastModDate.after(cal1.getTime()));
        Assertions.assertTrue(lastModDate.before(cal2.getTime()));

        // Of course, an 'equals' test would have been better, but then I would have had to
        // set the time as well - too lazy for that.
    }

    @Test
    void testShiftDown() { // Down, in this case, means visually on-screen.  Numerically it will go up.
        // No significance to the numeric values chosen, other than we know
        // that they are valid choices that could be made.

        // Get the text from a component before the shift, so we can 'follow' it as it goes.
        SearchResultComponent searchResultComponent2 = searchResultGroup.getNoteComponent(2);
        String theText2a = searchResultComponent2.getNoteData().noteString;

        // Do the shift down
        searchResultGroup.shiftDown(2);

        // Get the text again from the same component - verify it's different
        String theText2b = searchResultComponent2.getNoteData().noteString;
        Assertions.assertNotEquals(theText2a, theText2b);

        // Now get the text from the destination, verify it's our original text
        SearchResultComponent searchResultComponent3 = searchResultGroup.getNoteComponent(3);
        String theText3 = searchResultComponent3.getNoteData().noteString;
        Assertions.assertEquals(theText2a, theText3);
    }

    @Test
    void testShiftUp() { // Up, in this case, means visually on-screen.  Numerically it will go down.
        // No significance to the numeric values chosen, other than we know
        // that they are valid choices that could be made.

        // Get the text from a component before the shift, so we can 'follow' it as it goes.
        SearchResultComponent searchResultComponent5 = searchResultGroup.getNoteComponent(5);
        String theText5a = searchResultComponent5.getNoteData().noteString;

        // Do the shift up
        searchResultGroup.shiftUp(5);

        // Get the text again from the same component - verify it's different
        String theText5b = searchResultComponent5.getNoteData().noteString;
        Assertions.assertNotEquals(theText5a, theText5b);

        // Now get the text from the destination, verify it's our original text
        SearchResultComponent searchResultComponent4 = searchResultGroup.getNoteComponent(4);
        String theText4 = searchResultComponent4.getNoteData().noteString;
        Assertions.assertEquals(theText5a, theText4);
    }

    @Test
    void testSortText() {
        // Just the coverage -
        searchResultGroup.sortText(SearchResultGroup.ASCENDING);
        searchResultGroup.sortText(SearchResultGroup.DESCENDING);
    }

    @Test
    void testSaving() {
        // covers multiple methods
        searchResultGroup.setGroupChanged();
        searchResultGroup.preClose();
    }

    @Test
    void testPageNumberChanged() {
        // Just the coverage -
        searchResultGroup.pageNumberChanged();
    }



//    @Test
//    void testPrintList() {
//        searchResultGroup.printList();
//    }
}