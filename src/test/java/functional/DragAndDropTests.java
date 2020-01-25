import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterAll;
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

// This class will test portions of three production classes - the DndLayout and the two group
//   headers that utilize it.  Initially there was a 'DragAndDropDriver' class for the DndLayout
//   class alone but it provided its own implementation of the ClingSource interface rather than
//   testing the production code, so now those tests and the driver are replaced by the tests
//   here that utilize the actual application code for SearchResultHeader and TodoGroupHeader,
//   and no additional 'driver' is needed; that class has been remodeled into an area51 'main'
//   for the layout just to keep the implementation example, but is no longer used in automated
//   testing.  In order to fully test the layout features we need to do both a drag left and a
//   drag right but each of the header classes only needs one or the other, to get their required
//   code coverage.  A coin was flipped, and the result is that TodoGroupHeader will be used for
//   the drag left, and SearchResultHeader will be tested with a drag right operation.

public class DragAndDropTests {

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

    @AfterAll
    static void meLast() {
    }

    // These tests and the app work but the incrementation is not obvious; it comes from re-placing
    // the component with every handling of a MOUSE_DRAGGED event, so that if you debug, it will look
    // like the 'newX' has the same value.  Somewhere in the Dnd framework it uses that, plus the
    // absolute values, so that it works right, but you may not be able to see exactly why or how or
    // when, while debugging the DndLayout event handlers.  Hopefully no need to do that anyway.

    @Test
    void testDragLeft() throws InterruptedException {
        TodoNoteGroup todoNoteGroup = new TodoNoteGroup("Get New Job");
        TodoGroupHeader todoGroupHeader = todoNoteGroup.listHeader;
        JFrame testFrame = new JFrame("Drag Left And Drop Driver");
        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                testFrame.setVisible(false);
            }
        });

        testFrame.getContentPane().add(todoNoteGroup);
        testFrame.pack();
        testFrame.setSize(new Dimension(620, 540));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);

        long mouseWhen  = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        int mouseX = 10;
        int mouseY = 10;
        int mouseAbsY = 436;
        int mouseAbsXstart = 938;
        int mouseAbsXend = 740;  // Not all the way, but past the halfway point.

        MouseEvent hb3Pressed = new MouseEvent(todoGroupHeader.hb3,
                MouseEvent.MOUSE_PRESSED, mouseWhen, 0, mouseX, mouseY,
                mouseAbsXstart,  mouseAbsY, 0, false, 1 );

        MouseEvent hb3Dragged;

        MouseEvent hb3Released = new MouseEvent(todoGroupHeader.hb3,
                MouseEvent.MOUSE_RELEASED, mouseWhen, 0, mouseX, mouseY,
                mouseAbsXend,  mouseAbsY, 0, false, 1 );

        Thread.sleep(500);

        todoGroupHeader.headerLayout.mousePressed(hb3Pressed);

        for(int mouseDragX=mouseAbsXstart; mouseDragX>=mouseAbsXend; mouseDragX--) {
            hb3Dragged = new MouseEvent(todoGroupHeader.hb3,
                    MouseEvent.MOUSE_DRAGGED, mouseWhen, 0, 9, mouseY,
                    mouseDragX,  mouseAbsY, 0, false, 1 );
            todoGroupHeader.headerLayout.mouseDragged(hb3Dragged);
            Thread.sleep(20);
        }

        todoGroupHeader.headerLayout.mouseReleased(hb3Released);

        // Use this to view the action.  Then close the window manually.
//        while(testFrame.isVisible()) {
//            Thread.sleep(1000);
//        }

    } // end testDragLeft

    @Test
    void testDragRight() throws InterruptedException {
        SearchResultGroup searchResultGroup = new SearchResultGroup("20191029073938");
        SearchResultHeader searchResultHeader = searchResultGroup.listHeader;
        JFrame testFrame = new JFrame("Drag Right And Drop Driver");
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
        int mouseAbsY = 436;
        int mouseAbsXstart = 735;
        int mouseAbsXend = 940;  // Not all the way, but past the halfway point.

        MouseEvent hb1Pressed = new MouseEvent(searchResultHeader.hb1,
                MouseEvent.MOUSE_PRESSED, mouseWhen, 0, mouseX, mouseY, 0, false);

        MouseEvent hb1Dragged;

        MouseEvent hb1Released = new MouseEvent(searchResultHeader.hb1,
                MouseEvent.MOUSE_RELEASED, mouseWhen, 0, mouseX, mouseY,
                831,  mouseAbsY, 0, false, 1 );

        Thread.sleep(500);

        searchResultHeader.headerLayout.mousePressed(hb1Pressed);

        for(int mouseDragX=mouseAbsXstart; mouseDragX<=mouseAbsXend; mouseDragX++) {
            hb1Dragged = new MouseEvent(searchResultHeader.hb1,
                    MouseEvent.MOUSE_DRAGGED, mouseWhen, 0, 17, mouseY,
                    mouseDragX,  mouseAbsY, 0, false, 1 );

            searchResultHeader.headerLayout.mouseDragged(hb1Dragged);

            Thread.sleep(20);
        }

        searchResultHeader.headerLayout.mouseReleased(hb1Released);

        // Use this to view the action.  Then close the window manually.
//        while(testFrame.isVisible()) {
//            Thread.sleep(1000);
//        }
    } // end testDragRight

}
