/*  Representation of a single Note.
 */

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.*;

public class NoteComponent extends JPanel {
    private static final long serialVersionUID = 1L;

    // The Members
    private NoteData myNoteData;
    NoteTextField noteTextField;

    // Needed by container classes to set their scrollbar unit increment.
    // BUT - change to protected and make daynote use/override it
    static final int NOTEHEIGHT = 24;

    static final int NEEDS_TEXT = 77;    // Arbitrary values
    static final int HAS_BASE_TEXT = 88;
    static final int HAS_EXT_TEXT = 99;

    // Static values that are accessed from multiple contexts.
    private static Border offBorder;
    static Border redBorder;
    static Border highBorder;
    static Border lowBorder;
    static JPopupMenu popup;

    // This is how we get around the restriction on the
    //   use of 'this' in a static context (PopHandler).  We just
    //   have to be sure and set the value at runtime.
    static NoteComponent ncTheNoteComponent;

    // Internal Variables needed by more than one method -
    protected NoteGroup myNoteGroup;
    protected boolean initialized = false;
    private boolean noteChanged = false;
    protected int index;
    private static JMenuItem miClearLine;
    private static JMenuItem miCutLine;
    private static JMenuItem miCopyLine;
    private static JMenuItem miPasteLine;

    static {
        //-----------------------------------
        // Create the borders.
        //-----------------------------------
        offBorder = LineBorder.createGrayLineBorder();
        redBorder = BorderFactory.createLineBorder(Color.red, 2);
        highBorder = new SoftBevelBorder(BevelBorder.RAISED);
        lowBorder = new SoftBevelBorder(BevelBorder.LOWERED);

        //-----------------------------------
        // Create the popup menus.
        //-----------------------------------
        PopHandler popHandler = new PopHandler();

        popup = new JPopupMenu();
        popup.setFocusable(false);

        //--------------------------------------------
        // Define the popup menus for a NoteComponent
        //--------------------------------------------
        miCutLine = popup.add("Cut Line");
        miCutLine.addActionListener(popHandler);
        miCopyLine = popup.add("Copy Line");
        miCopyLine.addActionListener(popHandler);
        miPasteLine = popup.add("Paste Line");
        miPasteLine.addActionListener(popHandler);
        miClearLine = popup.add("Clear Line");
        miClearLine.addActionListener(popHandler);
    } // end static section

    @Override
    public boolean hasFocus() {
        return noteTextField.hasFocus();
    }

