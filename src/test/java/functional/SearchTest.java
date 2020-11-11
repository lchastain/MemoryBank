import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.*;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.*;
import java.time.LocalDate;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

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

        // Remove any pre-existing Test data
        File testData = new File(MemoryBank.userDataHome);
        try {
            FileUtils.cleanDirectory(testData);
        } catch (Exception ignore){
            System.out.println("Was unable to clean the test.user@lcware.net directory!");
        }

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testData);

        // Load up this Test user's application options
        AppOptions.loadOpts();

        // The problem with just having this in the BeforeEach was that we started
        // multiple JMenuItem listeners with each new atp, and each test ran so
        // fast that not all of the listeners would have gone
        // away before they were activated by other tests, causing some confusion.
        appTreePanel = new AppTreePanel(new JFrame(), MemoryBank.appOpts);
        appTreePanel.restoringPreviousSelection = true; // This should stop the multi-threading.

        appTreePanel.optionPane = new TestUtil();
        theTree = appTreePanel.getTree(); // Usage here means no unit test needed for getTree().
        amb = AppTreePanel.appMenuBar;

        // No significance to this value other than it needs to be a row that
        // we know for a fact will be there, even for a brand-new AppTreePanel.
        // In this case we've chosen a relatively low (safer) value, currently
        // should be 'Notes', with the first two being singles, and 'Views' collapsed.
        int theSelectionRow = 3;
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // These tests drive the app faster than it would go if it was only under user control.
        Thread.sleep(700); // Otherwise we see NullPointerExceptions after tests pass.
    }

    @AfterAll
    static void meLast() {
        theTree = null;
        amb = null;
        //appTreePanel.restoringPreviousSelection = false;
        appTreePanel = null;
    }

    // NOT a real test; this is just here to spit out the menu hierarchy so that
    // during 'real' test development, we will know what MenuItem should be retrieved
    // by the local getMenuItem function, so that it can be 'clicked'.
    //@Test
    public void showMenus() {
        int numMenus = amb.getMenuCount();
        MemoryBank.debug("Number of menus found: " + numMenus);
        for (int i = 0; i < numMenus; i++) {
            JMenu jm = amb.getMenu(i);
            if (jm == null) continue;
            System.out.println("Menu: " + jm.getText());

            for (int j = 0; j < jm.getItemCount(); j++) {
                JMenuItem jmi = jm.getItem(j);
                if (jmi == null) continue; // Separator
                System.out.println("    Menu Item text: " + jmi.getText());
            } // end for j
        } // end for i
    }

    // A utility function to retrieve a specified JMenuItem.
    // Calling contexts will perform a 'doClick' on the returned value.
    JMenuItem getMenuItem(String menu, String text) {
        JMenu jm;
        JMenuItem jmi = null;

        int numMenus = amb.getMenuCount();
        for (int i = 0; i < numMenus; i++) {
            jm = amb.getMenu(i);
            if (jm == null) continue;
            //System.out.println("Menu: " + jm.getText());
            if(jm.getText().equals(menu)) {
                for (int j = 0; j < jm.getItemCount(); j++) {
                    jmi = jm.getItem(j);
                    if (jmi == null) continue; // Separator
                    //System.out.println("    Menu Item text: " + jmi.getText());
                    if(jmi.getText().equals(text)) return jmi;
                } // end for j
            }
        } // end for i

        return jmi;
    }

    //=======================================================================================================
    // TESTS ARE BELOW
    //=======================================================================================================

    @Test
    void testCloseSearchResult() throws Exception {
        // First, select a known search result (we know the content of our test data)
        String theSearchResult = "20201107080423"; // Search text 'primatech'
        DefaultTreeModel theTreeModel = (DefaultTreeModel) theTree.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        DefaultMutableTreeNode dmtn = TestUtil.getTreeNodeForString(theRoot, theSearchResult);
        Assertions.assertNotNull(dmtn);
        TreePath tp = AppUtil.getTreePath(dmtn);
        Assertions.assertNotNull(tp);
        theTree.setSelectionPath(tp);
        Thread.sleep(200);
        assert Objects.requireNonNull(theTree.getSelectionPath()).getLastPathComponent().toString().equals(theSearchResult);

        // Now close it.
        appTreePanel.closeGroup();

        // And verify that the file for it is gone.
        String filename = MemoryBank.userDataHome + File.separatorChar + theSearchResult + ".sresults";
        File f = new File(filename);
        assertFalse(f.exists());

        // And verify that it is gone from the tree
        dmtn = TestUtil.getTreeNodeForString(theRoot, "20140312131216");
        assertNull(dmtn);

        // And that the tree selection switched up to 'Search Results'
        assert theTree.getSelectionPath().getLastPathComponent().toString().equals("Search Results");
    }


    // For this test we will search for notes modified After a selected date because this checks off
    // coverage for lines not tested elsewhere, as well as drilling down many of the private method
    // paths in this class, starting from the public menu 'Search' item.
    @Test
    void testDoSearch() throws InterruptedException {
        while(appTreePanel.spTheSearchPanel == null) {
            // Sleep, long enough for the SearchPanel to be constructed.
            // This is because its construction runs in a different thread and
            // we could otherwise end the test before we even configure the search.
            Thread.sleep(100);
        }

        // Bring up the SearchPanel by 'clicking' on the Search menu item.
        JMenuItem jmi = getMenuItem("App", "Search...");
        jmi.doClick(); // You could see multiple effects from this, if the other tests leave behind JMenuItem listeners.

        // Allow time for the search panel to be displayed.
        while(!appTreePanel.searching) {
            Thread.sleep(100);
        }

        // Select the Last Modified 'After' radio button (all other settings remain at their defaults).
        JRadioButton jRadioButton = appTreePanel.spTheSearchPanel.rbtnModAfter;
        jRadioButton.setSelected(true);

        // But programmatically setting the button to selected did not kick off the mouse listener, and
        // if it had, the date selection modal dialog would have come up and locked out any other processing,
        // so we can bring it up now but need  to carefully control the timing of what should happen:
        // 1.  Start a thread with a delay.  Meanwhile we run past that and:
        // 2.  Send a MouseEvent to the radio button listener, to bring up the modal date selection dialog.
        // 3.  After the delay, our thread picks back up and (expecting the dialog to be there) makes a
        //        MouseEvent selection on a (virtual) DayLabel whose handler will accept the 'input' that
        //        it got, close the dialog, and take us back to the basic SearchPanel that will be waiting
        //        for us to click the 'Search Now' button.
        // 4.  Programmatically click the 'Search Now' button.

        new Thread(new Runnable() {
            public void run() {
                try {
                    // Wait for the modal date selection dialog to appear.
                    Thread.sleep(1500);    // Step 1 - sleep until we get past step 2.

                    // Step 3, in the notes above (for the rest of this thread).
                    // On the YearView dialog window we want to select May 15th, 2019.
                    // Since it is in a previous year it will not show - that's ok, for the purposes of this test.
                    LocalDate ld = LocalDate.of(2019, 5, 15); // We know there is data here.

                    // Make a new 'virtual' DayLabel - it has the same handler as the 'real' ones.
                    YearView.DayLabel dayLabel = appTreePanel.spTheSearchPanel.yvDateChooser.new DayLabel();
                    // Did anyone notice how awesome was the above statment that instantiated that DayLabel?  Wow.
                    dayLabel.setText("15"); // The handler doesn't listen to DayLabels that have no text.
                    dayLabel.myDate = ld;  // Set this DayLabel's date to the one we want to use.

                    // Get the MouseListener for this DayLabel
                    MouseListener[] mouseListenerArray2 = dayLabel.getMouseListeners();

                    // Create a MousePressed event, and send it to the listener.
                    MouseEvent dayLabelPressed = new MouseEvent(dayLabel, MouseEvent.MOUSE_PRESSED,
                            1515, 0, 2, 2, 0, false);
                    mouseListenerArray2[0].mousePressed(dayLabelPressed);

                } catch (InterruptedException ignore) { }
            }
        }).start(); // Start the thread

        // Step 2, in the note above
        // Simulate a Mouse click on the 'Last Modified After' radio button
        MouseEvent modAfterClicked = new MouseEvent(jRadioButton, MouseEvent.MOUSE_CLICKED,
                158011902, 16, 10, 11, 0, false);
        MouseListener[] mouseListenerArray1 = jRadioButton.getMouseListeners();
        mouseListenerArray1[1].mouseClicked(modAfterClicked);  // The framework has a listener at index zero.
        // And now the modal date selection dialog should be up and awaiting an input, that we hope it
        // will get from our thread that was started above and has been patiently awaiting for this moment.

        // Give step 3 the time it needs to complete.
        while(appTreePanel.searching) {
            Thread.sleep(1500);
            // Ensure that the search will be run, regardless of the JOPtionPane.showOptionDialog return value (-1).
            appTreePanel.spTheSearchPanel.doSearch = true;

            // Step 4 in the note above (programmatically click on the 'Search Now' button of the SearchPanel) - will
            // not be done after all (because I don't know how).  Instead we will just close the dialog, and the flag
            // that we have set above will get us around the implied 'cancel'.  Of course extra code was added to
            // AppTreePanel to accomplish this, and that was wrong (but not wrong enough to make me want to change it).
            JOptionPane.getRootFrame().dispose();
        }

        // At current writing, the 'found' results is 139 and will probably climb higher,
        // over time so here we just check that some large number of records has been found.
        Assertions.assertTrue(appTreePanel.getTheNoteGroup().myNoteGroup.noteGroupDataVector.size() > 100);
    }

}