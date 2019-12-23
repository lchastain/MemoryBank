import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;

class DndLayoutTest {
    private DragAndDropDriver dragAndDropDriver;
    private DndLayout dndLayout;
    Box theContainer;

    // Keeping this class in the unit tests because this is where it started, but now that
    // there are tests for the actual dragging and dropping, those two tests could move out
    // of this class and into the functional tests.  Not doing it right now cuz I don't see
    // a compelling need, and there are higher priorities than organizational housekeeping.
    // (what, such as making a long-winded comment instead of just doing it, that you could
    // had done in the same amount of time?).
    // Ansr:  shut up.

    @BeforeEach
    void setUp() {
        dndLayout = new DndLayout();
        dndLayout.setMoveable(true);
    }

    @AfterEach
    void tearDown() {
    }

    void makeDriver() {
        theContainer = new Box(BoxLayout.Y_AXIS);

        for(int i=1; i<=6; i++) {
            JPanel newLine = new JPanel(new DndLayout());
            newLine.add(new JButton("button 1"), "1");
            newLine.add(new JButton("button 2"), "2");
            newLine.add(new JButton("button 3"), "Stretch");
            newLine.add(new JButton("button 4"), "4");
            theContainer.add(newLine);
        }
        dragAndDropDriver = new DragAndDropDriver(theContainer);
    }

    // These tests (and the app) work (for now?) but the incrementation is not obvious; it comes from re-placing
    // the component with every handling of a MOUSE_DRAGGED event, so that if you debug, it will look
    // like the 'newX' is the same, or one pixel off each time.  Somewhere in the system it uses that
    // plus the absolute values, so that it works right, but you may not be able to see exactly why
    // or how or when, while debugging the DndLayout event handlers.  Hopefully no further attention needed here.

    @Test
    void testDragLeft() throws InterruptedException {
        makeDriver();
        JFrame testFrame = new JFrame("Drag Left And Drop Driver");
        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                testFrame.setVisible(false);
            }
        });

        testFrame.getContentPane().add(dragAndDropDriver);
        testFrame.pack();
        testFrame.setSize(new Dimension(500, 250));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);

        long mouseWhen  = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        int mouseX = 10;
        int mouseY = 10;
        int mouseAbsY = 436;
        int mouseAbsXstart = 938;
        int mouseAbsXend = 870;

        MouseEvent hb3Pressed = new MouseEvent(dragAndDropDriver.headerButton3,
                MouseEvent.MOUSE_PRESSED, mouseWhen, 0, mouseX, mouseY,
                mouseAbsXstart,  mouseAbsY, 0, false, 1 );

        MouseEvent hb3Dragged;

        MouseEvent hb3Released = new MouseEvent(dragAndDropDriver.headerButton3,
                MouseEvent.MOUSE_RELEASED, mouseWhen, 0, mouseX, mouseY,
                mouseAbsXend,  mouseAbsY, 0, false, 1 );

        Thread.sleep(1000);

        dragAndDropDriver.dndLayout.mousePressed(hb3Pressed);

        for(int mouseDragX=mouseAbsXstart; mouseDragX>=mouseAbsXend; mouseDragX--) {
            hb3Dragged = new MouseEvent(dragAndDropDriver.headerButton3,
                    MouseEvent.MOUSE_DRAGGED, mouseWhen, 0, 9, mouseY,
                    mouseDragX,  mouseAbsY, 0, false, 1 );
            dragAndDropDriver.dndLayout.mouseDragged(hb3Dragged);
            Thread.sleep(30);
        }

        dragAndDropDriver.dndLayout.mouseReleased(hb3Released);

        // Use this to view the action.  Then close the window manually.
//        while(testFrame.isVisible()) {
//            Thread.sleep(1000);
//        }

    } // end testDragLeft

    @Test
    void testDragRight() throws InterruptedException {
        makeDriver();
        JFrame testFrame = new JFrame("Drag Right And Drop Driver");
        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                testFrame.setVisible(false);
            }
        });

        testFrame.getContentPane().add(dragAndDropDriver);
        testFrame.pack();
        testFrame.setSize(new Dimension(500, 250));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);

        long mouseWhen  = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        int mouseX = 16;
        int mouseY = 10;
        int mouseAbsY = 436;
        int mouseAbsXstart = 735;
        int mouseAbsXend = 793;

        MouseEvent hb1Pressed = new MouseEvent(dragAndDropDriver.headerButton1,
                MouseEvent.MOUSE_PRESSED, mouseWhen, 0, mouseX, mouseY, 0, false);

        MouseEvent hb1Dragged;

        MouseEvent hb1Released = new MouseEvent(dragAndDropDriver.headerButton1,
                MouseEvent.MOUSE_RELEASED, mouseWhen, 0, mouseX, mouseY,
                831,  mouseAbsY, 0, false, 1 );

        Thread.sleep(1000);

        dragAndDropDriver.dndLayout.mousePressed(hb1Pressed);

        for(int mouseDragX=mouseAbsXstart; mouseDragX<=mouseAbsXend; mouseDragX++) {
            hb1Dragged = new MouseEvent(dragAndDropDriver.headerButton1,
                    MouseEvent.MOUSE_DRAGGED, mouseWhen, 0, 17, mouseY,
                    mouseDragX,  mouseAbsY, 0, false, 1 );

            dragAndDropDriver.dndLayout.mouseDragged(hb1Dragged);

            Thread.sleep(40);
        }

        dragAndDropDriver.dndLayout.mouseReleased(hb1Released);

        // Use this to view the action.  Then close the window manually.
//        while(testFrame.isVisible()) {
//            Thread.sleep(1000);
//        }
    } // end testDragRight


    @Test
    void testMouseClicked() {
        dndLayout.mouseClicked(null);
    }

    @Test
    void testMouseEntered() {
        dndLayout.mouseEntered(null);
    }

    @Test
    void testMouseExited() {
        dndLayout.mouseExited(null);
    }

    @Test
    void testMouseMoved() {
        dndLayout.mouseMoved(null);
    }

    // Normally disabled, use this 'test' after enabling mouse event printouts,
    // to get absolute x/y values for use in test dev when constructing mouse events.
//    @Test
    void testTheDriver() throws InterruptedException {
        makeDriver();
        JFrame testFrame = new JFrame("Drag And Drop Driver");
        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                testFrame.setVisible(false);
            }
        });

        testFrame.getContentPane().add(dragAndDropDriver);
        testFrame.pack();
        testFrame.setSize(new Dimension(500, 250));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);

        while(testFrame.isVisible()) {
            Thread.sleep(1000);
        }
    } // end testTheDriver




}