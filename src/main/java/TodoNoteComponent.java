import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.Serial;
import java.time.LocalDate;
import java.util.ArrayList;

public class TodoNoteComponent extends NoteComponent {
    // The icon in a TodoNoteComponent is simpler than the one that would be inherited from IconNoteComponent.
    // The extra overhead is not needed and so this class can extend directly from NoteComponent.
    @Serial
    private static final long serialVersionUID = 1L;

    // The Members
    private TodoNoteData myTodoNoteData; // This one has an accessor
    protected final PriorityButton pbThePriorityButton;
    protected final StatusButton sbTheStatusButton;

    protected static final JMenuItem miClearPriority;
    protected static final JMenuItem miMoveToToday;
    protected static final JMenuItem miMoveToSelectedDate;

    private static final ImageIcon todo_done;
    private static final ImageIcon todo_inprog;
    private static final ImageIcon todo_wait;
    private static final ImageIcon todo_query;
    private static final ImageIcon todo_obe;

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

        //------------------------
        // Make the status icons
        //------------------------
        IconInfo iconInfo = new IconInfo();
        iconInfo.dataArea = DataArea.APP_ICONS;
        iconInfo.iconFormat = "gif";

        iconInfo.iconName = "button06_yes";
        todo_done = iconInfo.getImageIcon();
        IconInfo.scaleIcon(todo_done, 24, 20);

        iconInfo.iconName = "constru3";
        todo_inprog = iconInfo.getImageIcon();
        IconInfo.scaleIcon(todo_inprog, 24, 20);

        iconInfo.iconName = "watch1b";
        todo_wait = iconInfo.getImageIcon();
        IconInfo.scaleIcon(todo_wait, 24, 20);

        iconInfo.iconName = "button06_query";
        todo_query = iconInfo.getImageIcon();
        IconInfo.scaleIcon(todo_query, 24, 20);