    NoteComponent(NoteGroup ng, int i) {
        // This constructor will make NEW items only; loading of
        //  data is done by 'setNoteData'.
        super(new BorderLayout(2, 0));
        myNoteGroup = ng;
        index = i;

        noteTextField = new NoteTextField();
        add(noteTextField, "Center");

        MemoryBank.trace();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // Clear the visible text field AND clear the underlying data.
    // Child classes will override this in order to clear their own
    // components first, but they should call this method afterwards.
    //-----------------------------------------------------------------
    protected void clear() {
        // Clear the (base) Component - ie, the noteTextField
        noteTextField.setText("");
        noteTextField.setForeground(Color.black);
        noteTextField.setToolTipText(null);

        // Clear the data object.  Since child classes override the
        //   getNoteData method, this works for them as well.
        NoteData nd = getNoteData(); // this only works if initialized
        if (nd != null) nd.clear();

        // Notify the NoteGroup
        myNoteGroup.setGroupChanged();  // Ensure a group 'save'

        // Reset our own state and prepare this component to be reused -
        noteChanged = false;
        initialized = false;
    } // end clear


    // Do not let it grow to fill the available space in the container.
    public Dimension getMaximumSize() {
        Dimension d = super.getMaximumSize();
        return new Dimension(d.width, NOTEHEIGHT);
    } // end getMaximumSize

    //-----------------------------------------------------------------
    // Method Name: getNoteData
    //
    // Returns the data object that this component encapsulates and manages.
    //-----------------------------------------------------------------
    public NoteData getNoteData() {
        return myNoteData;
    } // end getNoteData


    public JComponent getNoteTextField() {
        return noteTextField;
    }

    // Need to keep the height constant.
    public Dimension getPreferredSize() {
        int minWidth = 100; // For the Text Field
        return new Dimension(minWidth, NOTEHEIGHT);
    } // end getPreferredSize


    public int getTextStatus() {
        int textStatus = NEEDS_TEXT;

        if (!initialized) return textStatus;
        NoteData tmpNoteData = getNoteData();
        if (tmpNoteData == null) return textStatus;

        if (tmpNoteData.getNoteString().trim().equals("")) {
            textStatus = NEEDS_TEXT;
        } else {
            textStatus = HAS_BASE_TEXT;
        } // end if

        if (!tmpNoteData.getExtendedNoteString().trim().equals("")) {
            textStatus = HAS_EXT_TEXT;
        } // end if

        return textStatus;
    } // end getTextStatus


    //---------------------------------------------------------------------
    // Method Name: initialize
    //
    // 'initialized' means that a NoteComponent has had text entered into
    //    its noteString.  This helps to reduce the work of the keyTyped
    //    handler.
    //
    // Aside from setting this critical flag, this method asociates a new
    //   data object with this NoteComponent (if it does not already have
    //   one), adds the data component to the group data Vector (if it is
    //   not already there), and enables the next NoteComponent to accept text
    //   entry.
    //
    // Called when a note first has data entered into it by the user.
    //---------------------------------------------------------------------
    protected void initialize() {
        NoteData theData = getNoteData();
        if (theData == null) makeDataObject();
        if (index >= myNoteGroup.lastVisibleNoteIndex) {
            // A NoteComponent will lose initialization and visible text if cleared,
            //   but will retain its data object.  We only want to add 'new' data
            //   to the group Vector.
            myNoteGroup.groupDataVector.addElement(getNoteData());
            myNoteGroup.activateNextNote(index);
        }
        initialized = true;
    } // end initialize


    //--------------------------------------------------------------
    // Method Name: makeDataObject
    //
    // Each child of this class (that manages a child of the
    //   NoteData class) should override this method and
    //   instantiate their own data.  Those overrides
    //   should not call super().
    //--------------------------------------------------------------
    protected void makeDataObject() {
        myNoteData = new NoteData();
    }


    //--------------------------------------------------------------
    // Method Name: noteActivated
    //
    // This method is called when a line either gains or loses focus.
    //
    // Child classes may override this method in order to do their
    //   own thing but they should probably call this one after that.
    //--------------------------------------------------------------
    protected void noteActivated(boolean blnIAmOn) {
        if (!blnIAmOn) {
            if (noteChanged)
                if (noteTextField.getText().trim().equals("")) {
                    NoteData nd = getNoteData();
                    if (nd == null) return; // Can this still happen?

                    //   Here we enforce the rule that notes must
                    //   have text before they can have additional features.
                    if (nd.extendedNoteString.trim().equals("")) clear();
                } // end if
        } // end if
    } // end noteActivated


    //----------------------------------------------------------
    // Method Name: resetComponent
    //
    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change without affecting the
    //   'lastModDate' since this method may be getting called
    //   after a data load, or a non-data change such as a swap.
    //----------------------------------------------------------
    protected void resetComponent() {

        String s;
        if (getNoteData() == null) s = "";
        else s = getNoteData().getNoteString();

        // Set the text of the component without affecting the lastModDate
        noteTextField.getDocument().removeDocumentListener(noteTextField);
        noteTextField.setText(s);
        noteTextField.getDocument().addDocumentListener(noteTextField);

        noteTextField.setTextColor();
        noteTextField.resetToolTip(getNoteData());
//    noteTextField.transferFocusUpCycle();  // new.. 3/19/2008
        // Commented out the above line, 7/20/2008, to clear the
        // problem of a to-do item being deselected as soon as
        // a date for it on the TMC was selected.  Need to see if
        // the problem(s) that it tried to fix are now back....

        noteChanged = false; // new.. 3/13/2008
    } // end resetComponent


    protected void resetNoteStatusMessage(int textStatus) {
        String s = " ";

        switch (textStatus) {
            case NEEDS_TEXT:
                s = "Click here to enter text for this note.";
                break;
            case HAS_BASE_TEXT:
                s = "Press 'Enter' to add a subject or an extended note.";
                break;
            case HAS_EXT_TEXT:
                // This gives away the 'hidden' text, if
                //   there is no primary (blue) text.
                s = "Double-click or press 'Enter' to see/edit";
                s += " the subject and extended note.";
        } // end switch
        myNoteGroup.setMessage(s);
    } // end resetNoteStatusMessage


    // This method is called each time before displaying the popup menu.
    //   Child classes may override it if they have additional selections,
    //   but they can still call this one first, to add the base items.
    protected void resetPopup() {
        popup.removeAll();
        popup.add(miCutLine);   // the default state is 'enabled'.
        popup.add(miCopyLine);
        popup.add(miPasteLine);
        popup.add(miClearLine);

        if (!initialized) {
            miCutLine.setEnabled(false);
            miCopyLine.setEnabled(false);
            miPasteLine.setEnabled(MemoryBank.clipboardNote != null);
            miClearLine.setEnabled(false);
            return;
        }

        NoteData menuNoteData = getNoteData();
        if (null != menuNoteData && menuNoteData.hasText()) {
            miCutLine.setEnabled(true);
            miCopyLine.setEnabled(true);
            miPasteLine.setEnabled(false);
            miClearLine.setEnabled(true);
        } else {
            // Find out how this happens, or confirm that it does not.
            throw new AssertionError();  // Added 8/30/2019 - remove, after this has not occurred for some time.
        }
    } // end resetPopup

    // Called by NoteGroup when shifting up/down.
    public void setActive() {
        noteTextField.requestFocusInWindow();
    } // end NoteComponent setActive


    public void setNoteChanged() {
        myNoteGroup.setGroupChanged();
        noteChanged = true;
    } // end setNoteChanged


    //----------------------------------------------------------
    // Method Name: setNoteData
    //
    // Set the data for this component.  Do not send a null; if you want
    //   to unset the NoteData then call 'clear' instead.
    // Child classes should override this method and then
    //   duplicate the needed steps rather than calling super.setNoteData.
    //   This is because their DATA component (the myNoteData equivalent)
    //   will be a child class of NoteData and not the instance that is
    //   affected here.  But in their overridden
    //   versions of resetComponent in order to show the change, they
    //   SHOULD call the super so that the changes here will also be seen.
    //----------------------------------------------------------
    public void setNoteData(NoteData newNoteData) {
        myNoteData = newNoteData;

        // show the updated noteString
        initialized = true; // but do not update the 'lastModDate'
        resetComponent();
        setNoteChanged();
    } // end setNoteData


    //----------------------------------------------------------------------------
    // The two 'shift' methods below were written so that child classes can
    //   override them and provide alternate behavior.  The alternate
    //   behavior would not be appropriate in all child class instances, in
    //   which case the child class does not need to override.
    // Known (needed) overrides at this time: DayNoteComponent.
    //----------------------------------------------------------------------------
    protected void shiftDown() {
        myNoteGroup.shiftDown(index);
    } // end shiftDown

    protected void shiftUp() {
        myNoteGroup.shiftUp(index);
    } // end shiftUp

    //------------------------------------------------------------------
    // Method Name: swap
    //
    //------------------------------------------------------------------
    public void swap(NoteComponent nc) {
        // Get a reference to the two data objects
        NoteData nd1 = this.getNoteData();
        NoteData nd2 = nc.getNoteData();

        // Note: getNoteData and setNoteData are working with references
        //   to data objects.  If you 'get' data from the component into a
        //   local variable and then later clear the component, you also
        //   cleared the data in your local variable because you never had
        //   a separate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (nd1 != null) nd1 = new NoteData(nd1);
        if (nd2 != null) nd2 = new NoteData(nd2);

        if (nd1 == null) nc.clear();
        else nc.setNoteData(nd1);

        if (nd2 == null) this.clear();
        else this.setNoteData(nd2);

        System.out.println("NoteComponent.swap");

        myNoteGroup.setGroupChanged();
    } // end swap

    //---------------------------------------------------------
    // End of NoteComponent specific methods
    //---------------------------------------------------------

    //---------------------------------------------------------
    // Inner Classes -
    //---------------------------------------------------------

    // This class implements a text field with a red border that
    //   appears when the focus is gained.
    protected class NoteTextField extends JTextField implements ActionListener,
            DocumentListener, FocusListener, MouseListener, KeyListener {
        private static final long serialVersionUID = 1L;
        public static final int minWidth = 80;

        public NoteTextField() {
            super();

            // See the KeyListener tutorial -
            // This is needed so that the KeyListener will hear a TAB.
            setFocusTraversalKeysEnabled(false);

            setBorder(offBorder);
            addMouseListener(this);
            setFont(Font.decode("Dialog-bold-14"));
            addActionListener(this);
            addFocusListener(this);
            getDocument().addDocumentListener(this);
            addKeyListener(this);
        } // end constructor

        // By sizing the text field to a smaller width than its container,
        //   it does not go beyond the viewable area,
        //   but it does expand to fit.  This cures the perceived error of
        //   an inability to scroll horizontally within the text field,
        //   that it previously had when it was longer than the container.
        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();

            // System.out.println("NoteTextField preferred size: " + d);
            d.width = minWidth;
            return d;
        } // end getPreferredSize

        private void resetToolTip(NoteData nd) {
            if (nd == null) return;

            String ss = nd.getSubjectString();
            String ns = nd.getExtendedNoteString().trim();
            String strToolTip;

            if (ss != null) {
                // System.out.println("Setting the tool tip to: " + ss);
                if (ss.trim().equals("")) ss = null;
            } // end if

            // The tool tip will be a concatenation of the subject
            //   and the extended note.  If one is not present then
            //   it will just be the other.  If neither, then null.
            if ((ss != null) && !ns.equals("")) {
                strToolTip = ss + "\n" + ns;
            } else if (ss != null) {
                strToolTip = ss;
            } else if (!ns.equals("")) {
                strToolTip = ns;
            } else {
                strToolTip = null;
            } // end if / else - setting strToolTip

            if (strToolTip != null) {
                // Insert line breaks as needed and enforce
                //   an overall text length limit.
                strToolTip = AppUtil.getBrokenString(strToolTip, 70, 10);

                // In case we had a (too large) gap of linefeeds in the middle
                //   and the cutoff didn't make it back to real text -
                strToolTip = strToolTip.trim();

                // Convert potentially malicious characters.
                strToolTip = strToolTip.replace("&", "&amp;");
                strToolTip = strToolTip.replace("<", "&lt;");

                // Wrap in HTML and PREserve the (remaining) formatting.
                strToolTip = "<html><pre>" + strToolTip + "</pre></html>";
            } // end if

            setToolTipText(strToolTip);
        } // end resetToolTip


        private void setTextColor() {
            NoteData nd = getNoteData();
            if (nd == null) return;
            if (!nd.getExtendedNoteString().trim().equals(""))
                setForeground(Color.blue);
            else
                setForeground(Color.black);
        } // end setTextColor

        //=====================================================================
        // EVENT HANDLERS
        //=====================================================================

        //---------------------------------------------------------
        // Method Name: actionPerformed
        //
        // Although there are several child classes, only this base
        //   class is handling events on the JTextField.  This
        //   method will be called directly as the event handler
        //   when the field has the
        //   focus and the user presses 'Enter', and indirectly
        //   when they mouse double-click on the field.
        //---------------------------------------------------------
        public void actionPerformed(ActionEvent ae) {
            MemoryBank.event();
            if (!this.isEditable()) return;
            if (!initialized) return;

            boolean extendedNoteChanged;

            // Highlight this note to show it is the one being modified.
            NoteComponent.this.setBorder(redBorder);

            NoteData tmpNoteData = getNoteData();
            extendedNoteChanged = myNoteGroup.editExtendedNoteComponent(tmpNoteData);

            if (extendedNoteChanged) {
                // Set (or clear) the tool tip.
                resetToolTip(tmpNoteData);

                setTextColor();
                setNoteChanged();
            } // end if

            // Remove the 'modification in progress' highlight
            NoteComponent.this.setBorder(null);
        } // end actionPerformed

        //---------------------------------------------------------
        // DocumentListener methods
        //---------------------------------------------------------
        public void insertUpdate(DocumentEvent e) {
            // System.out.println("insertUpdate: " + e.toString());
            if (!initialized) initialize();

            getNoteData().setNoteString(getText());
            setNoteChanged();
        } // end insertUpdate

        public void removeUpdate(DocumentEvent e) {
            // System.out.println("removeUpdate: " + e.toString());
            getNoteData().setNoteString(getText());
            setNoteChanged();
        } // end removeUpdate

        public void changedUpdate(DocumentEvent e) {
            System.out.println("changedUpdate: " + e.toString());
            getNoteData().setNoteString(getText());
            setNoteChanged();
        } // end changedUpdate


        //---------------------------------------------------------
        // FocusListener methods for the TextField
        //---------------------------------------------------------
        // Note: The order of focusGained / focusLost along with
        //  the visual indicators (borders, highlighting) and
        //  how they can be invoked by either up/down arrows,
        //  mouse clicks, or the tab key - is critical!
        //  The key to it all is to disallow other components from
        //  getting the focus when they appear (most notably, the
        //  PopupMenu and the vertical scrollbar).  Then, do
        //  everything based on Focus here being gained or lost.
        //---------------------------------------------------------

        public void focusGained(FocusEvent e) {
            // System.out.println("focusGained for index " + index);
            setBorder(redBorder);
            NoteComponent.this.scrollRectToVisible(getBounds());

            // We occasionally get a null pointer exception at startup.
            if (getCaret() == null) return;
            getCaret().setVisible(true);

            if (!initialized) return;
            noteActivated(true);
        } // end focusGained

        public void focusLost(FocusEvent e) {
            // System.out.println("focusLost for index " + index);
            setBorder(offBorder);
            getCaret().setVisible(false);

            noteActivated(false);
        } // end focusLost

        //---------------------------------------------------------
        // KeyListener methods for the TextField
        //---------------------------------------------------------
        @Override
        public void keyPressed(KeyEvent ke) {
            // Turn off a previous popup, if one is showing.
            // This could happen if a menu was showing, then the user
            //   pressed a TAB, UP or DOWN key to change focus - the
            //   popup menu would still be up, but active for the
            //   previous note vs the one that appeared to be active.
            if (popup.isVisible()) popup.setVisible(false);

            int kp = ke.getKeyCode();

            boolean shifted = ke.isShiftDown();

            // Translate TAB / Shift-TAB into DOWN / UP
            if (kp == KeyEvent.VK_TAB) {
                kp = KeyEvent.VK_DOWN;
                if (shifted) {
                    shifted = false;
                    kp = KeyEvent.VK_UP;
                } // end if
            } // end if

            if ((kp != KeyEvent.VK_DOWN) && (kp != KeyEvent.VK_UP)) return;

            if (shifted) {
                // System.out.println();
                if (kp == KeyEvent.VK_UP) shiftUp();
                else shiftDown();
            } else {
                // This is done in order to have the DOWN / UP arrow keys
                //  behave like the TAB / SHIFT-TAB keys.
                if (kp == KeyEvent.VK_DOWN) NoteTextField.this.transferFocus();
                else NoteTextField.this.transferFocusBackward();
            } // end if
        } // end keyPressed

        @Override
        public void keyReleased(KeyEvent e) {
        }

        @Override
        public void keyTyped(KeyEvent ke) {
            if (initialized) return;

            char kc = ke.getKeyChar();
            if (kc == KeyEvent.VK_TAB) return;
            if (kc == KeyEvent.VK_ENTER) return;
            if (kc == KeyEvent.VK_BACK_SPACE) return;
            initialize();
            noteActivated(true); // Added for SCR0091
        } // end keyTyped

        //---------------------------------------------------------
        // MouseListener methods for the TextField
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            MemoryBank.event();
            if (!this.hasFocus()) {
                // The rmb click does not get focus.
                requestFocusInWindow();
            }

            if (!this.isEditable()) return;
            int m = e.getModifiers();

            // Single click, right mouse button.
            if ((m & InputEvent.BUTTON3_MASK) != 0) {
                // System.out.println("Right click on index " + index);

                // In earlier Java versions (before 1.6), The rmb click
                //   did not get focus, so -
                // requestFocusInWindow();

                // NOTE:  The above 'request' is subject to queuing and
                //  handling priorities; it will not be honored until after
                //  this method completes (because it is an event handler)
                //  AND all other focus events are handled, including the
                //  focusLost method of the note that currently has it,
                //  but is about to lose it as a result of this request.

                // Do not allow the popup menu on the last visible note.
//                if (!initialized) return;   // 8/30/2019 Commented this line to now allow popup for 'Paste', with all other options disabled.  Remove when feature complete.

                // Ignore double right mouse clicks.
                if (e.getClickCount() == 2) return;

                // Set the global NoteComponent.  This is the primary
                // mechanism that allows the handler to be static.
                ncTheNoteComponent = NoteComponent.this;

                // Child classes will override resetPopup to enable/disable, add/remove
                //   menu items, based on the data content of the active NoteComponent.
                resetPopup();

                // Show the popup menu
                // System.out.println("Showing popup!");
                popup.show(e.getComponent(), e.getX(), e.getY());

                // Double click, left mouse button.
            } else if (e.getClickCount() == 2) {
                actionPerformed(new ActionEvent(e.getSource(), 0, ""));
            } // end if/else if
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
            popup.setVisible(false);
            if (!initialized) return;
            resetNoteStatusMessage(getTextStatus());
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {
            myNoteGroup.setMessage(" ");
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }

    } // end class NoteTextField

