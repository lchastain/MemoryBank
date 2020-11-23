import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.time.LocalDate;

public class MilestoneComponent extends NoteComponent {
    private static final long serialVersionUID = 1L;

    // The Members
    private TodoNoteData myTodoNoteData;
    private final StatusButton sbTheStatusButton;

    private GoalGroupPanel myNoteGroup;
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
        miClearPriority = contextMenu.add("Clear Priority");
        miClearPriority.addActionListener(popHandler);

        miMoveToToday = contextMenu.add("Move To Today");
        miMoveToToday.addActionListener(popHandler);

        miMoveToSelectedDate = contextMenu.add("Move To Selected Date");
        miMoveToSelectedDate.addActionListener(popHandler);

        JMenuItem miCopyToAnotherList = contextMenu.add("Copy To Another List...");
        miCopyToAnotherList.addActionListener(popHandler);

        JMenuItem miMoveToAnotherList = contextMenu.add("Move To Another List...");
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


    MilestoneComponent(GoalGroupPanel ng, int i) {
        super(ng, i);
        removeAll();   // We will redo the base layout.
        setLayout(new DndLayout());

        index = i;
        myNoteGroup = ng;

        sbTheStatusButton = new StatusButton();

        //----------------------------------------------------------
        // Graphical elements
        //----------------------------------------------------------
        // Note: The dndLayout does not care about any name other
        //   than 'Stretch', but some unique text must be provided
        //   for each component that is added.  Only one component
        //   in the layout can be the one to be stretched.
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
        if (sbTheStatusButton != null) sbTheStatusButton.clear();

        // Remove any selection from the Three Month Column.
        myNoteGroup.getThreeMonthColumn().setChoice(null);

        super.clear(); // This also sets the component 'initialized' to false.
        // And it leaves a 'gap' but we like that so no refresh here.
    } // end clear


    static String getIconFilename(int status) {
        char c = File.separatorChar;
        String iString = MemoryBank.logHome + c + "icons" + c;

        switch (status) {
            case TodoNoteData.TODO_COMPLETED:
                iString += "button06_yes.gif";
                break;
            case TodoNoteData.TODO_INPROG:
                iString += "constru3.gif";
                break;
            case TodoNoteData.TODO_WAITING:
                iString += "watch1b.gif";
                break;
            case TodoNoteData.TODO_QUERY:
                iString += "button06_query.gif";
                break;
            case TodoNoteData.TODO_OBE:
                iString += "button06_no.gif";
                break;
            case TodoNoteData.TODO_STARTED:
            default:
                iString = null;
                break;
        } // end switch
        return iString;
    } // end getIconFilename


    @Override
    NoteData getNoteData() {
        return myTodoNoteData;
    } // end getNoteData

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

    // Move the TodoNoteData to a Day Note.  This happens with Events as well,
    //   but here it is done at the individual Component level whereas Events
    //   are aged as a group and more than one might be affected with different
    //   effects on the visible interface, so the group 'refresh' is needed there
    //   and it is not possible to leave a gap because the entire list gets reloaded.
    //   But here - we just leave a gap.
    private void moveToDayNote(boolean useSelectedDate) {
        // TodoNoteData items have a 'slot' for a Subject, but no UI to set one.  So
        // now as it goes over to a DayNote, the Subject will be the name of the list
        // from which this item is being removed.
        myTodoNoteData.setSubjectString(myNoteGroupPanel.myNoteGroup.myGroupInfo.getGroupName());

        // Get the Date to which we will move this item - either one has been selected AND we want to use it,
        //  or there is always 'today'.
        LocalDate moveToDate;
        if (useSelectedDate) {
            moveToDate = myTodoNoteData.getTodoDate();
            MemoryBank.debug("Moving TodoNote to specified date: " + moveToDate.toString());
        } else {
            LocalDate today = LocalDate.now();
            MemoryBank.debug("Moving TodoNote to Today: " + today.toString());
            myTodoNoteData.setTodoDate(LocalDate.now());
        }

        // Convert the item to a DayNoteData, using the TodoNoteData-flavored constructor.
        DayNoteData dnd = new DayNoteData(myTodoNoteData);

        // Use the date to get the right group, then add the note to it.
        LocalDate theTodoDate = myTodoNoteData.getTodoDate();
        String groupName = CalendarNoteGroup.getGroupNameForDate(theTodoDate, GroupType.DAY_NOTES);
        NoteGroup theGroup = new GroupInfo(groupName, GroupType.DAY_NOTES).getNoteGroup();
        theGroup.addNote(dnd);

        // Save the updated DayNoteGroup and clear our note line.
        theGroup.saveNoteGroup();
        clear();  // This creates a 'gap'.
    } // end moveToDayNote