        iconInfo.iconName = "button06_no";
        todo_obe = iconInfo.getImageIcon();
        IconInfo.scaleIcon(todo_obe, 24, 20);

    } // end static section


    TodoNoteComponent(TodoNoteGroupPanel ng, int i) {
        super(ng, i);
        removeAll();   // We will redo the base layout.
        setLayout(new DndLayout());

        index = i;

        pbThePriorityButton = new PriorityButton();
        sbTheStatusButton = new StatusButton();

        //----------------------------------------------------------
        // Graphical elements
        //----------------------------------------------------------
        // Note: The dndLayout does not care about any name other
        //   than 'Stretch', but some unique text must be provided
        //   for each component that is added.  Only one component
        //   in the layout can be the one to be stretched.
        add(pbThePriorityButton, "pb");
        add(noteTextField, "Stretch"); // will resize along with container
        noteTextArea.setName("Stretch");
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

        // Remove any selection from the Three Month Column.
        ((TodoNoteGroupPanel) myNoteGroupPanel).getThreeMonthColumn().setChoice(null);

        super.clear(); // This also sets the component 'initialized' to false.
        // And it leaves a 'gap' in the Panel but we like that, so no refresh here.
    } // end clear


    static String getIconDescriptionForStatus(int status) {
        String iString;

        //noinspection EnhancedSwitchMigration
        switch (status) {
            case TodoNoteData.TODO_COMPLETED:
                iString = todo_done.getDescription();
                break;
            case TodoNoteData.TODO_INPROG:
                iString = todo_inprog.getDescription();
                break;
            case TodoNoteData.TODO_WAITING:
                iString = todo_wait.getDescription();
                break;
            case TodoNoteData.TODO_QUERY:
                iString = todo_query.getDescription();
                break;
            case TodoNoteData.TODO_OBE:
                iString = todo_obe.getDescription();
                break;
            case TodoNoteData.TODO_STARTED:
            default:
                iString = null;
                break;
        } // end switch
        return iString;
    } // end getIconDescriptionForStatus


    // Why is this override needed ???   It is, but why?
    @Override
    NoteData getNoteData() {
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
        ((TodoNoteGroupPanel) myNoteGroupPanel).showComponent(this, blnIAmOn);
        super.noteActivated(blnIAmOn);
    }

    // Move the TodoNoteData to a Day Note.  This happens with Events as well,
    //   but here it is done at the individual Component level whereas Events
    //   are aged as a group and more than one might be affected with different
    //   effects on the visible interface, so a group 'refresh' is needed there
    //   after the move and it is not possible to leave a gap because the entire
    //   list gets reloaded.  But for a TodoNoteGroupPanel - we just leave a gap.
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
            MemoryBank.debug("Moving TodoNote to Today: " + today);
            myTodoNoteData.setTodoDate(LocalDate.now());
        }

        // Convert the item to a DayNoteData, using the TodoNoteData-flavored constructor.
        DayNoteData dnd = new DayNoteData(myTodoNoteData);

        // Use the date to get the right DayNoteGroup, then add the note to it.
        LocalDate theTodoDate = myTodoNoteData.getTodoDate();
        String groupName = CalendarNoteGroup.getGroupNameForDate(theTodoDate, GroupType.DAY_NOTES);
        NoteGroup theGroup = new GroupInfo(groupName, GroupType.DAY_NOTES).getNoteGroup();
        theGroup.appendNote(dnd);

        // Save the updated DayNoteGroup.  This is done outside of any Panel.
        theGroup.saveNoteGroup();

        // If the day we have moved to was already being shown in the DayNoteGroupPanel then any unsaved
        // changes that it might have had, were preserved when the group was retrieved.  However, there
        // would not have been a panel reload of that Day, so we should remove the Panel from its keeper
        // in order to force a reload of the data if/when "Day Notes" is reselected.
        if(AppTreePanel.theInstance.theAppDays != null) {
            DayNoteGroupPanel theAppDays = AppTreePanel.theInstance.theAppDays;
            LocalDate theDayNoteDate = theAppDays.getDate();
            if (theAppDays.getTitle(theDayNoteDate).equals(theAppDays.getTitle(theTodoDate))) {
                // Delete the Panel from its keeper.
                AppTreePanel.theInstance.theAppDays = null;
                AppTreePanel.theInstance.theTabbedCalendarNoteGroupPanel = null;
            }
        }

        clear();  // Clear our note line.  This creates a 'gap'.
    } // end moveToDayNote

    public void resetColumnOrder(int theOrder) {
        String pos = String.valueOf(theOrder); // 123,132,213,231,312,321 - expecting one of these...
        System.out.println("TodoNoteComponent resetColumnOrder to " + pos);

        removeAll(); // With the DndLayout, it is easier to start over than try to rearrange.

        componentHeight = getComponentHeight(); // Defaulting to one line.
        if(theOrder < 200) { // 123, 132
            add(pbThePriorityButton, "1");
            if(theOrder == 123) {
                if (myTodoNoteData.multiline) {
                    componentHeight = MULTI_LINE_HEIGHT;
                    add(noteScroller, "Stretch");
                } else {
                    add(noteTextField, "Stretch");
                }
                add(sbTheStatusButton, "3");
            } else { // 132
                add(sbTheStatusButton, "3");
                if (myTodoNoteData.multiline) {
                    componentHeight = MULTI_LINE_HEIGHT;
                    add(noteScroller, "Stretch");
                } else {
                    add(noteTextField, "Stretch");
                }
            }
        } else if(theOrder < 300) { // 213, 231
            if (myTodoNoteData.multiline) {
                componentHeight = MULTI_LINE_HEIGHT;
                add(noteScroller, "Stretch");
            } else {
                add(noteTextField, "Stretch");
            }
            if(theOrder == 213) {
                add(pbThePriorityButton, "1");
                add(sbTheStatusButton, "3");
            } else { // 231
                add(sbTheStatusButton, "3");
                add(pbThePriorityButton, "1");
            }
        } else { // 312, 321
            if(theOrder == 312) {
                add(sbTheStatusButton, "3");
                add(pbThePriorityButton, "1");
                if (myTodoNoteData.multiline) {
                    componentHeight = MULTI_LINE_HEIGHT;
                    add(noteScroller, "Stretch");
                } else {
                    add(noteTextField, "Stretch");
                }
            } else { // 321
                add(sbTheStatusButton, "3");
                if (myTodoNoteData.multiline) {
                    componentHeight = MULTI_LINE_HEIGHT;
                    add(noteScroller, "Stretch");
                } else {
                    add(noteTextField, "Stretch");
                }
                add(pbThePriorityButton, "1");
            }
        }

        // This was needed after paging was implemented.
        if (myTodoNoteData.multiline) {
            noteTextArea.transferFocusUpCycle();  // new 4/18/2023
        } else {
            noteTextField.transferFocusUpCycle();  // new 3/19/2008
        }
    } // end resetColumnOrder


    // Called after a change to the encapsulated data, to show
    //   the visual effects of the change.
    @Override
    protected void resetComponent() {
        pbThePriorityButton.setPriority(myTodoNoteData.getPriority());
        sbTheStatusButton.setStatus(myTodoNoteData.getStatus());

        removeAll(); // With a DndLayout, it is easiest to just do it over.
        add(pbThePriorityButton);

        if (myTodoNoteData.multiline) {
            componentHeight = MULTI_LINE_HEIGHT;
            //remove(noteTextField);
            add(noteScroller, "Stretch");
            noteTextArea.requestFocusInWindow();
        } else {
            componentHeight = getComponentHeight();
            //remove(noteScroller);
            add(noteTextField,"Stretch");
            noteTextField.requestFocusInWindow();
        }
        add(sbTheStatusButton);

        int theOrder = ((TodoGroupProperties) myNoteGroupPanel.myNoteGroup.getGroupProperties()).columnOrder;
        if(theOrder != TodoNoteGroupPanel.INORDER) resetColumnOrder(theOrder);

    } // end resetComponent


    @Override
    void resetPanelStatusMessage(int textStatus) {
        String s = " ";

        switch (textStatus) {
            case NEEDS_TEXT -> s = "Click here to enter text for this task.";
            case HAS_BASE_TEXT -> s = "Double-click here to add details about this task.";
            case HAS_EXT_TEXT -> {
                // This gives away the 'hidden' text, if
                //   there is no primary (blue) text.
                s = "Double-click here to see/edit";
                s += " the additional details for this task.";
            }
        } // end switch
        myNoteGroupPanel.setStatusMessage(s);
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


    public void resetVisibility() {
        pbThePriorityButton.setVisible(((TodoGroupProperties) myNoteGroupPanel.myNoteGroup.myProperties).showPriority);
    } // end resetVisibility


    @Override
    void setEditable(boolean b) {
        super.setEditable(b);
        pbThePriorityButton.setEditable(b);
        sbTheStatusButton.setEditable(b);
    }

    @Override
    public void setNoteData(NoteData newNoteData) {
        if (newNoteData instanceof TodoNoteData) {  // same type, but cast is still needed
            setTodoNoteData((TodoNoteData) newNoteData);
        } else { // Not 'my' type, but we can make it so (coming from a 'paste' operation).
            setTodoNoteData(new TodoNoteData(newNoteData));
        }
    } // end setNoteData


    void setTodoNoteData(TodoNoteData newNoteData) {
        myTodoNoteData = newNoteData;

        // update visual components...
        initialized = true;  // without updating the 'lastModDate'
        resetText();
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
        //   had a separate copy of the data object, just the reference to it.

        // So - copy the data objects.
        if (tnd1 != null) tnd1 = new TodoNoteData(tnd1);
        if (tnd2 != null) tnd2 = new TodoNoteData(tnd2);

        if (tnd1 == null) tnc.clear();
        else tnc.setNoteData(tnd1);

        if (tnd2 == null) this.clear();
        else this.setTodoNoteData(tnd2);

        myNoteGroupPanel.setGroupChanged(true);
    } // end swap

    //---------------------------------------------------------
    // End of NoteComponent specific methods
    //---------------------------------------------------------

    //---------------------------------------------------------
    // Inner Classes -
    //---------------------------------------------------------
    protected class PriorityButton extends JButton implements MouseListener {
        @Serial
        private static final long serialVersionUID = 1L;

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
            disableEvents(AWTEvent.FOCUS_EVENT_MASK + AWTEvent.COMPONENT_EVENT_MASK
                    + AWTEvent.ACTION_EVENT_MASK + AWTEvent.MOUSE_MOTION_EVENT_MASK
                    + AWTEvent.ITEM_EVENT_MASK);
        } // end PriorityButton constructor

        public void clear() {
            setPriority(0);
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
            d.width = minWidth;
            return d;
        } // end getPreferredSize

        public int getPriority() {
            return Priority;
        }

        void setEditable(boolean b) {
            if(b) { // This limits us to only one mouseListener corresponding to 'this'.
                // The limitation is needed because this method is called whenever a page is (re-)loaded.
                MouseListener[] mouseListeners = getMouseListeners();
                boolean alreadyEditable = false;
                for (MouseListener ml : mouseListeners) {
                    if(ml.getClass() == this.getClass()) {
                        alreadyEditable = true;
                        break;
                    }
                }
                if (!alreadyEditable) { // Separate line, for debug clarity.
                    addMouseListener(this);
                }
            }
            // Luckily, if 'this' is not currently a MouseListener then the next line is a silent no-op,
            // and otherwise it does what we've asked.
            else removeMouseListener(this);
        }


        void setPriority(int value) {
            if (!initialized) return;
            if (value < 0) return;
            if (value > ((TodoNoteGroupPanel) myNoteGroupPanel).getMaxPriority()) return;
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

        // Override these, to disable the 'depressed' color change.
        public void setBackground() { }
        public void setForeground() { }

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
            myNoteGroupPanel.setStatusMessage(s);
        }

        public void mouseExited(MouseEvent e) {
            // System.out.println(e);
            rightClicked = false;
            leftClicked = false;
            myNoteGroupPanel.setStatusMessage(" ");
        } // end mouseExited

        public void mousePressed(MouseEvent e) {
            setActive();

            // System.out.println(e);

            int m = e.getModifiersEx();
            if ((m & InputEvent.BUTTON3_DOWN_MASK) != 0) {
//            if (e.isMetaDown()) {
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
                myNoteGroupPanel.setStatusMessage(s);
                return;
            } // end if

            if (leftClicked) {
                if (Priority < ((TodoNoteGroupPanel) myNoteGroupPanel).getMaxPriority()) Priority++;
                else Priority = 0;
                leftClicked = false;
            } else {
                if (Priority > 0) Priority--;
                else Priority = ((TodoNoteGroupPanel) myNoteGroupPanel).getMaxPriority();
                rightClicked = false;
            } // end if
            myNoteGroupPanel.setGroupChanged(true);
            if (Priority == 0) setText(" ");
            else setText(String.valueOf(Priority));
            myTodoNoteData.setPriority(Priority);
        } // end mouseReleased
        //---------------------------------------------------------

    } // end class PriorityButton


    protected class StatusButton extends LabelButton implements MouseListener {
        @Serial
        private static final long serialVersionUID = 1L;

        public static final int minWidth = 40;

        private int theStatus;
        private int theOriginalStatus;

        static ArrayList<Integer> statusLoop;

        public StatusButton() {
            setOpaque(true);
            showStatusIcon();

            // The numbers assigned in the data class are no longer representative
            //   of the desired cycling order, but they cannot be changed due to the
            //   need to keep previously persisted data with correct original values.
            //   So here, the order is set differently into an ArrayList that is used
            //   when handling a mouse click to cycle thru the available statuses that
            //   will represent a new or updated status for the item.
            statusLoop = new ArrayList<>();
            statusLoop.add(TodoNoteData.TODO_STARTED);
            statusLoop.add(TodoNoteData.TODO_INPROG);
            statusLoop.add(TodoNoteData.TODO_WAITING);
            statusLoop.add(TodoNoteData.TODO_QUERY);
            statusLoop.add(TodoNoteData.TODO_OBE);
            statusLoop.add(TodoNoteData.TODO_COMPLETED);
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
            if(b) { // This limits us to only one mouseListener corresponding to 'this'.
                // The limitation is needed because this method is called whenever a page is (re-)loaded.
                MouseListener[] mouseListeners = getMouseListeners();
                boolean alreadyEditable = false;
                for (MouseListener ml : mouseListeners) {
                    if(ml.getClass() == this.getClass()) {
                        alreadyEditable = true;
                        break;
                    }
                }
                if (!alreadyEditable) { // Separate line, for debug clarity.
                    addMouseListener(this);
                }
            }
            // Luckily, if 'this' is not currently a MouseListener then the next line is a silent no-op,
            // and otherwise it does what we've asked.
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
                case TodoNoteData.TODO_STARTED -> setIcon(null);
                case TodoNoteData.TODO_COMPLETED -> setIcon(todo_done);
                case TodoNoteData.TODO_INPROG -> setIcon(todo_inprog);
                case TodoNoteData.TODO_WAITING -> setIcon(todo_wait);
                case TodoNoteData.TODO_QUERY -> setIcon(todo_query);
                case TodoNoteData.TODO_OBE -> setIcon(todo_obe);
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
                myNoteGroupPanel.setStatusMessage(s);
                return;
            } // end if

            // Get the index into the statusLoop for the current status value.
            int theIndex = statusLoop.indexOf(theStatus);

            // Increase or Decrease the index per the mouse button,
            //   and adjust for potential wrap-around.
            if (e.isMetaDown()) {
                theIndex--;
                if (theIndex<0) theIndex = statusLoop.size() - 1;
            } else {
                theIndex++;
                if (theIndex >= statusLoop.size()) theIndex = 0;
            } // end if

            // Alter the status.
            theStatus = statusLoop.get(theIndex);
            myTodoNoteData.setStatus(theStatus);

            // Now display the correct icon.
            showStatusIcon();

            //showStatusMessage();
            myNoteGroupPanel.setStatusMessage(myTodoNoteData.getStatusString());
        } // end mouseClicked


        public void mouseEntered(MouseEvent e) {
            if (!initialized) return;

            // System.out.println(e);
            if (!myTodoNoteData.hasText()) return;
            myNoteGroupPanel.setStatusMessage(myTodoNoteData.getStatusString());
            theOriginalStatus = myTodoNoteData.getStatus();
        } // end mouseEntered


        public void mouseExited(MouseEvent e) {
            // System.out.println(e);
            myNoteGroupPanel.setStatusMessage(" ");

            if (!initialized) return;

            // Update the source data, if applicable.
            //  This allows several 'clicks' to occur but no required list save,
            //  as long as the user leaves the status where they found it.
            if (theOriginalStatus != myTodoNoteData.getStatus()) {
                myNoteGroupPanel.setGroupChanged(true);
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
            if (theNoteComponent == null) return;

            tnc = (TodoNoteComponent) theNoteComponent;
            JMenuItem jm = (JMenuItem) e.getSource();
            String s = jm.getText();
            // System.out.println(s);
            //noinspection EnhancedSwitchMigration
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
            tnc.myNoteGroupPanel.setGroupChanged(true);
        } // end actionPerformed
    } // end class PopHandler

} // end class TodoNoteComponent
