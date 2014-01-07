/* ***************************************************************************
 * File:    SearchResultHeader.java
 * Author:  D. Lee Chastain
 *
 ****************************************************************************/
/**
 This class is a collection of labels in a horizontal line to be
 used as the header for a SearchResultGroup.  The labels are
 formatted and mouse-activated to appear like buttons.  The columns
 are numbered but the default initial numbering does not constrain
 the subsequent ordering of the columns.
 */

import java.awt.*;
import java.awt.event.*;
import java.util.*;           // Vector
import javax.swing.*;

public class SearchResultHeader extends Container implements ClingSource {
    private static final long serialVersionUID = 1L;

    private SearchResultGroup parent;
    private HeaderButton hb1;  // Found In
    private HeaderButton hb2;  // Note Text
    private HeaderButton hb3;  // Last Mod
    private HeaderButton hb4;  // Deadline   // No longer used.
    private DndLayout headerLayout;

    public int hb1Width;
    public int hb2Width;
    public int hb3Width;
    public int hb4Width;

    public SearchResultHeader(SearchResultGroup p) {
        super();
        parent = p;
        hb1Width = 0;
        hb2Width = 0;
        hb3Width = 0;
        hb4Width = 0;
        headerLayout = new DndLayout();
        headerLayout.setMoveable(true); // Must be BEFORE add.
        headerLayout.setClingSource(this);
        setLayout(headerLayout);

        // Default Labels -
        hb1 = new HeaderButton("Found In");
        hb2 = new HeaderButton("Note Text");
        hb3 = new HeaderButton("Last Mod");
        hb4 = new HeaderButton("Deadline");

        // Whatever was stored...
        hb1.setText(parent.myVars.column1Label);
        hb2.setText(parent.myVars.column2Label);
        hb3.setText(parent.myVars.column3Label);
        hb4.setText(parent.myVars.column4Label);

        add(hb1, "First");
        add(hb2, "Stretch");
        add(hb3, "Third");
        // add(hb4, "Fourth");

        // Re-order, if necessary
        String pos = String.valueOf(parent.myVars.columnOrder);
        // System.out.println("In SearchResultHeader, columnOrder = " + pos);
        add(hb1, pos.indexOf("1"));
        add(hb2, pos.indexOf("2"));
        add(hb3, pos.indexOf("3"));
        // add(hb4, pos.indexOf("4"));

    } // end constructor