    // Do not call this method if the columns are already in order;
    //   it just wastes cpu cycles.  Test for that condition in the
    //   calling context and only make the call if needed.
    public void resetColumnOrder(int theOrder) {
        String pos = String.valueOf(theOrder);
        // System.out.println("TodoNoteComponent resetColumnOrder to " + pos);

        //   Note that now we do not provide the 'name' and so we will
        //   be going through the base layout class 'setNotes' method.
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
    @Override
    protected void resetComponent() {
        sbTheStatusButton.setStatus(myTodoNoteData.getStatus());
        super.resetComponent(); // the note text
    } // end resetComponent


    @Override
    void resetPanelStatusMessage(int textStatus) {
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
        myNoteGroup.setStatusMessage(s);
    } // end resetPanelStatusMessage


    // This method enables/disables the popup menu items.
    @Override
    protected void resetPopup() {
        super.resetPopup(); // Needed, but it removes our non-base class items.
        contextMenu.add(miClearPriority); // so we put them back.
        contextMenu.add(miMoveToToday);
        contextMenu.add(miMoveToSelectedDate);

        if (!initialized) {
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


    @Override
    void setEditable(boolean b) {
        super.setEditable(b);
        sbTheStatusButton.setEditable(b);
    }


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
        //   to data objects.  If you 'get' data from the NoteComponent
        //   into a local variable and then later clear the component, you have
        //   also just cleared the data in your local variable because you never
        //   had a separatate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (tnd1 != null) tnd1 = new TodoNoteData(tnd1);
        if (tnd2 != null) tnd2 = new TodoNoteData(tnd2);

        if (tnd1 == null) tnc.clear();
        else tnc.setNoteData(tnd1);

        if (tnd2 == null) this.clear();
        else this.setTodoNoteData(tnd2);

        myNoteGroup.setGroupChanged(true);
    } // end swap

    //---------------------------------------------------------
    // End of NoteComponent specific methods
    //---------------------------------------------------------

    //---------------------------------------------------------
    // Inner Class -
    //---------------------------------------------------------
    protected class StatusButton extends LabelButton implements MouseListener {
        private static final long serialVersionUID = 1L;

        public static final int minWidth = 40;

        private int theStatus;
        private int theOriginalStatus;

        public StatusButton() {
            super();

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


        void setEditable(boolean b) {
            if(b) addMouseListener(this);
            else removeMouseListener(this);
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
                myNoteGroup.setStatusMessage(s);
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
            myNoteGroup.setStatusMessage(myTodoNoteData.getStatusString());
        } // end mouseClicked


        public void mouseEntered(MouseEvent e) {
            if (!initialized) return;

            // System.out.println(e);
            if (!myTodoNoteData.hasText()) return;
            myNoteGroup.setStatusMessage(myTodoNoteData.getStatusString());
            theOriginalStatus = myTodoNoteData.getStatus();
        } // end mouseEntered


        public void mouseExited(MouseEvent e) {
            // System.out.println(e);
            myNoteGroup.setStatusMessage(" ");

            if (!initialized) return;

            // Update the source data, if applicable.
            //  This allows several 'clicks' to occur but no required list save,
            //  as long as the user leaves the status where they found it.
            if (theOriginalStatus != myTodoNoteData.getStatus()) {
                myNoteGroup.setGroupChanged(true);
            } // end if
        } // end mouseExited

        public void mousePressed(MouseEvent e) {
            setActive();
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
        }
    } // end class StatusButton


    // A static handler for popup menu items
    //------------------------------------------------------------
    private static class PopHandler implements ActionListener {
        MilestoneComponent tnc;

        public void actionPerformed(ActionEvent e) {
            if (theNoteComponent == null) return;

            tnc = (MilestoneComponent) theNoteComponent;
            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            // System.out.println(s);
            switch (s) {
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
            tnc.myNoteGroup.setGroupChanged(true);
        } // end actionPerformed
    } // end class PopHandler

} // end class TodoNoteComponent


//Embedded Data class
//-------------------------------------------------------------------



