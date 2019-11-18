import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.time.LocalDate;

public class TodoNoteComponent extends NoteComponent {
    private static final long serialVersionUID = 1L;

    // The Members
    private TodoNoteData myTodoNoteData;
    private PriorityButton pbThePriorityButton;
    private StatusButton sbTheStatusButton;

    private TodoNoteGroup myNoteGroup;
    private static JMenuItem miClearPriority;
    private static JMenuItem miMoveToToday;
    private static JMenuItem miMoveToSelectedDate;
    private static ImageIcon todo_done;
    private static ImageIcon todo_inprog;
    private static ImageIcon todo_wait;
    private static ImageIcon todo_query;
    private static ImageIcon todo_obe;

    static {
        //-----------------------------------
        // Add to the base class popup menu.
        //-----------------------------------
        PopHandler popHandler = new PopHandler();
        miClearPriority = popup.add("Clear Priority");
        miClearPriority.addActionListener(popHandler);

        miMoveToToday = popup.add("Move To Today");
        miMoveToToday.addActionListener(popHandler);

        miMoveToSelectedDate = popup.add("Move To Selected Date");
        miMoveToSelectedDate.addActionListener(popHandler);

        JMenuItem miCopyToAnotherList = popup.add("Copy To Another List...");
        miCopyToAnotherList.addActionListener(popHandler);

        JMenuItem miMoveToAnotherList = popup.add("Move To Another List...");
        miMoveToAnotherList.addActionListener(popHandler);

        //------------------------
        // Make the status icons
        //------------------------
        Image tmp;
        tmp = new ImageIcon(getIconFilename(1)).getImage();
        tmp = tmp.getScaledInstance(24, 20, Image.SCALE_SMOOTH);
        todo_done = new ImageIcon(tmp);

        tmp = new ImageIcon(getIconFilename(2)).getImage();
        tmp = tmp.getScaledInstance(24, 20, Image.SCALE_SMOOTH);
        todo_inprog = new ImageIcon(tmp);

        tmp = new ImageIcon(getIconFilename(3)).getImage();
        tmp = tmp.getScaledInstance(24, 20, Image.SCALE_SMOOTH);
        todo_wait = new ImageIcon(tmp);

        tmp = new ImageIcon(getIconFilename(4)).getImage();
        tmp = tmp.getScaledInstance(24, 20, Image.SCALE_SMOOTH);
        todo_query = new ImageIcon(tmp);

        tmp = new ImageIcon(getIconFilename(5)).getImage();
        tmp = tmp.getScaledInstance(24, 20, Image.SCALE_SMOOTH);
        todo_obe = new ImageIcon(tmp);

    } // end static section


    TodoNoteComponent(TodoNoteGroup ng, int i) {
        super(ng, i);
        setLayout(new DndLayout());

        index = i;
        myNoteGroup = ng;

        pbThePriorityButton = new PriorityButton();
        sbTheStatusButton = new StatusButton();

        //----------------------------------------------------------
        // Graphical elements
        //----------------------------------------------------------
        // Note: The dndLayout does not care about any name other
        //   than 'Stretch', but something must be provided.  Only
        //   one component can be the one to be stretched.
        add(pbThePriorityButton, "pb");
        add(noteTextField, "Stretch"); // will resize along with container
        add(sbTheStatusButton, "sb");

        MemoryBank.trace();
    } // end constructor


    //-----------------------------------------------------------------
    // Method Name: clear
    //
    // Clears both the Graphical elements and the underlying data.
    //-----------------------------------------------------------------
    @Override
    protected void clear() {
        // We need to clear out our own members before clearing the base component.
        if (pbThePriorityButton != null) pbThePriorityButton.clear();
        if (sbTheStatusButton != null) sbTheStatusButton.clear();

        // Clear the base component and its underlying data.
        super.clear(); // This also sets the component 'initialized' to false.
        // And it leaves a 'gap' but we like that so no refresh here but
        // we do want to remove any selection from the Three Month Column.
        myNoteGroup.getThreeMonthColumn().setChoice(null);
    } // end clear


