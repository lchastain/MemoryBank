/* ***************************************************************************
 *
 * File:  $Id: NoteComponent.java,v 1.7 2006/07/22 02:06:47 lee Exp $
 *
 * Author:  D. Lee Chastain
 *
 * $Log: NoteComponent.java,v $
 * Revision 1.7  2006/07/22 02:06:47  lee
 * Changes in support of the new data/component Note hierarchy.
 *
 * Revision 1.6  2006/02/20 01:00:45  lee
 * Reinstated serialVersionUID, for -Xlint.
 *
 * Revision 1.5  2005/12/04 16:13:48  lee
 * Removed unused variable.
 *
 * Revision 1.4  2005/08/21 14:03:12  lee
 * Changes to correctly support the various icon popup menu choices, to
 * include disabling of inappropriate choices.
 *
 * Revision 1.3  2005/08/07 15:48:28  lee
 * Corrected handling of default icon.  Added the 'reset' choice to the
 * popup menu.
 *
 * Revision 1.2  2005/07/31 17:54:41  lee
 * Changes in support of new architecture with NoteGroup as a base class
 * rather than an interface.  To significantly reduce code duplication and
 * prepare for additional 'NoteGroup' classes.
 *
 ****************************************************************************/
/**  Representation of a single Note.
 */

import java.awt.*;
import java.awt.event.*;
import java.io.Serializable;
import java.rmi.server.UID;
import java.util.*;     // Date, + ?

import javax.swing.*;
import javax.swing.event.*;   // DocumentListener
import javax.swing.border.*;

public class NoteComponent extends JPanel {
    private static final long serialVersionUID = 1L;

    // The Members
    private NoteData nd;
    protected NoteTextField noteTextField;

    // Needed by container classes to set their scrollbar unit increment.
    // BUT - change to protected and make daynote use/override it
    public static final int NOTEHEIGHT = 24;

    protected static final int NEEDS_TEXT = 77;    // Arbitrary values
    public static final int HAS_BASE_TEXT = 88;
    public static final int HAS_EXT_TEXT = 99;

    // Static values that are accessed from multiple contexts.
    protected static Border offBorder;
    protected static Border redBorder;
    protected static Border highBorder;
    protected static Border lowBorder;
    private static PopHandler popHandler;
    protected static JPopupMenu popup;

    // This is a workaround for the restriction on
    //   the use of 'this' in a static context.  We can
    //   now get past the compile-time issues, and just
    //   have to be sure and set the value at runtime.
    protected static NoteComponent ncTheNoteComponent;

    // Internal Variables needed by more than one method -
    protected NoteGroup myNoteGroup;
    protected boolean initialized = false;
    protected boolean noteChanged = false;
    protected int index;
    protected static JMenuItem miClearLine;

    static {
        //-----------------------------------
        // Create the borders.
        //-----------------------------------
        //offBorder = BorderFactory.createLineBorder(Color.gray, 2);
        offBorder = LineBorder.createGrayLineBorder();
        //offBorder = new EmptyBorder(0,3,0,0);
        redBorder = BorderFactory.createLineBorder(Color.red, 2);
        highBorder = new SoftBevelBorder(BevelBorder.RAISED);
        lowBorder = new SoftBevelBorder(BevelBorder.LOWERED);

        //-----------------------------------
        // Create the popup menus.
        //-----------------------------------
        popHandler = new PopHandler();

        popup = new JPopupMenu();
        popup.setFocusable(false);

        //--------------------------------------------
        // Define the popup menus for a NoteComponent
        //--------------------------------------------
        miClearLine = popup.add("Clear Line");
        miClearLine.addActionListener(popHandler);
    } // end static section