    //----------------------------------------------------------------
    // Method Name: doLayout
    //
    // Overrode this Container method in order to capture the column
    //   order change as a 'group changed' event.
    //----------------------------------------------------------------
    public void doLayout() {
        super.doLayout();
        if (headerLayout.Dragging) return;

        // System.out.println("SearchResultHeader.doLayout");

        int origColumnOrder = parent.myVars.columnOrder;
        if (getColumnOrder() != origColumnOrder) {
            parent.myVars.columnOrder = getColumnOrder();
            parent.setGroupChanged();
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
        Vector<JComponent> ClingOns = new Vector<JComponent>(rows, 1);

        HeaderButton hb = (HeaderButton) comp;

        SearchResultComponent tnc;

        for (int i = 0; i < rows; i++) {
            tnc = (SearchResultComponent) cTheContainer.getComponent(i);
            ((DndLayout) tnc.getLayout()).Dragging = true;

            // System.out.println("SearchResultHeader: hb.defaultLabel = " + hb.defaultLabel);

            // We still need to change the order, based on actual order.
            if (hb.defaultLabel.equals("Found In")) {
                compTempComp = tnc.getFoundInButton();
            } else if (hb.defaultLabel.equals("Note Text")) {
                compTempComp = tnc.getNoteTextField();
            } else if (hb.defaultLabel.equals("Last Mod")) {
                compTempComp = tnc.getLastModLabel();
            } else {
                // Now that there are only 3, this will throw an exception
                //   if it ever gets here.  Left it in to show me the problem
                //   in case it ever happens, and also so that the compiler
                //   believes that compTempComp will always have a value.
                compTempComp = (JComponent) tnc.getComponent(3);
            } // end if
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

    public int getColumnOrder() {
        int i = 0;
        HeaderButton hb;

        hb = (HeaderButton) getComponent(0);
        if (hb.defaultLabel.equals("Found In")) i += 100;
        if (hb.defaultLabel.equals("Note Text")) i += 200;
        if (hb.defaultLabel.equals("Last Mod")) i += 300;

        hb = (HeaderButton) getComponent(1);
        if (hb.defaultLabel.equals("Found In")) i += 10;
        if (hb.defaultLabel.equals("Note Text")) i += 20;
        if (hb.defaultLabel.equals("Last Mod")) i += 30;

        hb = (HeaderButton) getComponent(2);
        if (hb.defaultLabel.equals("Found In")) i += 1;
        if (hb.defaultLabel.equals("Note Text")) i += 2;
        if (hb.defaultLabel.equals("Last Mod")) i += 3;

        // System.out.println("SearchResultHeader.getColumnOrder: " + i);
        return i;
    } // end getColumnOrder

    public Dimension getPreferredSize() {
        Dimension d = super.getPreferredSize();
        // System.out.println("SearchResultHeader super.getPreferredSize: " + d);
        d.width = hb1.getPreferredSize().width;
        d.width += hb2.getPreferredSize().width;
        d.width += hb3.getPreferredSize().width;
        d.width += hb4.getPreferredSize().width;

        // System.out.println("SearchResultHeader correct width: " + total);
        return d;
    } // end getPreferredSize


    // Inner class
    //----------------------------------------------------------------
    class HeaderButton extends LabelButton implements MouseListener {
        private static final long serialVersionUID = 1L;
        String prompt;

        public HeaderButton(String s) {
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
        public void doSorting(int shift) {
            LogTree.showWorkingDialog(true);
            if (defaultLabel.equals("Note Text")) parent.sortNoteString(shift);
            else if (defaultLabel.equals("Last Mod")) parent.sortLastMod(shift);
            parent.setGroupChanged();
            LogTree.showWorkingDialog(false);
        } // end doSorting

        public void doUserHeader() {
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
            parent.setGroupChanged();

            setText(columnName);
        } // end doUserHeader

        public Dimension getPreferredSize() {
            if (!isVisible()) return new Dimension(0, 0);
            Dimension d = super.getPreferredSize();

            if (defaultLabel.equals("Found In")) {
                d.width = SearchResultComponent.FoundInButton.intWidth;
                hb1Width = d.width;
            } // end if

            if (defaultLabel.equals("Note Text")) {
                // System.out.println("'Note Text' HeaderButton PreferredSize: " + d);
                d.width = NoteComponent.NoteTextField.minWidth;
                hb2Width = d.width;
            } // end if

            if (defaultLabel.equals("Last Mod")) {
                d.width = SearchResultComponent.LastModLabel.intWidth;
                hb3Width = d.width;
            } // end if

            if (defaultLabel.equals("Deadline")) {
                d.width = 0; //parent.deadWidth;
                hb4Width = d.width;
            } // end if

            return d;
        } // end getPreferredSize

        //---------------------------------------------------------
        // MouseListener methods
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
            s = "Click to Sort by " + getText();

            parent.setMessage(s);
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {
            parent.setMessage(" ");
        } // end mouseExited

        // It was necessary to override this next method, to fix a problem
        //  that was only evident when dragging the 'Note Text' column -
        //  The focus would cycle thru all the items.
        public void mousePressed(MouseEvent e) {
            // This has the effect of taking the focus away from any
            //   item that may have it, and sending it 'away'.
            transferFocusUpCycle();
        } // end mouseExited


        public void mouseReleased(MouseEvent e) {
        } // end mouseReleased
        //---------------------------------------------------------
    } // end class HeaderButton


} // end class SearchResultHeader


