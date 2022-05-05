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
    boolean editable = true;
    static MouseEvent lastMouseEnteredEvent; // Needed for tooltip management

    // The Members
    NoteData myNoteData;
    NoteTextField noteTextField;

    // Needed by container classes to set their scrollbar unit increment.
    static final int NOTEHEIGHT = 24;

    static final int NEEDS_TEXT = 77;    // Arbitrary values
    static final int HAS_BASE_TEXT = 88;
    static final int HAS_EXT_TEXT = 99;

    // Static values that are accessed from multiple contexts.
    private static final Border offBorder;
    static Border redBorder;
    static Border highBorder;
    static Border lowBorder;
    static JPopupMenu contextMenu;

    // This is how we get around the restriction on the
    //   use of 'this' in a static context (PopHandler).  We just
    //   have to be sure and set the value at runtime.
    static NoteComponent theNoteComponent;

    // Internal Variables needed by more than one method -
    NoteComponentManager myManager;
    protected NoteGroupPanel myNoteGroupPanel;
    static NoteSelection mySelectionMonitor;
    protected boolean initialized = false;
    protected int index;
    private static final JMenuItem miClearLine;
    private static final JMenuItem miCutLine;
    private static final JMenuItem miCopyLine;
    private static final JMenuItem miPasteLine;

    static {
        // This ensures that mySelectionMonitor will never be null; now it may or may not be replaced.
        mySelectionMonitor = new NoteSelection() { }; // Unless this is replaced it will just use the default (no-op) methods.
        // LinkTargetSelectionPanel is the only real client of this; LinkagesEditorPanel is where it is reassigned.

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

        contextMenu = new JPopupMenu();
        contextMenu.setFocusable(false);

        //--------------------------------------------
        // Define the popup menus for a NoteComponent
        //--------------------------------------------
        miCutLine = contextMenu.add("Cut Line");
        miCutLine.addActionListener(popHandler);
        miCopyLine = contextMenu.add("Copy Line");
        miCopyLine.addActionListener(popHandler);
        miPasteLine = contextMenu.add("Paste Line");
        miPasteLine.addActionListener(popHandler);
        miClearLine = contextMenu.add("Clear Line");
        miClearLine.addActionListener(popHandler);
    } // end static section

    @Override
    public boolean hasFocus() {
        return noteTextField.hasFocus();
    }

    NoteComponent(NoteComponentManager noteComponentManager, int i) {
        super(new BorderLayout(2, 0));
        myManager = noteComponentManager;  // A NoteGroup, or the LinkagesEditorPanel
        if(myManager instanceof NoteGroupPanel) {
            myNoteGroupPanel = (NoteGroupPanel) myManager;
        }
        index = i;

        makeDataObject(); // Child classes override this method and set their own data types.

        noteTextField = new NoteTextField();
        if(!editable) {
            noteTextField.setEditable(false);
        }

        // This section disables the automatic scrolling done by a JScrollPane
        // when a component that it contains 'hears' an UP or DOWN arrow key.
        // This is done because we have our own scrolling logic, and
        // ours works better because it works in whole-note heights, vs the half-height that
        // we get otherwise.  Also, ours only kicks in when the note line would otherwise
        // not be fully visible, as opposed to every time the focus moves.
        //-------------------------------------------------------------------------------------
        InputMap im = noteTextField.getInputMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "scrollDown");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "scrollUp");
        ActionMap am = noteTextField.getActionMap();
        am.put("scrollDown", new AbstractAction() {
            static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                //System.out.println(e.getSource() + " - no go down");
            }
        });
        am.put("scrollUp", new AbstractAction() {
            static final long serialVersionUID = 1L;
            @Override
            public void actionPerformed(ActionEvent e) {
                //System.out.println(e.getSource() + " - no go up");
            }
        });
        //-------------------------------------------------------------------------------------

        add(noteTextField, "Center");

        MemoryBank.trace();
    } // end constructor


    // Method Name: clear
    // Clear the visible text field AND clear the underlying data.
    // Child classes will override this in order to clear their own
    // components first, but they should call this method afterwards.
    //-----------------------------------------------------------------
    void clear() {
        // Clear the data object.  Since child classes override the
        //   getNoteData method, this works for them as well.
        MemoryBank.debug("NoteComponent.clear, calling getNoteData()!"); // scr0050 troubleshooting.
        NoteData nd = getNoteData();
        MemoryBank.debug("NoteComponent.clear, calling NoteData.clear!"); // scr0050 troubleshooting.
        if (nd != null) nd.clear(); // This can possibly affect groupDataVector.

        // Clear the (base) Component - ie, the noteTextField
        noteTextField.clear();

        // Notify the Manager
        myManager.setGroupChanged(true);  // Ensure a group 'save'

        // Reset our own state and prepare this component to be reused -
        initialized = false;
    } // end clear


    // Do not let this component grow to fill the available space in the container.
    public Dimension getMaximumSize() {
        Dimension d = super.getMaximumSize();
        return new Dimension(d.width, NOTEHEIGHT);
    } // end getMaximumSize


    //-----------------------------------------------------------------
    // Method Name: getNoteData
    //
    // Returns the data object that this component encapsulates and manages.
    //-----------------------------------------------------------------
    NoteData getNoteData() {
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

        if (!tmpNoteData.getNoteString().trim().equals("")) {
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
    // Called (by keyTyped) when a note first has data entered into it by the user.
    //
    // 'initialized' means that a NoteComponent has had text entered into
    //    its noteTextField.  This flag helps to reduce the work of the
    //    keyTyped handler.
    //
    // In addition to setting this critical flag, this method (usually) enables the
    //   next NoteComponent in the group to accept text entry.
    //---------------------------------------------------------------------
    protected void initialize() {
        initialized = true;
        if (index >= myManager.getLastVisibleNoteIndex()) {
            myManager.activateNextNote(index);
        }
    } // end initialize


    //--------------------------------------------------------------
    // Method Name: makeDataObject
    //
    // Each child of this class (that manages a child of the
    //   NoteData class) should override this method and
    //   instantiate their own data type.  Those overrides
    //   should not call this 'super' method.
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
        if(!initialized) return; // No need for this, on notes where nothing was ever done.
        if (!blnIAmOn) {
                if (noteTextField.getText().trim().equals("")) {
                    NoteData nd = getNoteData();

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
    } // end resetComponent


    void resetPanelStatusMessage(int textStatus) {
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
        myManager.setStatusMessage(s);
    } // end resetPanelStatusMessage


    // This method is called each time before displaying the popup menu.
    //   Child classes may override it if they have additional selections,
    //   but they can still call this one first, to add the base items.
    void resetPopup() {
        contextMenu.removeAll();
        contextMenu.add(miCutLine);   // the default state is 'enabled'.
        contextMenu.add(miCopyLine);
        contextMenu.add(miPasteLine);
        contextMenu.add(miClearLine);

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
        }
    } // end resetPopup

    // Called by NoteGroup when shifting up/down.
    public void setActive() {
        noteTextField.requestFocusInWindow();
    } // end NoteComponent setActive

    void setEditable(boolean b) {
        editable = b;
        noteTextField.setEditable(editable);
    }

    public void setNoteChanged() {
        myManager.setGroupChanged(true);
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
        myManager.shiftDown(index);
    } // end shiftDown

    protected void shiftUp() {
        myManager.shiftUp(index);
    } // end shiftUp


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

        myManager.setGroupChanged(true);
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
            // This is needed so that the KeyListener will hear a TAB.
            setFocusTraversalKeysEnabled(false);

            setBorder(offBorder);
            addMouseListener(this);
            setFont(Font.decode("Dialog-bold-14"));
            addActionListener(this);  // The Enter key, and a mouse double-click.
            addFocusListener(this);
            getDocument().addDocumentListener(this); // cut/paste/changed
            addKeyListener(this);
        } // end constructor

        private void clear() {
            // Remove the document listener, to avoid thread deadlocks.
            getDocument().removeDocumentListener(this);

            // Clear the text field
            setText(null);
            setForeground(Color.black);
            setToolTipText(null);

            // Restore the document listener.
            getDocument().addDocumentListener(this);
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

        // This provides a gap between the bounds of the NoteComponent and the location of its tooltip,
        //   if it has one.  It is just low enough (by a few pixels) that we go thru 'mouseExited' if we try to
        //   move the pointer into the tooltip text area, and that causes the tooltip to go away.
        // Important:  Probably not hardened in the face of different L&Fs; optimized for Windows Classic.
        // Also: a 'fast' mouse move from within the bounds of the NoteComponent into the popped-up tooltip
        //    can evade the mouseExited event as the cursor crosses the 6-pixel gap, in which case the
        //    tooltip stays up much longer.  But we can live with that.
        @Override
        public Point getToolTipLocation(MouseEvent e) {
            int offsetY = 30; // Good enough for most components.
            Object object = e.getSource();
            try {
                JComponent component = (JComponent) object;
                //Rectangle rectangle = component.getBounds();
                offsetY = component.getBounds().height + 6;
            } catch (Exception ignore){}

            return new Point(10, offsetY);
        }


        // Turn off the currently displayed tooltip, if there is one.
        void hideToolTip() {
            // We preserve a single (static) last MOUSE_ENTERED event across ALL NoteTextFields, but there is a
            // sequence of operations after program startup, whereby execution could come here while this reference
            // is still null.  So the following  block of code is conditional on it not being null.
            if(lastMouseEnteredEvent != null) {
                // Get a reference to the last entered note
                NoteTextField theSource = (NoteTextField) lastMouseEnteredEvent.getSource();
                // and use the reference along with the MOUSE_ENTERED event, to gen up a 'MOUSE_EXITED' event.
                MouseEvent mouseExitedEvent = new MouseEvent(theSource, MouseEvent.MOUSE_EXITED,
                        lastMouseEnteredEvent.getWhen(), lastMouseEnteredEvent.getModifiersEx(),
                        -1, -1, lastMouseEnteredEvent.getClickCount(), false);

                // Now get ALL the mouse listeners on that earlier note.
                // This is because tooltips are displayed by the JVM library code and not our own, so if there
                //   is a tooltip showing then it was put there by a listener in that code, so that is the listener
                //   to which we need to send the event to get the tooltip to go away.
                MouseListener[] theListeners = theSource.getMouseListeners();

                // Cycle thru the listeners and call .mouseExited() on all of them.
                // (including our own, but that one will not remove the tooltip).
                for(MouseListener ml: theListeners) {
                    ml.mouseExited(mouseExitedEvent);
                }
            }
        }


        private void resetToolTip(NoteData nd) {
            if (nd == null) return;

            String subjectString = nd.getSubjectString();
            String extendedNoteString = nd.getExtendedNoteString().trim();
            String strToolTip;

            if (subjectString != null) {
                // System.out.println("Setting the tool tip to: " + ss);
                if (subjectString.trim().equals("")) subjectString = null;
            } // end if

            // The tool tip will be a concatenation of the subject
            //   and the extended note.  If one is not present then
            //   it will just be the other.  If neither, then null.
            if ((subjectString != null) && !extendedNoteString.equals("")) {
                strToolTip = subjectString + System.lineSeparator() + extendedNoteString;
            } else if (subjectString != null) {
                strToolTip = subjectString;
            } else if (!extendedNoteString.equals("")) {
                strToolTip = extendedNoteString;
            } else {
                strToolTip = null;
            } // end if / else - setting strToolTip

            if (strToolTip != null) {
                // Insert line breaks as needed and enforce
                //   an overall text length limit.
                strToolTip = AppUtil.getTooltipString(strToolTip);

                // In case we had a (too large) gap of linefeeds in the middle
                //   and the cutoff didn't make it back to real text -
                strToolTip = strToolTip.trim();

                // Convert potentially malicious characters.
                strToolTip = strToolTip.replace("&", "&amp;");
                strToolTip = strToolTip.replace("<", "&lt;");

                // Wrap in HTML and PREserve the original formatting, to hold on to indents and multi-line.
                strToolTip = "<html><pre>" + strToolTip + "</pre></html>";

            } // end if

            setToolTipText(strToolTip);
        } // end resetToolTip

        @Override
        public void setText(String s) {
            super.setText(s);
            setCaretPosition(0); // Do not leave notes scrolled horizontally.
        }

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

        //<editor-fold desc="actionPerformed method for the TextField">
        // Although there are several child classes, only this base class is handling events on the JTextField.
        //   This method will be called directly as the event handler when the field has the focus and the user
        //   presses 'Enter', and indirectly when they mouse double-click on the field.
        //---------------------------------------------------------
        public void actionPerformed(ActionEvent ae) {
            MemoryBank.event();
            boolean extendedNoteChanged;
            if (!this.isEditable()) return;
            if (!initialized) return;
            hideToolTip(); // Turn off the currently displayed tooltip, if any.

            // Highlight this note to show it is the one being modified.
            NoteComponent.this.setBorder(redBorder);

            NoteData tmpNoteData = getNoteData();
            extendedNoteChanged = myManager.editExtendedNoteComponent(tmpNoteData);

            if (extendedNoteChanged) {
                // Set (or clear) the tool tip.
                resetToolTip(tmpNoteData);

                setTextColor();
                setNoteChanged();
            } // end if

            // Remove the 'modification in progress' highlight
            NoteComponent.this.setBorder(null);
        } // end actionPerformed
        //</editor-fold>


        //<editor-fold desc="DocumentListener methods for the TextField">
        public void insertUpdate(DocumentEvent e) {
            // System.out.println("insertUpdate: " + e.toString());
            if (!initialized) initialize();

            getNoteData().setNoteString(getText());
            setNoteChanged();
        } // end insertUpdate

        public void removeUpdate(DocumentEvent e) {
            System.out.println("removeUpdate: " + e.toString());
            getNoteData().setNoteString(getText());
            setNoteChanged();
        } // end removeUpdate

        public void changedUpdate(DocumentEvent e) {
            System.out.println("changedUpdate: " + e.toString());
            getNoteData().setNoteString(getText());
            setNoteChanged();
        } // end changedUpdate
        //</editor-fold>


        //<editor-fold desc="FocusListener methods for the TextField">
        // Note: The order of focusGained / focusLost along with
        //  the visual indicators (borders, highlighting) and
        //  how they can be invoked by either up/down arrows,
        //  mouse clicks, or the tab key - is critical!
        //  The key to it all is to disallow other components from
        //  getting the focus when they appear (most notably, the
        //  PopupMenu and the vertical scrollbar).  Then, do
        //  everything based on Focus here being gained or lost.
        public void focusGained(FocusEvent e) {
            // System.out.println("focusGained for index " + index);
            setBorder(redBorder);
            NoteComponent.this.scrollRectToVisible(getBounds());  // Does this scroll text on the line, or the line in the scrollpane?
            if(mySelectionMonitor != null) mySelectionMonitor.noteSelected(getNoteData());

            // We occasionally get a null pointer exception at startup.
            if (getCaret() == null) return;
// trying to disable the pre-highlighted text seen in todo lists.  Need to consistently reproduce, first.
//            setSelectionStart(getSelectionEnd());
            getCaret().setVisible(true);

            if (!initialized) return;
            noteActivated(true);
        } // end focusGained

        public void focusLost(FocusEvent e) {
            // System.out.println("focusLost for index " + index);
            setBorder(offBorder);
            getCaret().setVisible(false);
            // We do not de-select at this point because any selection would be lost
            // when the user clicks 'ok', for instance.
            // Instead, selections are cleared prior to presenting new choices.

            noteActivated(false);
        } // end focusLost
        //</editor-fold>


        //<editor-fold desc="KeyListener methods for the TextField">
        @Override
        public void keyPressed(KeyEvent ke) {
            // Turn off a previous popup, if one is showing.
            // This could happen if a menu was showing, then the user
            //   pressed a TAB, UP or DOWN key to change focus - the
            //   popup menu would still be up, but active for the
            //   previous note vs the one that appeared to be active.
            if (contextMenu.isVisible()) contextMenu.setVisible(false);

            hideToolTip(); // Turn off the currently displayed tooltip, if any.

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
            initialize(); // this will activate the next note
            noteActivated(true); // Added for SCR0091
        } // end keyTyped
        //</editor-fold>


        //<editor-fold desc="MouseListener methods for the TextField">
        public void mouseClicked(MouseEvent e) {
            MemoryBank.event();
            if (!this.hasFocus()) {
                // The rmb click does not change focus, so we help it out.
                requestFocusInWindow();
            }

            if (!this.isEditable()) return;
            int m = e.getModifiersEx();

            // Single click, right mouse button.
            if(e.getButton()==MouseEvent.BUTTON3) {
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

                // Ignore double right mouse clicks.
                if (e.getClickCount() == 2) return;

                // Set the global NoteComponent.  This is the primary
                // mechanism that allows the handler to be static.
                theNoteComponent = NoteComponent.this;

                // Child classes will override resetPopup to enable/disable, setNotes/remove
                //   menu items, based on the data content of the active NoteComponent.
                resetPopup();

                // Show the popup menu
                // System.out.println("Showing popup!");
                contextMenu.show(e.getComponent(), e.getX(), e.getY());

                // Double click, left mouse button.
            } else if (e.getClickCount() == 2) {
                actionPerformed(new ActionEvent(e.getSource(), 0, ""));
            } // end if/else if
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
            contextMenu.setVisible(false); // This gets rid of a previous one, if any.
            lastMouseEnteredEvent = e;
            if (!initialized) return;

            resetPanelStatusMessage(getTextStatus());
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {
            myManager.setStatusMessage(" ");
        }

        public void mousePressed(MouseEvent e) {
        }

        public void mouseReleased(MouseEvent e) {
        }
        //</editor-fold>

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
            if (theNoteComponent == null) return; // Since this is a static method, we cannot use 'this'.

            JMenuItem jm = (JMenuItem) e.getSource();
            NoteData noteData = theNoteComponent.getNoteData(); // Not used in every case, below.
            String theMenuItemText = jm.getText();
            switch (theMenuItemText) {
                case "Cut Line":
                    MemoryBank.clipboardNote = noteData.copy();  // isolate source data
                    theNoteComponent.clear();
                    theNoteComponent.setNoteChanged();
                    break;
                case "Copy Line":
                    MemoryBank.clipboardNote = noteData.copy();  // isolate source data
                    break;
                case "Paste Line":
                    theNoteComponent.initialize();
                    // Pasting a copy allows us to do a paste multiple times without re-copying.

                    // But we need to use the right data type for the component; a copy/paste could cross over data types.
                    if(theNoteComponent.getNoteData().getClass() == MemoryBank.clipboardNote.getClass()) {
                        // If they are the same type then we can take a full copy.
                        theNoteComponent.setNoteData(MemoryBank.clipboardNote.copy());
                    } else {
                        // Otherwise we can only take the elements of the base class.
                        theNoteComponent.setNoteData(new NoteData(MemoryBank.clipboardNote));
                    }
                    theNoteComponent.setNoteChanged();
                    break;
                case "Clear Line":
                    theNoteComponent.clear();
                    theNoteComponent.setNoteChanged();
                    break;
                default:
                    System.out.println(theMenuItemText);
                    break;
            }
        } // end actionPerformed
    } // end class PopHandler

} // end class NoteComponent