    NoteComponent(NoteGroup ng, int i) {
        // This constructor will make NEW items only; loading of
        //  data is done by 'setNoteData'.
        super(new BorderLayout(2, 0));
        myNoteGroup = ng;
        index = i;

        noteTextField = new NoteTextField();
        add(noteTextField, "Center");

        MemoryBank.init();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // NoteComponent Clear - calls the 'clear' for all.
    //-----------------------------------------------------------------
    protected void clear() {
        //  Clear the data object.  Since throughout the NoteComponent
        //    hierarchy there is only one data object, the next lines
        //    work for any child class's encapsulated data.
        NoteData nd = getNoteData();
        if (nd != null) nd.clear();

        // Clear the Component
        noteTextField.setText("");
        noteTextField.setForeground(Color.black);
        noteTextField.setToolTipText(null);

        myNoteGroup.reportFocusChange(this, false); // Notify the NoteGroup
        myNoteGroup.setGroupChanged();  // Ensure a group 'save'
        noteChanged = false;    // Reset our own state.
        initialized = false;    // Needs to be last.
    } // end clear


    // Do not let it grow to fill the available space in the container.
    public Dimension getMaximumSize() {
        Dimension d = super.getMaximumSize();
        return new Dimension(d.width, NOTEHEIGHT);
    } // end getMaximumSize

    //-----------------------------------------------------------------
    // Method Name: getNoteData
    //
    // Returns the data object that this component encapsulates
    //   and manages.  Used primarily in operations at
    //   the group level such as load, save, shift, sort, etc.
    //-----------------------------------------------------------------
    public NoteData getNoteData() {
        if (!initialized) return null;
        return nd;
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
    // Called when a note first has data entered into it by the user.
    //---------------------------------------------------------------------
    protected void initialize() {
        makeDataObject();
        initialized = true;
        myNoteGroup.vectGroupData.addElement(getNoteData());
        myNoteGroup.activateNextNote(index);
        myNoteGroup.reportFocusChange(this, true);
    } // end initialize


    //--------------------------------------------------------------
    // Method Name: makeDataObject
    //
    // Each child of this class (that manages a child of the
    //   NoteData class) should override this method and
    //   instantiate their own data.
    //--------------------------------------------------------------
    protected void makeDataObject() {
        nd = new NoteData();
    } // end


    //--------------------------------------------------------------
    // Method Name: noteActivated
    //
    // This method is called when a line either gains or loses
    //   focus.  It reports the event back to the encapsulating
    //   group.
    //
    // Child classes may override this method in order to add
    //   to it but they should probably call this
    //   one before going on with their new business.
    //--------------------------------------------------------------
    protected void noteActivated(boolean blnIAmOn) {
        if (!blnIAmOn) {
            if (noteChanged)
                if (noteTextField.getText().trim().equals("")) {
                    // If the line had been cleared then getNoteData
                    //   will return null.
                    NoteData nd = getNoteData();
                    if (nd == null) return;

                    // On the other hand, its text fields may have been
                    //   individually deleted, in which case we should
                    //   do the clear to enforce the rule that notes must
                    //   have text before they can have additional features.
                    if (nd.extendedNoteString.trim().equals("")) clear();
                } // end if
        } // end if

        myNoteGroup.reportFocusChange(this, blnIAmOn);
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
        // problem of a todo item being deselected as soon as
        // a date for it on the TMC was selected.  Need to see if
        // the problem(s) that it tried to fix are now back....

        noteChanged = false; // new.. 3/13/2008
    } // end resetComponent


    protected void resetMouseMessage(int textStatus) {
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
    } // end resetMouseMessage


    // This method is called each time before displaying the popup menu
    //   so that child classes may override it and customize the selections.
    protected void resetPopup() {
        popup.removeAll();
        popup.add(miClearLine);
    } // end resetPopup

    // Called by NoteGroup when shifting up/down.
    public void setActive() {
        noteTextField.requestFocusInWindow();
    } // end NoteComponent setActive


    //------------------------------------------
    // Method Name: setEditable
    //
    // Provides a pass-thru from this encapsulating
    //   component, to the actual JTextField.
    // So far (6/24/2007) only the SearchResultComponent
    //   is using, and only to set to false.
    //------------------------------------------
    public void setEditable(boolean b) {
        noteTextField.setEditable(b);
    } // end setEditable


    public void setNoteChanged() {
        myNoteGroup.setGroupChanged();
        noteChanged = true;
    } // end setNoteChanged


    //----------------------------------------------------------
    // Method Name: setNoteData
    //
    // Called directly by a NoteGroup during a load, and
    //   indirectly (via swap) for a shift up/down.
    // Do not send a null; if you want to 'un' set the note
    //   data then call 'clear' instead.
    // Child classes should override this method and then
    //   duplicate the steps rather than calling super.setNoteData.
    //   This is because their data component (the nd equivalent)
    //   will be a child class of NoteData.  In their overridden
    //   versions of resetComponent, they SHOULD call the super.
    //----------------------------------------------------------
    public void setNoteData(NoteData newNoteData) {
        nd = newNoteData;
        initialized = true;

        // update visual component without updating the 'lastModDate'
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
        //   a separatate copy of the data object, just the reference to it.

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
            DocumentListener, FocusListener, MouseListener {
        private static final long serialVersionUID = -2147072345512384327L;

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

            addKeyListener(new KeyAdapter() {
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

                // This is done in order to initialize the component -
                public void keyTyped(KeyEvent ke) {
                    MemoryBank.event();
                    if (initialized) return;

                    char kc = ke.getKeyChar();
                    if (kc == KeyEvent.VK_TAB) return;
                    if (kc == KeyEvent.VK_ENTER) return;
                    if (kc == KeyEvent.VK_BACK_SPACE) return;
                    initialize();
                } // end keyTyped
            });  // end of adding a KeyListener
        } // end constructor


        public String getExtText() {
            return getNoteData().getExtendedNoteString();
        }

        public String getSubject() {
            return getNoteData().getSubjectString();
        }

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
                strToolTip = LogUtil.getBrokenString(strToolTip, 70, 10);

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
            // System.out.println("changedUpdate: " + e.toString());
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
                if (!initialized) return;

                // Ignore double right mouse clicks.
                if (e.getClickCount() == 2) return;

                // Set the global NoteComponent.  This is the primary
                // mechanism that allows the handler to be static.
                ncTheNoteComponent = NoteComponent.this;

                // Allow a child class to enable/disable, add/remove
                //   menu items, based on which NoteComponent is active.
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
            resetMouseMessage(getTextStatus());
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

    private static class PopHandler implements ActionListener {
        public void actionPerformed(ActionEvent e) {
            if (ncTheNoteComponent == null) return;

            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            if (s.equals("Clear Line")) {
                ncTheNoteComponent.clear();
//        ncTheNoteComponent.myNoteGroup.reportComponentChange(ncTheNoteComponent, false);
// moved to 'clear' 3/4/2008 - remove these lines if all is still working
// after a while.        
            } else {
                System.out.println(s);
            } // end if/else if
            ncTheNoteComponent.setNoteChanged();
        } // end actionPerformed
    } // end class PopHandler

} // end class NoteComponent


class NoteData implements Serializable {
    private static final long serialVersionUID = 5299342314918199917L;

    private String strNoteId;
    private Date dateLastMod;
    protected String noteString;
    protected String subjectString;
    protected String extendedNoteString;
    protected int extendedNoteWidthInt;
    protected int extendedNoteHeightInt;

    public NoteData() {
        super();  // whatever happens for a generic object...

        strNoteId = new UID().toString();
        dateLastMod = new Date();
        clear();
    } // end constructor


    // The copy constructor (clone)
    public NoteData(NoteData ndCopy) {
        this();  // Provides a unique strNoteId
        this.extendedNoteHeightInt = ndCopy.extendedNoteHeightInt;
        this.extendedNoteString = ndCopy.extendedNoteString;
        this.extendedNoteWidthInt = ndCopy.extendedNoteWidthInt;
        this.noteString = ndCopy.noteString;
        this.subjectString = ndCopy.subjectString;
        this.dateLastMod = ndCopy.dateLastMod;
    } // end constructor


    protected void clear() {
        noteString = "";

        // initialize subject to null to indicate that a default should
        // be used.  A value of "" should stay "".
        subjectString = null;

        extendedNoteString = "";
        extendedNoteWidthInt = 300;
        extendedNoteHeightInt = 200;
    } // end clear

    public int getExtendedNoteHeightInt() {
        return extendedNoteHeightInt;
    }

    public String getExtendedNoteString() {
        return extendedNoteString;
    }

    public int getExtendedNoteWidthInt() {
        return extendedNoteWidthInt;
    }

    protected Date getNoteDate() {
        return null;
    }

    public String getNoteId() {
        return strNoteId;
    }

    public String getNoteString() {
        return noteString;
    }

    public Date getLastModDate() {
        return dateLastMod;
    }

    public String getSubjectString() {
        return subjectString;
    }

    public boolean hasText() {
        if (!noteString.trim().equals("")) return true;
        if (!extendedNoteString.trim().equals("")) return true;
        return false;
    } // end hasText()

    public void setExtendedNoteHeightInt(int val) {
        extendedNoteHeightInt = val;
    }


    public void setExtendedNoteString(String val) {
        extendedNoteString = val;
        dateLastMod = new Date();
    }


    public void setExtendedNoteWidthInt(int val) {
        extendedNoteWidthInt = val;
    }


    public void setNoteString(String value) {
        noteString = value;
        dateLastMod = new Date();
    }


    public void setSubjectString(String value) {
        subjectString = value;
        dateLastMod = new Date();
    }
} // end class NoteData

