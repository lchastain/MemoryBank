/**
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


public class DndLayout extends GridLayout implements MouseListener,
        MouseMotionListener {
    private static final long serialVersionUID = 1L;

    public boolean Dragging;

    private Container parent;
    private boolean Moveable;
    private int offset;          // used with dragging

    Component prevComponent;
    Component nextComponent;
    Component stretch;
    Vector<JComponent> ClingLeft;
    Vector<JComponent> ClingOns;
    Vector<JComponent> ClingRight;
    ClingSource cs;

    public DndLayout() {
        super(1, 0, 0, 0);
        Moveable = false;
        Dragging = false;
        stretch = null;
        cs = null;
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

        this.parent = parent;
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


    public void resetActualPosition() {
        boolean swapped = true;
        Component c1, c2;
        int c1x, c2x;
        int c1width, c2width;
        int count = parent.getComponentCount();

        while (swapped) {
            swapped = false;
            for (int i = 0; i < (count - 1); i++) {
                c1 = parent.getComponent(i);
                c2 = parent.getComponent(i + 1);

                c1x = c1.getX();
                c2x = c2.getX();
                c1width = c1.getWidth();
                // System.out.println("c1 Width: " + c1.getWidth() + "\tPreferred: " + c1.getPreferredSize().width);
                // System.out.println("c2 Width: " + c2.getWidth() + "\tPreferred: " + c2.getPreferredSize().width);

                // Note:  When the 'Stretch' component is not visible, it can
                //   report a negative width.  this will cause the 'if' structure
                //   below to work the opposite of visible rows, resulting in swaps
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
                    parent.add(c2, i);
                } // end if
            } // end for i
        } // end while

        if (cs == null) return;

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
    public void resetRelativePosition(Component comp) {
        prevComponent = null;
        nextComponent = null;

        Component c;
        int thisX, thisWidth;
        int cX, cWidth;
        int count = parent.getComponentCount();

        thisX = comp.getX();
        thisWidth = comp.getSize().width;

        for (int i = 0; i < count; i++) {
            c = parent.getComponent(i);
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

        if (cs == null) return;
        ClingLeft = null;
        ClingRight = null;
        if (prevComponent != null) ClingLeft = cs.getClingons(prevComponent);
        if (nextComponent != null) ClingRight = cs.getClingons(nextComponent);
    } // end resetRelativePosition

    public void setClingSource(ClingSource cs) {
        this.cs = cs;
    } // end setClingSource

    private void setRelativePosition(Component comp) {
        if (cs == null) return;

        // Set the next and previous components
        prevComponent = null;
        nextComponent = null;

        Component tmpComp;
        int numc = parent.getComponentCount();
        for (int i = 0; i < numc; i++) {
            tmpComp = parent.getComponent(i);
            if (comp == tmpComp) {
                if (i < numc - 1) nextComponent = parent.getComponent(i + 1);
                if (i > 0) prevComponent = parent.getComponent(i - 1);
                break;
            } // end if
        } // end for i

        ClingLeft = null;
        ClingRight = null;
        ClingOns = cs.getClingons(comp);
        if (prevComponent != null) ClingLeft = cs.getClingons(prevComponent);
        if (nextComponent != null) ClingRight = cs.getClingons(nextComponent);
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
        parent.add(comp, 0);  // rearrange the Z order.
    } // end mousePressed

    public void mouseReleased(MouseEvent e) {
        Dragging = false;
        resetActualPosition();
        parent.doLayout();
    }
    //---------------------------------------------------------

    //-------------------------------------------------
    // MouseMotionListener methods
    //-------------------------------------------------
    public synchronized void mouseDragged(MouseEvent me) {
        Component comp = (Component) me.getSource();
        Point p = comp.getLocation();
        int newX = p.x + me.getX() - offset;
        int myWidth = comp.getWidth();

        if (newX > p.x) { // moving to the right
            if (nextComponent != null) { // if there is another column
                int nextColWidth = nextComponent.getWidth();
                int nextColX = nextComponent.getX();

                // Check to see if we've moved past the 'swap' threshhold.
                if (newX >= (nextColX + nextColWidth / 2 - myWidth)) {
                    nextComponent.setLocation(nextColX - myWidth, p.y);
                    if (ClingRight != null) placeClingons(1, nextColX - myWidth);
                    resetRelativePosition(comp);
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
                    resetRelativePosition(comp);
                } // end if
            } // end if
        } // end if
        comp.setLocation(newX, p.y);
        if (ClingOns != null) placeClingons(0, newX);
    } // end mouseDragged

    public void mouseMoved(MouseEvent me) {
    }
} // end class DndLayout





