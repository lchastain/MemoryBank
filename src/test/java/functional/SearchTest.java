import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;

@SuppressWarnings("BusyWait")
//@Disabled
public class SearchTest {
    private static AppTreePanel appTreePanel;
    private static JTree theTree;
    private static AppMenuBar amb;

    @BeforeAll
    static void meFirst() throws IOException {
        MemoryBank.debug = true;

        // Set the location for our user data (the directory will be created, if not already there)
        MemoryBank.setUserDataHome("test.user@lcware.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception ignore) {
            System.out.println("Was unable to clean the test.user@lcware.net directory!");
        }

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        assert testResource != null;
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();

        // The problem with just having this in the BeforeEach was that we started
        // multiple JMenuItem listeners with each new atp, and each test ran so
        // fast that not all of the listeners would have gone
        // away before they were activated by other tests, causing some confusion.
        appTreePanel = TestUtil.getTheAppTreePanel();
        appTreePanel.restoringPreviousSelection = true; // This should stop the multi-threading.

        appTreePanel.optionPane = new TestUtil();
        theTree = appTreePanel.getTree(); // Usage here means no united test needed for getTree().
        amb = appTreePanel.getAppMenuBar();

        // No significance to this value other than it needs to be a row that
        // we know for a fact will be there, even for a brand-new AppTreePanel.
        // In this case we've chosen a relatively low (safer) value.
        int theSelectionRow = 3;
    }

    @AfterAll
    static void meLast() {
        theTree = null;
        amb = null;
        appTreePanel = null;
    }

