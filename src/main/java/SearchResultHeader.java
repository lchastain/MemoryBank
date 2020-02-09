/*
 This class is a collection of labels in a horizontal line to be
 used as the header for a SearchResultGroup.  The labels are
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

public class SearchResultHeader extends Container implements ClingSource {
    private static final long serialVersionUID = 1L;

    private SearchResultGroup parent;
    HeaderButton hb1;  // Found In
    HeaderButton hb2;  // Note Text
    HeaderButton hb3;  // Last Mod
    HeaderButton hb4;  // Deadline   // No longer used.
    DndLayout headerLayout;

    public SearchResultHeader(SearchResultGroup p) {
        super();
        parent = p;
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

        add(hb1, "First");
        add(hb2, "Stretch");
        add(hb3, "Third");

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
    @Override
    public void doLayout() {
        super.doLayout();
        if (headerLayout.Dragging) return;

        // System.out.println("SearchResultHeader.doLayout");

        int origColumnOrder = parent.myVars.columnOrder;
        if (getColumnOrder() != origColumnOrder) {
            parent.myVars.columnOrder = getColumnOrder();
            parent.setLeafChanged(true);
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
        JComponent compTempComp = null;

        Container cTheContainer = parent.groupNotesListPanel;
        int rows = cTheContainer.getComponentCount();
        Vector<JComponent> ClingOns = new Vector<>(rows, 1);

        HeaderButton hb = (HeaderButton) comp;

        SearchResultComponent tnc;

        for (int i = 0; i < rows; i++) {
            tnc = (SearchResultComponent) cTheContainer.getComponent(i);
            ((DndLayout) tnc.getLayout()).Dragging = true;

            // System.out.println("SearchResultHeader: hb.defaultLabel = " + hb.defaultLabel);

            // We still need to change the order, based on actual order.
            switch (hb.defaultLabel) {
                case "Found In":
                    compTempComp = tnc.getFoundInButton();
                    break;
                case "Note Text":
                    compTempComp = tnc.getNoteTextField();
                    break;
                case "Last Mod":
                    compTempComp = tnc.getLastModLabel();
                    break;
            }
            ClingOns.addElement(compTempComp);
        } // end for i
        return ClingOns;
    } // end getClingons


    String getColumnHeader(int i) {
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

        // System.out.println("SearchResultHeader correct width: " + total);
        return d;
    } // end getPreferredSize


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
            if (defaultLabel.equals("Found In")) return;
            AppTreePanel.showWorkingDialog(true);
            if (defaultLabel.equals("Note Text")) parent.sortNoteString(shift);
            else if (defaultLabel.equals("Last Mod")) parent.sortLastMod(shift);
            parent.setLeafChanged(true);
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
            parent.setLeafChanged(true);

            setText(columnName);
        } // end doUserHeader

        public Dimension getPreferredSize() {
            if (!isVisible()) return new Dimension(0, 0);
            Dimension d = super.getPreferredSize();

            if (defaultLabel.equals("Found In")) {
                d.width = SearchResultComponent.FoundInButton.intWidth;
            } // end if

            if (defaultLabel.equals("Note Text")) {
                // System.out.println("'Note Text' HeaderButton PreferredSize: " + d);
                d.width = NoteComponent.NoteTextField.minWidth;
            } // end if

            if (defaultLabel.equals("Last Mod")) {
                d.width = SearchResultComponent.LastModLabel.intWidth;
            } // end if

            if (defaultLabel.equals("Deadline")) {
                d.width = 0; //parent.deadWidth;
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
            String theHeaderText = getText();
            if(defaultLabel.equals("Found In")) {
                parent.setMessage("Sorry, unable to sort by '" + theHeaderText + "'.");
            } else {
                parent.setMessage("Click to Sort by " + theHeaderText + " and shift-click to sort descending.");
            }
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


