import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.ZonedDateTime;

class SearchResultGroupTest {
    private static SearchResultGroupPanel searchResultGroup;

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
    }

    @BeforeEach
    void beforeEach() throws InterruptedException {
        // We have chosen a known search result, so the
        // tests below will know the limitation of indices and text content.
        String theNodeName = "20190927161325";
        searchResultGroup = (SearchResultGroupPanel) NoteGroupFactory.loadNoteGroup("Search Result", theNodeName);
        Thread.sleep(200); // Tests need some settling time.
    }

    // Needed after SCR0075
    // This tests that the display of the LastModDate will use the stored date, vs 'today'.
    @Test
    void testLoadLastModDate() {
        // The other tests here muck too much with the data; need a fresh results list.
        String theFileName = "20191029073938";  // Search text 'Office'
        SearchResultGroupPanel srg = (SearchResultGroupPanel) NoteGroupFactory.loadNoteGroup("Search Result", theFileName);
        Assertions.assertNotNull(srg);

        // Get the LMD of the third (index is zero-based) visible component in this group.
        SearchResultComponent searchResultComponent2 = srg.getNoteComponent(2);
        ZonedDateTime lastModDateTime = searchResultComponent2.getNoteData().getLastModDate();
        Assertions.assertNotNull(lastModDateTime);
        LocalDate lastModDate = lastModDateTime.toLocalDate();

        // We 'know' the Last Mod Date of this one - 02 October 2019
        // Verify that we have it right -
        Assertions.assertEquals(lastModDate, LocalDate.of(2019, 10, 2));
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
    void testSortLastMod() {
        // Just the coverage -
        searchResultGroup.sortLastMod(NoteGroupPanel.ASCENDING);
        searchResultGroup.sortLastMod(NoteGroupPanel.DESCENDING);
    }

    @Test
    void testSortText() {
        // Just the coverage -
        searchResultGroup.sortText(NoteGroupPanel.ASCENDING);
        searchResultGroup.sortText(NoteGroupPanel.DESCENDING);
    }

    @Test
    void testSaving() {
        // covers multiple methods
        searchResultGroup.setGroupChanged(true);
        searchResultGroup.preClosePanel();
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