    // There are other tests that run a search, but this is the only one that tests against a LastMod date.
    @Test
    void testDoSearch() throws InterruptedException {
        appTreePanel.searchPanel = new SearchPanel(); // Avoid a null exception in doSearch, when called directly.
        SearchPanelSettings searchPanelSettings = appTreePanel.searchPanel.getSettings();

        // Select the Last Modified 'After' radio button (all other settings remain at their defaults).
        JRadioButton jRadioButton = appTreePanel.searchPanel.rbtnModAfter;
        jRadioButton.setSelected(true);

        // But programmatically setting the button to selected did not kick off the mouse listener; we need to
        //   set the date value directly -
        searchPanelSettings.modChoice = 3; // AFTER
        searchPanelSettings.dateLastMod1String = LocalDate.of(2019, 5, 15).toString();
        appTreePanel.searchPanel.loadTheSettings();

        // And now send those settings to the method to kick off the search.
        appTreePanel.doSearch(appTreePanel.searchPanel);

        // Allow time for the search to complete.
        while(appTreePanel.searching) {
            Thread.sleep(500);
        }

        // At current writing, the 'found' results is 139 and will probably climb higher,
        // over time so here we just check that some large number of records has been found.
        Assertions.assertTrue(appTreePanel.getTheNoteGroupPanel().myNoteGroup.noteGroupDataVector.size() > 100);

        // This is useful if running as the user after this test has completed, to see how the tree looks now.
        AppOptions.saveOpts();  // (preClose, at the AppTreePanel level - doesn't save opts).
    }

// This much earlier version has some mouse events and threads for menu item, radio button,
//    and date selections that should not be lost - keep here indefinitely as examples, but
//    with recent changes to branch selection behaviors (tested by BranchSelectionTests), this one
//    will not work correctly; it was rewritten when it began to 'hang' here, now that selection
//    of the branch could invoke a new SearchPanel.  And that's just the tip of the potential
//    issues with doing it this way - do not 'repair' this methodology; the one above is much
//    much better to accomplish the goals of the test.
//    @Test
//    void testDoSearch() throws InterruptedException {
//        // Bring up the SearchPanel by 'clicking' on the Search menu item.  The menu bar handler in
//        //   AppTreePanel will send us to its 'prepareSearch()' method.
//        JMenuItem jmi = AppUtil.getMenuItem(new JMenu("App"), "Search...");
//        jmi.doClick(); // You could see multiple effects from this, if the other tests leave behind JMenuItem listeners.
//
//        // Allow time for the search panel to be displayed.
//        while(!appTreePanel.searching) {
//            Thread.sleep(1000);
//        }
//
//        // Select the Last Modified 'After' radio button (all other settings remain at their defaults).
//        JRadioButton jRadioButton = appTreePanel.searchPanel.rbtnModAfter;
//        jRadioButton.setSelected(true);
//
//        // But programmatically setting the button to selected did not kick off the mouse listener, and
//        // if it had, the date selection modal dialog would have come up and locked out any other processing,
//        // so we can bring it up now but need  to carefully control the timing of what should happen:
//        // 1.  Start a thread with a delay.  Meanwhile we run past that and:
//        // 2.  Send a MouseEvent to the radio button listener, to bring up the modal date selection dialog.
//        // 3.  Sleep until the date selection dialog is showing
//        // 4.  Make a MouseEvent selection on a (virtual) DayLabel whose handler will accept the 'input' that
//        //      it got, close the dialog, and take us back to the basic SearchPanel that will be waiting
//        //      for us to click the 'Search Now' button.
//        // 5.  Programmatically click the 'Search Now' button.
//
//        new Thread(new Runnable() {
//            public void run() {
//                try {
//                    // Wait for the modal date selection dialog to appear.
//                    Thread.sleep(1500);    // Step 1 - sleep until we get past step 2.
//
//                    // Step 3, in the notes above (for the rest of this thread).
//                    // On the YearView dialog window we want to select May 15th, 2019.
//                    // Since it is in a previous year it will not show - that's ok, for the purposes of this test.
//                    LocalDate ld = LocalDate.of(2019, 5, 15); // We know there is data here.
//
//                    // Make a new 'virtual' DayLabel - it has the same handler as the 'real' ones.
//                    YearView.DayLabel dayLabel = appTreePanel.searchPanel.yvDateChooser.new DayLabel();
//                    // Did anyone notice how awesome was the above statment that instantiated that DayLabel?  Wow.
//                    dayLabel.setText("15"); // The handler doesn't listen to DayLabels that have no text.
//                    dayLabel.myDate = ld;  // Set this DayLabel's date to the one we want to use.
//
//                    // Get the MouseListener for this DayLabel
//                    MouseListener[] mouseListenerArray2 = dayLabel.getMouseListeners();
//
//                    // Create a MousePressed event, and send it to the listener.
//                    MouseEvent dayLabelPressed = new MouseEvent(dayLabel, MouseEvent.MOUSE_PRESSED,
//                            1515, 0, 2, 2, 0, false);
//                    mouseListenerArray2[0].mousePressed(dayLabelPressed);
//
//                } catch (InterruptedException ignore) { }
//            }
//        }).start(); // Start the thread
//
//        // Step 2, in the note above -
//        // Simulate a Mouse click on the 'Last Modified After' radio button
//        MouseEvent modAfterClicked = new MouseEvent(jRadioButton, MouseEvent.MOUSE_CLICKED,
//                158011902, 16, 10, 11, 0, false);
//        MouseListener[] mouseListenerArray1 = jRadioButton.getMouseListeners();
//        mouseListenerArray1[1].mouseClicked(modAfterClicked);  // The framework has a listener at index zero.
//        // And now the modal date selection dialog should be up and awaiting an input, that we hope it
//        // will get from our thread that was started above and has been patiently awaiting for this moment.
//
//        // 3.  Sleep until the date selection dialog (invoked by step 2) is showing
//        while(appTreePanel.searching) {
//            Thread.sleep(1500);
//
//            // Step 4 in the note above (programmatically click on the 'Search Now' button of the SearchPanel) - will
//            // not be done after all (because I don't know how).  Instead we will just close the dialog, and the flag
//            // that we have set above will get us around the implied 'cancel'.  Of course extra code was added to
//            // AppTreePanel to accomplish this, and that was wrong (but not wrong enough to make me want to change it).
////            JOptionPane.getRootFrame().dispose();
//        }
//
//        // At current writing, the 'found' results is 139 and will probably climb higher,
//        // over time so here we just check that some large number of records has been found.
//        Assertions.assertTrue(appTreePanel.getTheNoteGroup().myNoteGroup.noteGroupDataVector.size() > 100);
//    }

}