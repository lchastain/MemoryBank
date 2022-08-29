import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.Vector;

// This is a class to be used in testing the DndLayout.
// It differs from the production implementations in that the header
// line is defined here whereas the content below it is optional and
// is to be sent in by an invoking context, with the requirements:
// It has the same number of columns as we do here, and each row
// of content is in a panel that also has its own DndLayout (not shared
// with other panels), and each column is the same or greater width as
// the columns of the header we set here.  These requirements make this
// class far from reusable, but that's how it is and why, therefore, it
// is only defined in our Area51 and only used for T&E.
//
// See also the DndLayout usage by TodoNoteComponent or SearchResultComponent.

public class DragAndDropDriver extends JPanel implements ClingSource {
    JFrame testFrame;
    JPanel headerPanel;
    LabelButton headerButton1;
    LabelButton headerButton2;
    LabelButton headerButton3;
    LabelButton headerButton4;
    DndLayout dndLayout;
    Box theContainer;

    public DragAndDropDriver() {
        this(null);
    }

    public DragAndDropDriver(Box theContainer) {
        super(new BorderLayout());
        this.theContainer = theContainer;

        dndLayout = new DndLayout();
        dndLayout.setMoveable(true);
        headerPanel = new JPanel(dndLayout);
        headerButton1 = new LabelButton(" Column1 ");
        headerButton2 = new LabelButton(" Column2 ");
        headerButton3 = new LabelButton(" Column3 ");
        headerButton4 = new LabelButton(" Column4 ");

        headerPanel.add(headerButton1, "1");
        headerPanel.add(headerButton2, "2");
        headerPanel.add(headerButton3, "Stretch");
        headerPanel.add(headerButton4, "4");
        add(headerPanel, BorderLayout.NORTH);

        if (theContainer != null) {
            add(theContainer, BorderLayout.CENTER);
            dndLayout.setClingSource(this);

            // Get the first row of container content, so we can set widths.
            JPanel rowOne = (JPanel) theContainer.getComponent(0);
            int width1 = Integer.max(headerButton1.getPreferredSize().width, rowOne.getComponent(0).getPreferredSize().width);
            headerButton1.setPreferredSize(new Dimension(width1, headerButton1.getPreferredSize().height));
            int width2 = Integer.max(headerButton2.getPreferredSize().width, rowOne.getComponent(1).getPreferredSize().width);
            headerButton2.setPreferredSize(new Dimension(width2, headerButton2.getPreferredSize().height));
            int width3 = Integer.max(headerButton3.getPreferredSize().width, rowOne.getComponent(2).getPreferredSize().width);
            headerButton3.setPreferredSize(new Dimension(width3, headerButton3.getPreferredSize().height));
            int width4 = Integer.max(headerButton4.getPreferredSize().width, rowOne.getComponent(3).getPreferredSize().width);
            headerButton4.setPreferredSize(new Dimension(width4, headerButton4.getPreferredSize().height));
        }
    }

    static Box makeContent() {
        Box theContainer = new Box(BoxLayout.Y_AXIS);

        for(int i=1; i<=6; i++) {
            JPanel newLine = new JPanel(new DndLayout());
            newLine.add(new JButton("button 1"), "1");
            newLine.add(new JButton("button 2"), "2");
            newLine.add(new JButton("button 3"), "Stretch");
            newLine.add(new JButton("button 4"), "4");
            theContainer.add(newLine);
        }
        return theContainer;
    }


    public Vector<JComponent> getClingons(Component comp) {
        JComponent compTempComp;

        // Determine which column is being moved, by the position of the component in the headerPanel.
        int theColumn = 5;  // initialized to a bad value.
        for(int p = 0; p<headerPanel.getComponentCount(); p++) {
            Component c = headerPanel.getComponent(p);
            if(c == comp) {
                theColumn = p;
            }
        }

        int rows = theContainer.getComponentCount();
        Vector<JComponent> ClingOns = new Vector<>(rows, 1);

        JPanel tnc;

        for (int i = 0; i < rows; i++) {
            tnc = (JPanel) theContainer.getComponent(i); // get the 'i'th row of the content in the container.
            ((DndLayout) tnc.getLayout()).Dragging = true;

            compTempComp = (JComponent) tnc.getComponent(theColumn);

            ClingOns.addElement(compTempComp);
        } // end for i
        return ClingOns;
    } // end getClingons