    //--------------------------------------------------------------------

    // The PopHandler needs to be static; otherwise we get one for every
    // component in the NoteGroup, and ALL of them would respond to the
    // "one" requesting MenuItem.
    // This is because the menu items are also static, so the same JMenuItem
    // is being shown for each NoteComponent, with either a single ActionListener,
    // or several of them if this class were not also static.
    private static class PopHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (ncTheNoteComponent == null) return;

            JMenuItem jm = (JMenuItem) e.getSource();
            NoteData nd; // Needed to isolate old/source data from new/pasted.
            String theMenuItemText = jm.getText();
            switch (theMenuItemText) {
                case "Cut Line":
                    nd = ncTheNoteComponent.getNoteData();
                    MemoryBank.clipboardNote = nd.copy();
                    ncTheNoteComponent.clear();
                    break;
                case "Copy Line":
                    nd = ncTheNoteComponent.getNoteData();
                    MemoryBank.clipboardNote = nd.copy();
                    break;
                case "Paste Line":
                    ncTheNoteComponent.initialize();
                    // Pasting a copy allows us to do a paste multiple times without re-copying.
                    ncTheNoteComponent.setNoteData(MemoryBank.clipboardNote.copy());
                    break;
                case "Clear Line":
                    ncTheNoteComponent.clear();
                    break;
                default:
                    System.out.println(theMenuItemText);
                    break;
            }
            ncTheNoteComponent.setNoteChanged();
        } // end actionPerformed
    } // end class PopHandler

} // end class NoteComponent


