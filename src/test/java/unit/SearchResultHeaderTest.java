import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

class SearchResultHeaderTest {

    @BeforeAll
    static void meFirst() throws IOException {
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

        // Retrieve a fresh set of test data from test resources
        String fileName = "jondo.nonamus@lcware.net";
        File testResource = FileUtils.toFile(AppTreePanel.class.getResource(fileName));
        FileUtils.copyDirectory(testResource, testDataLoc);

        // Load up this Test user's application options
        MemoryBank.loadOpts();
    }

    // This test is needed in order to get coverage for an anonymous class that is
    // created to handle sorting when a HeaderButton is clicked once.  If shifted,
    // the sort would be done in reverse order.  While here we also cover the less
    // complex other mouse listener methods.
    @Test
    void testHeaderButtonMouseEvents() throws InterruptedException {
        SearchResultGroup searchResultGroup = new SearchResultGroup("20191029073938");
        SearchResultHeader searchResultHeader = searchResultGroup.listHeader;
        JFrame testFrame = new JFrame("Sort by Search Text Driver");
        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                testFrame.setVisible(false);
            }
        });

        testFrame.getContentPane().add(searchResultGroup);
        testFrame.pack();
        testFrame.setSize(new Dimension(620, 540));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);

        long mouseWhen  = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        int mouseX = 16;
        int mouseY = 10;

        // Time to view the list prior to reacting to mouse events.
        Thread.sleep(500);

        MouseEvent hb2Entered = new MouseEvent(searchResultHeader.hb2,
                MouseEvent.MOUSE_ENTERED, mouseWhen, 0, mouseX, mouseY, 0, false);
        searchResultHeader.hb2.mouseEntered(hb2Entered);

        MouseEvent hb2Pressed = new MouseEvent(searchResultHeader.hb2,
                MouseEvent.MOUSE_PRESSED, mouseWhen, 0, mouseX, mouseY, 0, false);
        searchResultHeader.hb2.mousePressed(hb2Pressed);

        MouseEvent hb2Released = new MouseEvent(searchResultHeader.hb2,
                MouseEvent.MOUSE_RELEASED, mouseWhen, 0, mouseX, mouseY, 0, false);
        searchResultHeader.hb2.mouseReleased(hb2Released);

        // This is the 'main' part of the test
        MouseEvent hb2Clicked = new MouseEvent(searchResultHeader.hb2,
                MouseEvent.MOUSE_CLICKED, mouseWhen, 0, mouseX, mouseY, 0, false);
        searchResultHeader.hb2.mouseClicked(hb2Clicked);

        MouseEvent hb2Exited = new MouseEvent(searchResultHeader.hb2,
                MouseEvent.MOUSE_EXITED, mouseWhen, 0, mouseX, mouseY, 0, false);
        searchResultHeader.hb2.mouseExited(hb2Exited);

        testFrame.setVisible(false);  // needed when ALL tests run; window stays up too long.

        // Use this to view the action.  Then close the window manually.
//        while(testFrame.isVisible()) {
//            Thread.sleep(1000);
//        }
    }
}