    void testDragLeft() throws InterruptedException {
        long mouseWhen  = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        int mouseX = 10;
        int mouseY = 10;
        int mouseAbsY = 436;
        int mouseAbsXstart = 938;
        int mouseAbsXend = 880;

        MouseEvent hb3Pressed = new MouseEvent(headerButton3,
                MouseEvent.MOUSE_PRESSED, mouseWhen, 0, mouseX, mouseY,
                mouseAbsXstart,  mouseAbsY, 0, false, 1 );

        MouseEvent hb3Dragged;

        MouseEvent hb3Released = new MouseEvent(headerButton3,
                MouseEvent.MOUSE_RELEASED, mouseWhen, 0, mouseX, mouseY,
                mouseAbsXend,  mouseAbsY, 0, false, 1 );

        Thread.sleep(1000);

        dndLayout.mousePressed(hb3Pressed);

        for(int mouseDragX=mouseAbsXstart; mouseDragX>=mouseAbsXend; mouseDragX--) {
            hb3Dragged = new MouseEvent(headerButton3,
                    MouseEvent.MOUSE_DRAGGED, mouseWhen, 0, 9, mouseY,
                    mouseDragX,  mouseAbsY, 0, false, 1 );
            dndLayout.mouseDragged(hb3Dragged);
            Thread.sleep(30);
        }

        dndLayout.mouseReleased(hb3Released);

        // Use this to view the action.  Then close the window manually.
        while(testFrame.isVisible()) {
            //noinspection BusyWait
            Thread.sleep(1000);
        }
    } // end testDragLeft

    void testDragRight() throws InterruptedException {
        long mouseWhen  = LocalDateTime.now().toEpochSecond(OffsetDateTime.now().getOffset());
        int mouseX = 16;
        int mouseY = 10;
        int mouseAbsY = 436;
        int mouseAbsXstart = 735;
        int mouseAbsXend = 793;

        MouseEvent hb1Pressed = new MouseEvent(headerButton1,
                MouseEvent.MOUSE_PRESSED, mouseWhen, 0, mouseX, mouseY, 0, false);

        MouseEvent hb1Dragged;

        MouseEvent hb1Released = new MouseEvent(headerButton1,
                MouseEvent.MOUSE_RELEASED, mouseWhen, 0, mouseX, mouseY,
                831,  mouseAbsY, 0, false, 1 );

        Thread.sleep(1000);

        dndLayout.mousePressed(hb1Pressed);

        for(int mouseDragX=mouseAbsXstart; mouseDragX<=mouseAbsXend; mouseDragX++) {
            hb1Dragged = new MouseEvent(headerButton1,
                    MouseEvent.MOUSE_DRAGGED, mouseWhen, 0, 17, mouseY,
                    mouseDragX,  mouseAbsY, 0, false, 1 );

            dndLayout.mouseDragged(hb1Dragged);

            Thread.sleep(40);
        }

        dndLayout.mouseReleased(hb1Released);

        // Use this to view the action.  Then close the window manually.
        while(testFrame.isVisible()) {
            //noinspection BusyWait
            Thread.sleep(1000);
        }
    } // end testDragRight


    public static void main(String[] args) throws InterruptedException {
        Box theContainer = DragAndDropDriver.makeContent();
        DragAndDropDriver theDriver = new DragAndDropDriver(theContainer);
        MemoryBank.debug = true;
        MemoryBank.userEmail = "test.user@lcware.net";

        theDriver.testFrame = new JFrame("Drag And Drop Driver");

        theDriver.testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        theDriver.testFrame.getContentPane().add(theDriver);
        theDriver.testFrame.pack();
        theDriver.testFrame.setSize(new Dimension(500, 250));
        theDriver.testFrame.setVisible(true);
        theDriver.testFrame.setLocationRelativeTo(null);

//        theDriver.testDragLeft();
        theDriver.testDragRight();

    }

}
