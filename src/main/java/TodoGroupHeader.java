/*
 This class is a collection of labels in a horizontal line to be
 used as the header for a TodoNoteGroup.  The labels are
 formatted and mouse-activated to appear like buttons.  The columns
 are numbered but the default initial numbering does not constrain
 the subsequent ordering of the columns.
 */

import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Vector;

public class TodoGroupHeader extends Container implements ClingSource {
    private static final long serialVersionUID = 1L;

    private final TodoNoteGroupPanel parent;
    HeaderButton hb1;  // Priority
    HeaderButton hb2;  // To Do Text
    HeaderButton hb3;  // Status
    HeaderButton hb4;  // Deadline   // No longer used.
    DndLayout headerLayout;

    TodoGroupHeader(TodoNoteGroupPanel p) {
        super();
        parent = p;
        headerLayout = new DndLayout();
        headerLayout.setMoveable(true); // Must be BEFORE add.
        headerLayout.setClingSource(this);
        setLayout(headerLayout);

        // Default Labels -
        hb1 = new HeaderButton("Priority");
        hb2 = new HeaderButton("To Do Text");
        hb3 = new HeaderButton("Status");
        hb4 = new HeaderButton("Deadline");

        // Whatever was stored...
        hb1.setText(((TodoGroupProperties) parent.myProperties).column1Label);
        hb2.setText(((TodoGroupProperties) parent.myProperties).column2Label);
        hb3.setText(((TodoGroupProperties) parent.myProperties).column3Label);
        hb4.setText(((TodoGroupProperties) parent.myProperties).column4Label);

        add(hb1, "First");
        add(hb2, "Stretch");
        add(hb3, "Third");
        // add(hb4, "Fourth");

        // Re-order the columns, if necessary
        String pos = String.valueOf(((TodoGroupProperties) parent.myProperties).columnOrder);
        // System.out.println("In TodoGroupHeader, columnOrder = " + pos);
        add(hb1, pos.indexOf("1"));
        add(hb2, pos.indexOf("2"));
        add(hb3, pos.indexOf("3"));
        // add(hb4, pos.indexOf("4"));

        hb1.setVisible(((TodoGroupProperties) parent.myProperties).showPriority);
    } // end constructor


    //----------------------------------------------------------------
    // Method Name: doLayout
    //
    // Overrode this Container method in order to capture the column
    //   order change as a 'group changed' event.
    //----------------------------------------------------------------
    @Override
    public void doLayout() {
        super.doLayout();

        if (headerLayout.Dragging) return;

        int origColumnOrder = ((TodoGroupProperties) parent.myProperties).columnOrder;
        if (getColumnOrder() != origColumnOrder) {
            ((TodoGroupProperties) parent.myProperties).columnOrder = getColumnOrder();
//            parent.setGroupChanged(true);
            parent.groupChanged = true;
            // System.out.println("\n\nSet Group changed flag!");
        }
    } // end doLayout


    //----------------------------------------------------------------
    // Method Name: getClingons
    //
    // Called by the DndLayout prior to possibly handling a drag
    //   operation.  When the
    //   layout responds to a drag operation on a column header,
    //   it needs to have the list (Vector) of components that go in
    //   that column.  This header class accesses its container to
    //   provide the needed info.  'Clingons' because the items in
    //   the column 'cling' to their header during a drag operation.
    //----------------------------------------------------------------
    public Vector<JComponent> getClingons(Component comp) {
        JComponent compTempComp;

        Container cTheContainer = parent.groupNotesListPanel;
        int rows = cTheContainer.getComponentCount();
        Vector<JComponent> ClingOns = new Vector<>(rows, 1);

        HeaderButton hb = (HeaderButton) comp;

        TodoNoteComponent tnc;

        for (int i = 0; i < rows; i++) {
            tnc = (TodoNoteComponent) cTheContainer.getComponent(i);
            ((DndLayout) tnc.getLayout()).Dragging = true;

            // System.out.println("TodoGroupHeader: hb.defaultLabel = " + hb.defaultLabel);

            // We still need to change the order, based on actual order.
            switch (hb.defaultLabel) {
                case "Priority":
                    compTempComp = tnc.getPriorityButton();
                    break;
                case "To Do Text":
                    compTempComp = tnc.getNoteTextField();
                    break;
                case "Status":
                    compTempComp = tnc.getStatusButton();
                    break;
                default:
                    // Now that there are only 3, this will throw an exception
                    //   if it ever gets here.  Left it in to show me the problem
                    //   in case it ever happens, and also so that the compiler
                    //   believes that compTempComp will always have a value.
                    compTempComp = (JComponent) tnc.getComponent(3);
                    break;
            }
            ClingOns.addElement(compTempComp);
        } // end for i
        return ClingOns;
    } // end getClingons


    public String getColumnHeader(int i) {
        String s = null;
        switch (i) {
            case 1:
                s = hb1.getText();
                break;
            case 2:
                s = hb2.getText();
                break;
            case 3:
                s = hb3.getText();
                break;
            case 4:
                s = hb4.getText();
                break;
        } // end switch
        return s;
    } // end getColumnHeader

