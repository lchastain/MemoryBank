import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Vector;

// This is a class to be used in testing the DndLayout.
// It differs from the production implementations in that the header
// line is defined here whereas the content below is optional and
// is to be sent in by an invoking context, with the requirements:
// It has the same number of columns as we do here, and each row
// of content is in a panel that also has its own DndLayout (not shared
// with other panels), and each column is the same or greater width as
// the columns of the header we set here.  These requirements make this
// class far from reusable, but that's how it is and why, therefore, it
// is only defined in our Area51 and only used by the DndLayoutTest.
//
// See also the DndLayout usage by TodoNoteComponent or SearchResultComponent.

public class DragAndDropDriver extends JPanel implements ClingSource {
    JPanel headerPanel;
    LabelButton headerButton1;
    LabelButton headerButton2;
    LabelButton headerButton3;
    LabelButton headerButton4;
    DndLayout dndLayout;
    Container theContainer;

    public DragAndDropDriver() {
        this(null);
    }

    public DragAndDropDriver(Container theContainer) {
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


    public static void main(String[] args) {
        DragAndDropDriver theDriver = new DragAndDropDriver();
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("test.user@lcware.net");

        JFrame testFrame = new JFrame("Drag And Drop Driver");

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        testFrame.getContentPane().add(theDriver);
        testFrame.pack();
        testFrame.setSize(new Dimension(350, 250));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