    static String getIconFilename(int i) {
        char c = File.separatorChar;
        String iString = MemoryBank.logHome + c + "images" + c;

        switch (i) {
            case 1:
                iString += "button06_yes.gif";
                break;
            case 2:
                iString += "constru3.gif";
                break;
            case 3:
                iString += "watch1b.gif";
                break;
            case 4:
                iString += "button06_query.gif";
                break;
            case 5:
                iString += "button06_no.gif";
                break;
            default:
                iString = null;
                break;
        } // end switch
        return iString;
    } // end getIconFilename


    @Override
    public NoteData getNoteData() {
        return myTodoNoteData;
    } // end getNoteData

    PriorityButton getPriorityButton() {
        return pbThePriorityButton;
    }

    StatusButton getStatusButton() {
        return sbTheStatusButton;
    }

    @Override
    protected void makeDataObject() {
        myTodoNoteData = new TodoNoteData();
    } // end


    @Override
    protected void noteActivated(boolean blnIAmOn) {
        myNoteGroup.showComponent(this, blnIAmOn);
        super.noteActivated(blnIAmOn);
    }

    //--------------------------------------------------------------------------
    // Method Name: moveToDayNote
    //
    // Move the TodoNoteData to a Day Note.  This happens with Events,
    //   as well, but here it is done
    //   at the individual Component level, whereas Events are aged as a group
    //   and more than one might be affected with different effects on the
    //   visible interface, so the group 'refresh' is needed there and it
    //   is not possible to leave a gap because the entire list gets reloaded.
    //   But here - we just leave a gap (but that might change eventually, when
    //   we get a 'todolist refresh' feature).
    //--------------------------------------------------------------------------
    private void moveToDayNote(boolean useDate) {
        MemoryBank.debug("Moving...");
        MemoryBank.debug("  To Date = " + useDate);

        if (!useDate) myTodoNoteData.setTodoDate(LocalDate.now());

        boolean success;
        String s = NoteGroup.prettyName(myNoteGroup.getGroupFilename());
        myTodoNoteData.setSubjectString(s);

        // Prepare to preserve the item, then do so by calling addNote.
//        DayNoteData dnd = myTodoNoteData.getDayNoteData();
        DayNoteData dnd = new DayNoteData(myTodoNoteData);
        String theFilename;
        LocalDate theTodoDate = myTodoNoteData.getTodoDate();
        theFilename = AppUtil.findFilename(theTodoDate, "D");
        if (theFilename.equals("")) {
            theFilename = AppUtil.makeFilename(theTodoDate, "D");
        } // end if
        success = AppUtil.addNote(theFilename, dnd);

        if (success) {
            MemoryBank.debug("Move succeeded");
            // We don't know if this day is already showing, or not.
            // So the 'note added' flag is set, so if/when the tree view
            // switches to DayNotes, the current date choice will be (re-)loaded,
            // whether or not that was the day to which we just added a note.
            // But for the data - AppUtil.addNote is what added it to the file.
            DayNoteGroup.blnNoteAdded = true;
            clear();  // This creates a 'gap'.
        } else {
            MemoryBank.debug("Move failed");
            s = "Cannot preserve this item.\n";
            s += "Review the error stream for more info.";
            JOptionPane.showMessageDialog(this, s,
                    "Error", JOptionPane.ERROR_MESSAGE);
        } // end if
    } // end moveToDayNote


    //----------------------------------------------------------------
    // Method Name: resetColumnOrder
    //
    // Do not call this method if the columns are already in order;
    //   it just wastes cpu cycles.  Test for that condition in the
    //   calling context and only make the call if needed.
    //----------------------------------------------------------------
    public void resetColumnOrder(int theOrder) {
        String pos = String.valueOf(theOrder);
        // System.out.println("TodoNoteComponent resetColumnOrder to " + pos);

        //   Note that now we do not provide the 'name' and so we will
        //   be going through the base layout class 'add' method.
        add(pbThePriorityButton, pos.indexOf("1"));
        add(noteTextField, pos.indexOf("2"));
        add(sbTheStatusButton, pos.indexOf("3"));

        // This was needed after paging was implemented.
        noteTextField.transferFocusUpCycle();  // new 3/19/2008
    } // end resetColumnOrder