    int getColumnOrder() {
        int i = 0;
        HeaderButton hb;

        hb = (HeaderButton) getComponent(0);
        if (hb.defaultLabel.equals("Priority")) i += 100;
        if (hb.defaultLabel.equals("To Do Text")) i += 200;
        if (hb.defaultLabel.equals("Status")) i += 300;

        hb = (HeaderButton) getComponent(1);
        if (hb.defaultLabel.equals("Priority")) i += 10;
        if (hb.defaultLabel.equals("To Do Text")) i += 20;
        if (hb.defaultLabel.equals("Status")) i += 30;

        hb = (HeaderButton) getComponent(2);
        if (hb.defaultLabel.equals("Priority")) i += 1;
        if (hb.defaultLabel.equals("To Do Text")) i += 2;
        if (hb.defaultLabel.equals("Status")) i += 3;

        return i;
    } // end getColumnOrder

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        // System.out.println("TodoGroupHeader super.getPreferredSize: " + d);
        d.width = hb1.getPreferredSize().width;
        d.width += hb2.getPreferredSize().width;
        d.width += hb3.getPreferredSize().width;
        d.width += hb4.getPreferredSize().width;

        // System.out.println("TodoGroupHeader correct width: " + total);
        return d;
    } // end getPreferredSize


    void resetVisibility() {
        hb1.setVisible(((TodoGroupProperties) parent.myProperties).showPriority);
    } // end resetVisibility


    // Inner class
    //----------------------------------------------------------------
    class HeaderButton extends LabelButton implements MouseListener {
        private static final long serialVersionUID = 1L;
        String prompt;

        HeaderButton(String s) {
            super(s);
            addMouseListener(this);
        } // end constructor


        //----------------------------------------------------------------------
        // Method Name: doSorting
        //
        // Called from a thread started by the handler, so that the handler
        //   (in the main processing thread) can return immediately and allow
        //   the animation in the working dialog graphic to display correctly.
        //----------------------------------------------------------------------
        void doSorting(int shift) {
            if (defaultLabel.equals("Status")) return; // Non-sortable column

            AppTreePanel.showWorkingDialog(true);

            switch (defaultLabel) {
                case "Priority":
                    parent.sortPriority(shift);
                    break;
                case "To Do Text":
                    parent.sortText(shift);
                    break;
//                case "Deadline":    See the note in the commented-out TodoNoteGroup.sortDeadline()
//                    parent.sortDeadline(shift);
//                    break;
            }

            parent.setGroupChanged(true);
            AppTreePanel.showWorkingDialog(false);
        } // end doSorting

        void doUserHeader() {
            String s1 = getText();
            String s2 = defaultLabel;

            // initialize the columnName value
            String columnName = s1;
            String title = "User Text Entry";

            columnName = (String) JOptionPane.showInputDialog(
                    this,                         // parent component - for modality
                    prompt,                       // prompt
                    title,                        // pane title bar
                    JOptionPane.QUESTION_MESSAGE, // type of pane
                    null,                         // icon
                    null,                         // list of choices
                    columnName);                 // initial value

            if (columnName == null) return;      // No user entry
            columnName = columnName.trim();   // trim spaces

            if (columnName.equals(s1)) return; // No difference
            if (columnName.equals("")) columnName = s2; // reset value
            parent.setGroupChanged(true);

            setText(columnName);
        } // end doUserHeader

        public Dimension getPreferredSize() {
            if (!isVisible()) return new Dimension(0, 0);
            Dimension d = super.getPreferredSize();

            if (defaultLabel.equals("Priority")) {
                d.width = TodoNoteComponent.PriorityButton.minWidth;
            } // end if

            if (defaultLabel.equals("To Do Text")) {
                // System.out.println("'To Do Text' HeaderButton PreferredSize: " + d);
                d.width = NoteComponent.NoteTextField.minWidth;
            } // end if

            if (defaultLabel.equals("Status")) {
                d.width = TodoNoteComponent.StatusButton.minWidth;
            } // end if

            if (defaultLabel.equals("Deadline")) {
                d.width = 0; //parent.deadWidth;
            } // end if

            return d;
        } // end getPreferredSize

        //---------------------------------------------------------
        // MouseListener methods
        //
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            if (e.isMetaDown()) {
                doUserHeader();
            } else {
                final int shiftPressed = e.getModifiers() & InputEvent.SHIFT_MASK;
                new Thread(new Runnable() {
                    public void run() {
                        doSorting(shiftPressed);
                    }
                }).start(); // Start the thread
            } // end if
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
            String s;
            if (e.getSource() == hb3)
                s = "Sorting by Status must be done manually (shift-up/down).";
            else
                s = "Click to Sort by " + getText();

            parent.setStatusMessage(s);
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {
            parent.setStatusMessage(" ");
        } // end mouseExited

        // It was necessary to override this next method, to fix a problem
        //  that was only evident when dragging the 'To Do Text' column -
        //  The focus would cycle thru all the todo items.  This
        //  was visually annoying, but after deadlines were tied to
        //  the ThreeMonthColumn widget, it had a performance impact
        //  that was intolerable.  dlc 7/16/2004
        public void mousePressed(MouseEvent e) {
            // This has the effect of taking the focus away from any
            //  Todo Item that may have it, and sending it 'away'.
            transferFocusUpCycle();
        } // end mouseExited


        public void mouseReleased(MouseEvent e) {
        } // end mouseReleased

        //---------------------------------------------------------
    } // end class HeaderButton


} // end class TodoGroupHeader


