import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.awt.event.WindowEvent;

class SearchPanelTest {
    SearchPanel theSearchPanel;

    @BeforeEach
    void setUp() {
        theSearchPanel = new SearchPanel();
    }

    @AfterEach
    void tearDown() {
    }

    @Test
    void testGetSummary() {
    }

    @Test
    void testFoundItFalse() {
        boolean theResult;
        SearchPanelSettings searchPanelSettings;
        String word1 = "bling";
        String word2 = "blang";
        String word3 = "blarg";

        // 'line' comments below refer to the line number of the
        // Excel spreadsheet Truth Table from the docs.
        // That table contains 24 cases where the find result will be false.

        // Test one of the six 'Af' cases (this is line 49)
        // An empty (new) NoteData - one specified keyword, does not get found.
        searchPanelSettings = new SearchPanelSettings();
        NoteData searchingIn = new NoteData();
        searchingIn.setNoteString("Just a note"); // To ensure LMD gets set.
        searchPanelSettings.word1 = word1;
        theSearchPanel.setTheSettings(searchPanelSettings);
        theResult = theSearchPanel.foundIt(searchingIn);
        Assertions.assertFalse(theResult);

        // Test one of the six 'Nfat' cases (this is line 32)
        searchPanelSettings.word1 = word1;
        searchPanelSettings.and1 = true;
        searchPanelSettings.word2 = word2;
        theSearchPanel.setTheSettings(searchPanelSettings);
        searchingIn.setNoteString(word2);
        theResult = theSearchPanel.foundIt(searchingIn);
        Assertions.assertFalse(theResult);

        // Test one of the three 'P1taC3f' cases  (this is line 33)
        searchPanelSettings = new SearchPanelSettings();
        searchPanelSettings.word1 = word1;
        searchPanelSettings.word2 = word2;
        searchPanelSettings.word3 = word3;
        searchPanelSettings.and2 = true;
        searchPanelSettings.or1 = true;
        searchPanelSettings.paren1 = true;
        theSearchPanel.setTheSettings(searchPanelSettings);
        searchingIn.setNoteString(word2);
        theResult = theSearchPanel.foundIt(searchingIn);
        Assertions.assertFalse(theResult);

        // Test the P1faC3t case - (this is line 39)
        // We want to hit this line:  if (!p1) return false;
        searchPanelSettings = new SearchPanelSettings();
        searchPanelSettings.word1 = word1;
        searchPanelSettings.word2 = word2;
        searchPanelSettings.word3 = word3;
        searchPanelSettings.and2 = true;
        searchPanelSettings.or1 = true;
        searchPanelSettings.paren1 = true;
        theSearchPanel.setTheSettings(searchPanelSettings);
        searchingIn.setNoteString(word3);
        theResult = theSearchPanel.foundIt(searchingIn);
        Assertions.assertFalse(theResult);

        // Test one of the two P1foC3f cases - (this is line 35)
        // We want to hit this line:  if (!p1 && !blnC3) return false;
        searchPanelSettings = new SearchPanelSettings();
        searchPanelSettings.word1 = word1;
        searchPanelSettings.word2 = word2;
        searchPanelSettings.word3 = word3;
        searchPanelSettings.and1 = true;
        searchPanelSettings.or2 = true;
        searchPanelSettings.paren1 = true;
        theSearchPanel.setTheSettings(searchPanelSettings);
        searchingIn.setNoteString(word2);
        theResult = theSearchPanel.foundIt(searchingIn);
        Assertions.assertFalse(theResult);

        // Test one of the three 'C1faP2t' cases  (this is line 36)
        // We want to hit this line:  if (!blnC1) return false;
        searchPanelSettings = new SearchPanelSettings();
        searchPanelSettings.word1 = word1;
        searchPanelSettings.word2 = word2;
        searchPanelSettings.word3 = word3;
        searchPanelSettings.and1 = true;
        searchPanelSettings.or2 = true;
        searchPanelSettings.paren2 = true;
        theSearchPanel.setTheSettings(searchPanelSettings);
        searchingIn.setNoteString(word2);
        theResult = theSearchPanel.foundIt(searchingIn);
        Assertions.assertFalse(theResult);

        // Test the C1taP2f case - (this is line 24)
        // We want to hit this line:  return p2;   // C1taP2f
        searchPanelSettings = new SearchPanelSettings();
        searchPanelSettings.word1 = word1;
        searchPanelSettings.word2 = word2;
        searchPanelSettings.word3 = word3;
        searchPanelSettings.and1 = true;
        searchPanelSettings.or2 = true;
        searchPanelSettings.paren2 = true;
        theSearchPanel.setTheSettings(searchPanelSettings);
        searchingIn.setNoteString(word1);
        theResult = theSearchPanel.foundIt(searchingIn);
        Assertions.assertFalse(theResult);

        // Test the C1foP2f case - (this is line 34)
        // We want to hit this line:  return p2 || blnC1;  // C1foP2f - lines 34,40 two cases
        searchPanelSettings = new SearchPanelSettings();
        searchPanelSettings.word1 = word1;
        searchPanelSettings.word2 = word2;
        searchPanelSettings.word3 = word3;
        searchPanelSettings.and2 = true;
        searchPanelSettings.or1 = true;
        searchPanelSettings.paren2 = true;
        theSearchPanel.setTheSettings(searchPanelSettings);
        searchingIn.setNoteString(word2);
        theResult = theSearchPanel.foundIt(searchingIn);
        Assertions.assertFalse(theResult);
    }

    // These methods are just one-liner flag-readers of otherwise private
    // boolean values; just calling them for coverage, since their values
    // don't really matter at this point.
    @Test
    void testSearchBooleans() {
        theSearchPanel.searchGoals();
        theSearchPanel.searchDays();
        theSearchPanel.searchEvents();
        theSearchPanel.searchLists();
        theSearchPanel.searchMonths();
        theSearchPanel.searchYears();
    }

    // Since the Date dialog is a modal window, we need to start our 'closing' thread before
    // the window even appears.  So - we start off with a 'sleep' which will give the
    // SearchPanel time to make and display the dialog, just in time for our thread to wake
    // up and close it.
    @Test
    void testShowDateDialogAndWindowClosing() {
        new Thread(new Runnable() {
            public void run() {
                try {
                    Thread.sleep(1500);
                    WindowEvent we = new WindowEvent(theSearchPanel.dialogWindow, WindowEvent.WINDOW_CLOSING);
                    theSearchPanel.dialogWindow.dispatchEvent(we);
                } catch (InterruptedException ignore) { }
            }
        }).start(); // Start the thread
        theSearchPanel.showDateDialog("Select a date", 1);
    }
}