    //----------------------------------------------------------
    // Method Name: resetComponent
    //
    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change.
    //----------------------------------------------------------
    protected void resetComponent() {
        pbThePriorityButton.setPriority(myTodoNoteData.getPriority());
        sbTheStatusButton.setStatus(myTodoNoteData.getStatus());
        super.resetComponent(); // the note text
    } // end resetComponent


    protected void resetMouseMessage(int textStatus) {
        String s = " ";

        switch (textStatus) {
            case NEEDS_TEXT:
                s = "Click here to enter text for this task.";
                break;
            case HAS_BASE_TEXT:
                s = "Double-click here to add details about this task.";
                break;
            case HAS_EXT_TEXT:
                // This gives away the 'hidden' text, if
                //   there is no primary (blue) text.
                s = "Double-click here to see/edit";
                s += " the additional details for this task.";
        } // end switch
        myNoteGroup.setMessage(s);
    } // end resetMouseMessage


    // This method enables/disables the popup menu items.
    @Override
    protected void resetPopup() {
        super.resetPopup(); // Needed, but it removes our non-base class items.
        popup.add(miClearPriority); // so we put them back.
        popup.add(miMoveToToday);
        popup.add(miMoveToSelectedDate);

        if(!initialized) {
            miClearPriority.setEnabled(false);
            miMoveToToday.setEnabled(false);
            miMoveToSelectedDate.setEnabled(false);
            return;
        }

        if (myTodoNoteData.hasText()) {
            miClearPriority.setEnabled(myTodoNoteData.getPriority() > 0);
            miMoveToToday.setEnabled(true);
            miMoveToSelectedDate.setEnabled(myTodoNoteData.getTodoDate() != null);
        } else {
            // Find out how this happens, or confirm that it does not.
            throw new AssertionError();  // Added 8/30/2019 - remove, after this has not occurred for some time.
        } // end if
    } // end resetPopup


    public void resetVisibility() {
        pbThePriorityButton.setVisible(myNoteGroup.myVars.showPriority);
    } // end resetVisibility


    @Override
    public void setNoteData(NoteData newNoteData) {
        if (newNoteData instanceof TodoNoteData) {  // same type, but cast is still needed
            setTodoNoteData((TodoNoteData) newNoteData);
        } else { // Not 'my' type, but we can make it so.
            setTodoNoteData(new TodoNoteData(newNoteData));
        }
    } // end setNoteData


    void setTodoNoteData(TodoNoteData newNoteData) {
        myTodoNoteData = newNoteData;

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetComponent();
        setNoteChanged();
    } // end setTodoNoteData


    // Exchange data content between this component and the input parameter.
    @Override
    public void swap(NoteComponent tnc) {
        // Get a reference to the two data objects
        TodoNoteData tnd1 = (TodoNoteData) this.getNoteData();
        TodoNoteData tnd2 = (TodoNoteData) tnc.getNoteData();

        // Note: getNoteData and setNoteData are working with references
        //   to data objects.  If you 'get' data into a local variable
        //   and then later clear the component, you have also just
        //   cleared the data in your local variable because you never had
        //   a separatate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (tnd1 != null) tnd1 = new TodoNoteData(tnd1);
        if (tnd2 != null) tnd2 = new TodoNoteData(tnd2);

        if (tnd1 == null) tnc.clear();
        else tnc.setNoteData(tnd1);

        if (tnd2 == null) this.clear();
        else this.setTodoNoteData(tnd2);

        myNoteGroup.setGroupChanged();
    } // end swap

    //---------------------------------------------------------
    // End of NoteComponent specific methods
    //---------------------------------------------------------

    //---------------------------------------------------------
    // Inner Classes -
    //---------------------------------------------------------
    protected class PriorityButton extends JButton implements MouseListener {
        private static final long serialVersionUID = 3476852731589070975L;

        public static final int minWidth = 48;
        private int Priority;

