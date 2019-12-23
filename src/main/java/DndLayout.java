/*
 A customized layout that implements a single row with zero or
 one 'stretchable' component.  The stretchable component will
 take the remainder of container width, after the other
 components are set to their preferred width.  Also supports
 horizontal Drag and Drop functionality, hence the 'Dnd' name.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.Vector;

// This layout presents components in a horizontal line, with one of the components
// being 'stretchable'.  The components may be rearranged via Drag & Drop.

// In production this layout is being used for both the header and the rows of certain
// NoteGroup types.  Only the header line listens for mouseDragged events, and the
// widths are kept in sync because both the header and the rows are in a JScrollPane
// where the header is set by the JScrollPane.setColumnHeaderView() method.  If this
// layout is applied to some other type of container, you may need to sync up the
// widths of the content columns with the header columns yourself.

public class DndLayout extends GridLayout implements MouseListener,
        MouseMotionListener {
    private static final long serialVersionUID = 1L;

    public boolean Dragging;

    private Container clingonContainer;
    private boolean Moveable;
    private int offset;          // used with dragging

    private Component prevComponent;
    private Component nextComponent;
    private Component stretch;
    private Vector<JComponent> ClingLeft;
    private Vector<JComponent> ClingOns;
    private Vector<JComponent> ClingRight;
    private ClingSource clingSource;

    public DndLayout() {
        super(1, 0, 0, 0);
        Moveable = false;
        Dragging = false;
        stretch = null;
        clingSource = null;
    } // end constructor

    public void addLayoutComponent(String name, Component comp) {
        if (Moveable) {
            comp.addMouseListener(this);
            comp.addMouseMotionListener(this);
        } // end if
        if (name.equals("Stretch")) stretch = comp;
    }//addLayoutComponent


    public void layoutContainer(Container parent) {
        if (Dragging) return;

        this.clingonContainer = parent;
        synchronized (parent.getTreeLock()) {
            int ncols = parent.getComponentCount();
            if (ncols == 0) return;

            int otherWidths = 0;
            int stretchWidth;
            Component comp;
            Insets insets = parent.getInsets();
            int c;
            int w;
            int h = parent.getSize().height - (insets.top + insets.bottom);
            Dimension d;

            int x = insets.left;
            int y = insets.top;

            // First loop is needed only to see what size all the other columns
            //   will need, in order to calculate the 'stretch' column width.
            for (c = 0; c < ncols; c++) {
                comp = parent.getComponent(c);
                if (!comp.isVisible()) continue;
                if (comp == stretch) continue;

                d = comp.getPreferredSize();
                w = d.width;
                otherWidths += w;
            } // end for c (col)
            w = parent.getWidth();
            stretchWidth = w - otherWidths - (insets.left + insets.right);

            // Now size and place the components
            for (c = 0; c < ncols; c++) {
                comp = parent.getComponent(c);
                if (!comp.isVisible()) continue;
                if (comp == stretch) w = stretchWidth;
                else w = comp.getPreferredSize().width;
                comp.setBounds(x, y, w, h);
                x = x + w;
            } // end for c (col)

        } // end synchronized section
    } // end layoutContainer

    private void placeClingons(int col, int x) {
        int y;
        Vector<JComponent> v = null;
        Component c;
        if (col == -1) v = ClingLeft;
        if (col == 0) v = ClingOns;
        if (col == 1) v = ClingRight;

        assert v != null;
        for (int i = 0; i < v.size(); i++) {
            c = v.elementAt(i);
            if (col == 0) c.getParent().add(c, 0); // Z order
            y = c.getLocation().y;
            c.setLocation(x, y);
        } // end for i
    } // end placeClingons


    private void resetActualPosition() {
        boolean swapped = true;
        Component c1, c2;
        int c1x, c2x;
        int c1width, c2width;
        if(clingonContainer == null) return;
        int count = clingonContainer.getComponentCount();

        while (swapped) {
            swapped = false;
            for (int i = 0; i < (count - 1); i++) {
                c1 = clingonContainer.getComponent(i);
                c2 = clingonContainer.getComponent(i + 1);

                c1x = c1.getX();
                c2x = c2.getX();
                c1width = c1.getWidth();
                // System.out.println("c1 Width: " + c1.getWidth() + "\tPreferred: " + c1.getPreferredSize().width);
                // System.out.println("c2 Width: " + c2.getWidth() + "\tPreferred: " + c2.getPreferredSize().width);

                // Note:  When the 'Stretch' component is not visible, it can
                //   report a negative width, which will cause the 'if' structure
                //   below to handle it improperly, resulting in swaps
                //   when not wanted, or vice-versa.  Before paging this was not a
                //   visible problem; now when you perform a sort or column reorder
                //   on a 'short' page, it becomes apparent when going back to a
                //   'full' page.  The solution for now (3/11/2008) is to fix it at
                //   the paging event (pageNumberChanged) of the NoteGroup by making
                //   a call to checkColumnOrder for those groups that have moveable
                //   columns.

                c2width = c2.getWidth();
                if (c1x + c1width / 2 > c2x + c2width / 2) {
                    swapped = true;
                    clingonContainer.add(c2, i);
                } // end if
            } // end for i
        } // end while

        if (clingSource == null) return;

        // The remainder of this method deals with
        //   handling a drag operation.
        DndLayout clingLayout;

        // System.out.println("DndLayout.resetActualPosition Clingons.size = " + ClingOns.size());

        for (int i = 0; i < ClingOns.size(); i++) {
            Component c = ClingOns.elementAt(i);
            clingLayout = (DndLayout) (c.getParent().getLayout());
            clingLayout.resetActualPosition();
            clingLayout.Dragging = false;
            c.getParent().doLayout();
        } // end for i
    } // end resetActualPosition

    // Called after a draggable component has crossed a 'swap'
    //   threshhold, in order to respecify adjacent components.
    private void resetRelativePosition(Component comp) {
        prevComponent = null;
        nextComponent = null;

        Component c;
        int thisX, thisWidth;
        int cX, cWidth;
        if(clingonContainer == null) return;
        int count = clingonContainer.getComponentCount();

        thisX = comp.getX();
        thisWidth = comp.getSize().width;

        for (int i = 0; i < count; i++) {
            c = clingonContainer.getComponent(i);
            if (c == comp) continue;
            if (!c.isVisible()) continue;
            cX = c.getX();
            cWidth = c.getWidth();

            // tryouts for nextComponent -
            if ((cX + cWidth / 2) > (thisX + thisWidth / 2)) {
                if (nextComponent == null) nextComponent = c;
                else {
                    if (cX < nextComponent.getX()) nextComponent = c;
                } // end else
            } // end if

            // tryouts for prevComponent -
            if ((cX + cWidth / 2) < (thisX + thisWidth / 2)) {
                if (prevComponent == null) prevComponent = c;
                else {
                    if (cX > prevComponent.getX()) prevComponent = c;
                } // end else
            } // end if
        } // end for i

        if (clingSource == null) return;
        ClingLeft = null;
        ClingRight = null;
        if (prevComponent != null) ClingLeft = clingSource.getClingons(prevComponent);
        if (nextComponent != null) ClingRight = clingSource.getClingons(nextComponent);
    } // end resetRelativePosition

    public void setClingSource(ClingSource cs) {
        this.clingSource = cs;
    } // end setClingSource

    private void setRelativePosition(Component comp) {
        if (clingSource == null) return;

        // Set the next and previous components
        prevComponent = null;
        nextComponent = null;

        Component tmpComp;
        int numc = clingonContainer.getComponentCount();
        for (int i = 0; i < numc; i++) {
            tmpComp = clingonContainer.getComponent(i);
            if (comp == tmpComp) {
                if (i < numc - 1) nextComponent = clingonContainer.getComponent(i + 1);
                if (i > 0) prevComponent = clingonContainer.getComponent(i - 1);
                break;
            } // end if
        } // end for i

        ClingLeft = null;
        ClingRight = null;
        ClingOns = clingSource.getClingons(comp);
        if (prevComponent != null) ClingLeft = clingSource.getClingons(prevComponent);
        if (nextComponent != null) ClingRight = clingSource.getClingons(nextComponent);
    } // end setRelativePosition


    public void setMoveable(boolean b) {
        Moveable = b;
    }

    //---------------------------------------------------------
    // MouseListener methods
    //---------------------------------------------------------
    public void mouseClicked(MouseEvent e) {
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    public void mousePressed(MouseEvent e) {
        //System.out.println("Mouse Event: " + e.toString());
        // First, get the offset.  This is the exact location of the
        //   mouse cursor within the component being moved.  When we
        //   want to place the component in its new location, the X
        //   coordinate of that location should NOT include this amount.
        offset = e.getX();

        // Now set the 'Dragging' boolean.  Of course at this mousePressed
        //   event we are not actually dragging; that is handled in the
        //   'mouseDragged' method, but this boolean is used to 'bracket'
        //   the dragging action, because every drag operation starts with
        //   a mousePressed (where we set it to true) and ends with a
        //   mouseReleased (where we set it to false).  The main usage
        //   is to tell the layoutContainer method to hold off, until we are
        //   no longer dragging.  Basically, the mouseDragged method handles
        //   the layout during dragging.
        Dragging = true;

        Component comp = (Component) e.getSource();
        setRelativePosition(comp); // initialze prev & next
        clingonContainer.add(comp, 0);  // rearrange the Z order.
    } // end mousePressed

    public void mouseReleased(MouseEvent e) {
        //System.out.println("Mouse Event: " + e.toString());
        Dragging = false;
        resetActualPosition();
        clingonContainer.doLayout();
    }
    //---------------------------------------------------------

    //-------------------------------------------------
    // MouseMotionListener methods
    //-------------------------------------------------
    public synchronized void mouseDragged(MouseEvent me) {
        Component comp = (Component) me.getSource();
        Point p = comp.getLocation();
        //System.out.println("Mouse Event: " + me.toString() + "  " + p.toString());
        int newX = p.x + me.getX() - offset;
        int myWidth = comp.getWidth();

        // This is needed for both the 'main' line (a header, probably) and any potential clingons.
        resetRelativePosition(comp);

        if (newX > p.x) { // moving to the right
            if (nextComponent != null) { // if there is another column
                int nextColWidth = nextComponent.getWidth();
                int nextColX = nextComponent.getX();

                // Check to see if we've moved past the 'swap' threshhold.
                if (newX >= (nextColX + nextColWidth / 2 - myWidth)) {
                    nextComponent.setLocation(nextColX - myWidth, p.y);
                    if (ClingRight != null) placeClingons(1, nextColX - myWidth);
                } // end if
            } // end if
        } // end if

        if (newX < p.x) { // moving to the left
            if (prevComponent != null) { // if there is another column
                int prevColWidth = prevComponent.getWidth();
                int prevColX = prevComponent.getX();

                // Check to see if we've moved past the 'swap' threshhold.
                if ((newX <= (prevColX + prevColWidth / 2) &&
                        (newX + myWidth > (prevColX + prevColWidth / 2)))) {
                    prevComponent.setLocation(prevColX + myWidth, p.y);
                    if (ClingLeft != null) placeClingons(-1, prevColX + myWidth);
                } // end if
            } // end if
        } // end if
        comp.setLocation(newX, p.y);
        if (ClingOns != null) placeClingons(0, newX);
    } // end mouseDragged

    public void mouseMoved(MouseEvent me) {
    }
} // end class DndLayout