        // Note: These two variables are used to obtain a finer granularity
        //   of mouse events than reported by the JVM - mouseReleased should
        //   not have an effect if there was a mouseExited.  getClickCount still
        //   reports a '1' in that case, so we use these vars to distinguish
        //   between a precise click (priority +/-) and a sliding click (ignored).
        boolean leftClicked = false;
        boolean rightClicked = false;

        public PriorityButton() {
            super("  ");

            setFont(Font.decode("Monospaced-bold-12"));
            addMouseListener(this);
            disableEvents(AWTEvent.FOCUS_EVENT_MASK + AWTEvent.COMPONENT_EVENT_MASK
                    + AWTEvent.ACTION_EVENT_MASK + AWTEvent.MOUSE_MOTION_EVENT_MASK
                    + AWTEvent.ITEM_EVENT_MASK);
        } // end PriorityButton constructor

        public void clear() {
            setPriority(0);
        }

        public int getPriority() {
            return Priority;
        }

        void setPriority(int value) {
            if(!initialized) return;
            if (value < 0) return;
            if (value > myNoteGroup.getMaxPriority()) return;
            Priority = value;
            if (Priority == 0) setText("  ");
            else setText(String.valueOf(Priority));
            myTodoNoteData.setPriority(Priority);

        } // end setPriority

        //----------------------------------------------
        // Overridden AWT methods
        //----------------------------------------------
        public boolean isFocusPainted() {
            return false;
        }

        public boolean isFocusable() {
            return false;
        }

        public Dimension getMaximumSize() {
            return getPreferredSize();
        }

        public Dimension getMinimumSize() {
            return getPreferredSize();
        }

        public Dimension getPreferredSize() {
            if (!isVisible()) return new Dimension(0, 0);
            Dimension d = super.getPreferredSize();

            if (d.width < minWidth) {
                d.width = minWidth;
            } // end if
            return d;
        } // end getPreferredSize

        // Override these, to disable the 'depressed' color change.
        public void setBackground() {
        }

        public void setForeground() {
        }

        //---------------------------------------------------------
        // FocusListener methods
        //---------------------------------------------------------
        public void focusGained() {
            transferFocus();
        }

        public void focusLost() {
        }

        //---------------------------------------------------------
        // MouseListener methods
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            // System.out.println(e);
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
            if (!initialized) return;

            // System.out.println(e);
            if (!myTodoNoteData.hasText()) return;

            String s;
            if (Priority == 0) {
                s = "Left mouse click to increase priority";
            } else {
                s = "Right mouse click to decrease priority";
            } // end if
            myNoteGroup.setMessage(s);
        }

        public void mouseExited(MouseEvent e) {
            // System.out.println(e);
            rightClicked = false;
            leftClicked = false;
            myNoteGroup.setMessage(" ");
        } // end mouseExited

        public void mousePressed(MouseEvent e) {
            setActive();

            // System.out.println(e);

            if (e.isMetaDown()) {
                rightClicked = true;
                leftClicked = false;
            } else {
                leftClicked = true;
                rightClicked = false;
            } // end if
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
            // System.out.println(e);
            if (!(leftClicked || rightClicked)) return;

            if (!initialized) {
                String s;
                s = "An item must have text before a priority can be set!";
                myNoteGroup.setMessage(s);
                return;
            } // end if

            if (leftClicked) {
                if (Priority < myNoteGroup.getMaxPriority()) Priority++;
                else Priority = 0;
                leftClicked = false;
            } else {
                if (Priority > 0) Priority--;
                else Priority = myNoteGroup.getMaxPriority();
                rightClicked = false;
            } // end if
            myNoteGroup.setGroupChanged();
            if (Priority == 0) setText(" ");
            else setText(String.valueOf(Priority));
            myTodoNoteData.setPriority(Priority);
        } // end mouseReleased
        //---------------------------------------------------------

    } // end class PriorityButton


    protected class StatusButton extends LabelButton implements MouseListener {
        private static final long serialVersionUID = -6742084936189173516L;

        public static final int minWidth = 40;

        private int theStatus;
        private int theOriginalStatus;

        public StatusButton() {
            super();
            addMouseListener(this);

            setOpaque(true);
            showStatusIcon();
        } // end StatusButton constructor

        public void clear() {
            setIcon(null);
            setStatus(TodoNoteData.TODO_STARTED); // 0
        } // end clear


        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();

            if (d.width < minWidth) {
                d.width = minWidth;
            } // end if
            return d;
        } // end getPreferredSize


        // Added this 8/28/2019 for tests, but expect it will be needed in future endeavors
        // such as goal percentage completion calculations, if not other features.
        public int getStatus() {
            return theStatus;
        }

        public void setStatus(int i) {
            if (!initialized) return;
            if (i < 0) return;
            if (i > TodoNoteData.TODO_OBE) return;

            theStatus = i;
            myTodoNoteData.setStatus(i);
            showStatusIcon();
        } // end setStatus


        void showStatusIcon() {
            switch (theStatus) {
                case TodoNoteData.TODO_STARTED:
                    setIcon(null);
                    break;
                case TodoNoteData.TODO_COMPLETED:
                    setIcon(todo_done);
                    break;
                case TodoNoteData.TODO_INPROG:
                    setIcon(todo_inprog);
                    break;
                case TodoNoteData.TODO_WAITING:
                    setIcon(todo_wait);
                    break;
                case TodoNoteData.TODO_QUERY:
                    setIcon(todo_query);
                    break;
                case TodoNoteData.TODO_OBE:
                    setIcon(todo_obe);
                    break;
            } // end switch
        } // end showStatusIcon


        //---------------------------------------------------------
        // MouseListener methods
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
            setActive();

            if (!initialized) {
                String s;
                s = "An item must have text before a status can be set!";
                myNoteGroup.setMessage(s);
                return;
            } // end if

            int i;
            if (e.isMetaDown()) {
                i = -1;
            } else {
                i = 1;
            } // end if

            // Alter the status.
            theStatus = theStatus + i;
            if (theStatus > TodoNoteData.TODO_OBE) theStatus = TodoNoteData.TODO_STARTED;
            if (theStatus < TodoNoteData.TODO_STARTED) theStatus = TodoNoteData.TODO_OBE;
            myTodoNoteData.setStatus(theStatus);

            // Now display the correct icon.
            showStatusIcon();

            //showStatusMessage();
            myNoteGroup.setMessage(myTodoNoteData.getStatusString());
        } // end mouseClicked


        public void mouseEntered(MouseEvent e) {
            if (!initialized) return;

            // System.out.println(e);
            if (!myTodoNoteData.hasText()) return;
            myNoteGroup.setMessage(myTodoNoteData.getStatusString());
            theOriginalStatus = myTodoNoteData.getStatus();
        } // end mouseEntered


        public void mouseExited(MouseEvent e) {
            // System.out.println(e);
            myNoteGroup.setMessage(" ");

            if (!initialized) return;

            // Update the source data, if applicable.
            //  This allows several 'clicks' to occur but no required list save,
            //  as long as the user leaves the status where they found it.
            if (theOriginalStatus != myTodoNoteData.getStatus()) {
                myNoteGroup.setGroupChanged();
            } // end if
        } // end mouseExited

        public void mousePressed(MouseEvent e) {
            setActive();
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
        }

        //---------------------------------------------------------
    } // end class StatusButton


    // A static handler for popup menu items
    //------------------------------------------------------------
    private static class PopHandler implements ActionListener {
        TodoNoteComponent tnc;

        public void actionPerformed(ActionEvent e) {
            if (ncTheNoteComponent == null) return;

            tnc = (TodoNoteComponent) ncTheNoteComponent;
            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            // System.out.println(s);
            switch (s) {
                case "Clear Priority":
                    tnc.pbThePriorityButton.clear();
                    break;
                case "Move To Today":
                    tnc.moveToDayNote(false);
                    break;
                case "Move To Selected Date":
                    tnc.moveToDayNote(true);
                    break;
                default:  // A way of showing new menu items with no action (yet).
                    System.out.println(s);
                    break;
            }
            tnc.myNoteGroup.setGroupChanged();
        } // end actionPerformed
    } // end class PopHandler

} // end class TodoNoteComponent


//Embedded Data class
//-------------------------------------------------------------------



