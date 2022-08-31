import com.fasterxml.jackson.core.type.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Vector;

@SuppressWarnings({"unchecked", "rawtypes"})
public class TodoNoteGroupPanel extends NoteGroupPanel implements DateSelection {
    private static final Logger log = LoggerFactory.getLogger(TodoNoteGroupPanel.class);
    private static final int DEFAULT_PAGE_SIZE = 20;

    // Values used in sorting.
    private static final int TOP = 0;
    static final int BOTTOM = 1;
    private static final int STAY = 2;
    static final int INORDER = 123;

    TodoGroupHeader listHeader;
    private ThreeMonthColumn tmc;  // For Date selection
    private TodoNoteComponent tNoteComponent;

    public TodoNoteGroupPanel(@NotNull GroupInfo groupInfo, int pageSize) {
        super(pageSize);

        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.
        myNoteGroup.myNoteGroupPanel = this;
        if(groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        loadNotesPanel();

        int theOrder = INORDER;
        if (myNoteGroup.getGroupProperties() != null) {
            theOrder = ((TodoGroupProperties) myNoteGroup.getGroupProperties()).columnOrder;
        } // end if
        if (theOrder != INORDER) checkColumnOrder();

        listHeader = new TodoGroupHeader(this);
        setGroupHeader(listHeader);

        buildMyPanel(groupInfo.getGroupName());
        theNotePager.reset(1);
        setListMenu(AppMenuBar.getNodeMenu("To Do List"));
    } // end constructor


    public TodoNoteGroupPanel(GroupInfo groupInfo) {
        this(groupInfo, DEFAULT_PAGE_SIZE);
    }

    public TodoNoteGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.TODO_LIST), DEFAULT_PAGE_SIZE);
    }


    @Override
    protected void adjustMenuItems(boolean b) {
        //MemoryBank.debug("TodoNoteGroupPanel.adjustMenuItems <" + b + ">");
        if(fosterNoteGroupPanel != null) { // This NoteGroupPanel is one tab of a collection.
            fosterNoteGroupPanel.adjustMenuItems(b);
        } else {
            super.adjustMenuItems(b);
        }
    }

    private void buildMyPanel(String groupName) {
        log.debug("Building components for a TodoGroupPanel named: " + groupName);

        tmc = new ThreeMonthColumn(); // Will be garbage-collected, if we set a different one.
        tmc.setSubscriber(this);

        // Create the window title
        JLabel lblListTitle = new JLabel();
        lblListTitle.setHorizontalAlignment(JLabel.CENTER);
        lblListTitle.setForeground(Color.white);
        lblListTitle.setFont(Font.decode("Serif-bold-20"));
        lblListTitle.setText(groupName);

        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(Color.blue);
        heading.add(lblListTitle, "Center");

        // Set the pager's background to the same color as this row,
        //   since the title on this row is taller and that makes
        //   a thin slice of 'open space' below the pager.  This is
        //   better than stretching the pager control because that
        //   separator spacing is actually visually preferred.
        theNotePager.setBackground(heading.getBackground());
        heading.add(theNotePager, "East");

        theBasePanel.add(heading, BorderLayout.NORTH);

        // Wrapped tmc in a FlowLayout panel, to prevent stretching.
        JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnl1.add(tmc);
        theBasePanel.add(pnl1, BorderLayout.EAST);
        listHeader = new TodoGroupHeader(this);
        setGroupHeader(listHeader);
    }


    //-------------------------------------------------------------------
    // Method Name: checkColumnOrder
    //
    // Re-order the columns.
    // This may be needed if the list had been saved with a different
    //   column order than the default.  In that case, this method is
    //   called from the constructor after the file load.
    // It is also needed after paging away from a 'short' page where
    //   a 'sort' was done, even though no reordering occurred.
    // We do it for ALL notes, visible or not, so that
    //   newly activated notes will appear properly.
    //-------------------------------------------------------------------
    private void checkColumnOrder() {
        TodoNoteComponent tempNote;

        for (int i = 0; i <= getHighestNoteComponentIndex(); i++) {
            tempNote = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
            tempNote.resetColumnOrder(((TodoGroupProperties) myNoteGroup.myProperties).columnOrder);
        } // end for
    } // end checkColumnOrder


    private GroupInfo chooseMergeGroup() {
        GroupInfo theChoice = null;
        ArrayList<String> groupNames = MemoryBank.dataAccessor.getGroupNames(GroupType.TODO_LIST, true);
        groupNames.remove(getGroupName()); // Remove ourselves from consideration.

        // Convert to an Object array so the JOptionPane static method can present a selection list.
        Object[] theNames = new String[groupNames.size()];
        theNames = groupNames.toArray(theNames);

        String message = "Choose a list to merge with " + getGroupName();
        String title = "Merge TodoLists";
        // Important issue here!  The selection list is presented as an initially closed combobox dropdown with
        //   either your initial selection or the first choice selected.  BUT if the number of choices is more
        //   than 20, there is no dropdown control; you see a scrollable list instead, with no preselected choice.
        //   This is built-in Swing behavior, not my doing.
        String theGroupName = optionPane.showInputDialog(theBasePanel, message,
                title, JOptionPane.PLAIN_MESSAGE, null, theNames, null);

        System.out.println("The choice is: " + theGroupName);
        if (theGroupName != null) {
            theChoice = new GroupInfo(theGroupName, GroupType.TODO_LIST);
        }
        return theChoice;
    } // end chooseMergeGroup


    // Interface to the Three Month Calendar; called by the tmc.
    @Override // Implementation of the DateSelection interface
    public void dateSelected(LocalDate ld) {
        if(getEditable()) {
            MemoryBank.debug("Date selected on TMC = " + ld);

            if (tNoteComponent == null) {
                String s;
                s = "You must select an item before a date can be linked!";
                setStatusMessage(s);
                tmc.setChoice(null);
                return;
            } // end if

            TodoNoteData tnd = (TodoNoteData) (tNoteComponent.getNoteData());
            tnd.setTodoDate(ld);
            tNoteComponent.setTodoNoteData(tnd);
        }
    } // end dateSelected

    int getMaxPriority() {
        return ((TodoGroupProperties) myNoteGroup.myProperties).maxPriority;
    } // end getMaxPriority


    //--------------------------------------------------------
    // Method Name: getNoteComponent
    //
    // Returns a TodoNoteComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    //--------------------------------------------------------
    @Override
    public TodoNoteComponent getNoteComponent(int i) {
        return (TodoNoteComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    ThreeMonthColumn getThreeMonthColumn() {
        return tmc;
    }


    //-------------------------------------------------------------------
    // Method Name: makeNewNote
    //
    // Called by the NoteGroup (base class) constructor
    //-------------------------------------------------------------------
    @Override
    JComponent makeNewNote(int i) {
        TodoNoteComponent tnc = new TodoNoteComponent(this, i);
        tnc.setVisible(false);
        return tnc;
    } // end makeNewNote


    @SuppressWarnings({"unchecked"})
    public void merge() {
        GroupInfo groupInfo = chooseMergeGroup();
        if (groupInfo == null) return;

        // Load the group to merge in -
        Object[] theGroup = myNoteGroup.groupDataAccessor.loadNoteGroupData(groupInfo);
        //System.out.println("Merging NoteGroup data from JSON file: " + AppUtil.toJsonString(theGroup));

        Vector<TodoNoteData> mergeVector = AppUtil.mapper.convertValue(theGroup[1], new TypeReference<>() {
        });

        // Create a 'set', to contain only unique items from both lists.
        LinkedHashSet<NoteData> theUniqueSet = new LinkedHashSet<>(myNoteGroup.noteGroupDataVector);
        theUniqueSet.addAll(mergeVector);

        // Make a new Vector from the unique set, and set our group data to the new merged data vector.
        Vector mergedVector = new Vector<>(theUniqueSet);
        showGroupData(mergedVector);
        setGroupChanged(true);
    } // end merge


    // Overrides the base class no-op method, to ensure the group
    //   columns are displayed in the correct order.
    @Override
    protected void pageNumberChanged() {
        if (tNoteComponent != null) showComponent(tNoteComponent, false);

        // The column order must be reset because we may be coming from
        //   a page that had fewer items than this one, where a sort had
        //   occurred.  In the dndLayoutManager, non-visible rows appear
        //   to have a negative width in the text column, which causes
        //   them to be swapped on the 'short' page, only to show up
        //   (now in the wrong order) when paging to a 'full' page.
        // You cannot fix this by disallowing a swap for non-showing rows,
        //   because an intentional swap will not affect them and then
        //   the problem would appear when adding new notes.
        // Will not make this call conditional on a short-page-sort,
        //   because the gain in performance against the overhead needed
        //   to track that condition, is questionable.
        checkColumnOrder();
    } // end pageNumberChanged


    @Override
    void preClosePanel() {
        updateProperties();
        super.preClosePanel();
    }


    // Disabled this 9/02/2019 so that it does not pull down code coverage for tests.
    // If you REALLY REALLY need it, uncomment here and in the TodoGroupHeader to re-enable.
    // Did write a test for it (but then disabled it too) because as the feature is currently
    // written it needs user interaction with a Print dialog and we don't yet have an
    // interface like Notifier or FileChooser to overcome that.  Of course such an interface
    // could quickly be written but this feature itself is in serious question - its usage
    // and usefulness appears to be near-none, and there are some known problems with it
    // (SCR0066)
    // Did not just remove entirely because the work to create this method was significant, and
    // it may become relevant and usable again at some point.  So - keep this here but disabled
    // until it is either repaired or we know for a FACT that it will never again be needed.
//    public void printList() {
//        int t = strTheGroupFilename.lastIndexOf(".todolist");
//        String dumpFileName;    // Formatted text for printout
//        dumpFileName = strTheGroupFilename.substring(0, t) + ".dump";
//
//        PrintWriter outFile = null;
//        TodoNoteComponent tnc;
//        TodoNoteData tnd;
//        int i;
//        int Priority;
//        String todoText;
//        String extText;
//
//        // Open the output file.
//        try {
//            outFile = new PrintWriter(new BufferedWriter
//                    (new FileWriter(dumpFileName)), true);
//        } catch (IOException e) {
//            e.printStackTrace();
//            System.exit(0);
//        } // end try/catch
//
//        int listSize = lastVisibleNoteIndex;
//        // System.out.println("listSize = " + listSize);
//
//        for (i = 0; i < listSize; i++) {
//            tnc = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
//            tnd = (TodoNoteData) tnc.getNoteData();
//
//            // Print no priorities higher than pCutoff
//            Priority = tnd.getPriority();
//            if (myVars.pCutoff < Priority) continue;
//
//            // Do not print items with no primary text
//            todoText = tnd.getNoteString().trim();
//            if (todoText.equals("")) continue;
//
//            // spacing after the previous line.  By placing it here, there
//            //   will be none after the last line.
//            if (i > 0)
//                for (int j = 0; j < myVars.lineSpace; j++) outFile.println("");
//
//            // Completion space
//            if (myVars.pCSpace) outFile.print("_______ ");
//
//            // Priority
//            if (myVars.pPriority) {
//                outFile.print("(");
//                if (Priority < 10) outFile.print(" "); // leading space
//                outFile.print(Priority + ")  ");
//            } // end if
//
//            // Deadline
//            if (myVars.pDeadline) {
//                String pdead = "";  // tnc.getDeadText();
//
//                if (!pdead.equals("")) {
//                    // use the rest of the line for the deadline
//                    outFile.println(pdead);
//
//                    // indent the next line to account for:
//                    if (myVars.pCSpace) outFile.print("        "); // Completion Space
//                    if (myVars.pPriority) outFile.print("      "); // Priority
//                } // end if
//            } // end if
//
//            // To Do Text
//            outFile.println(todoText);
//
//            // Extended Text
//            if (myVars.pEText) {
//                extText = tnd.getExtendedNoteString();
//                if (!extText.equals("")) {
//                    String indent = "    ";
//                    StringBuilder rs = new StringBuilder(indent); // ReturnString
//
//                    for (int j = 0; j < extText.length(); j++) {
//                        if (extText.substring(j).startsWith("\t"))
//                            rs.append("        "); // convert tabs to 8 spaces.
//                        else if (extText.substring(j).startsWith("\n"))
//                            rs.append("\n").append(indent);
//                        else
//                            rs.append(extText, j, j + 1);
//                    } // end for j
//
//                    extText = rs.toString();
//                    outFile.println(extText);
//                } // end if
//            } // end if
//        } // end for i
//        outFile.close();
//
//        TextFilePrinter tfp = new TextFilePrinter(dumpFileName,
//                JOptionPane.getFrameForComponent(this));
//        tfp.setOptions(myVars.pHeader, myVars.pFooter, myVars.pBorder);
//        tfp.setVisible(true);
//    } // end printList


    // Called from the menu bar:  AppTreePanel.handleMenuBar() --> saveGroupAs() --> saveAs()
    // Prompts the user for a new list name, checks it for validity,
    // then if ok, saves the file with that name.
    boolean saveAs() {
        Frame theFrame = JOptionPane.getFrameForComponent(theBasePanel);

        String thePrompt = "Please enter the new list name";
        int q = JOptionPane.QUESTION_MESSAGE;
        String newName = optionPane.showInputDialog(theFrame, thePrompt, "Save As", q);

        // The user cancelled; return with no complaint.
        if (newName == null) return false;

        newName = newName.trim(); // eliminate outer space.

        // Test new name validity.
        String theComplaint = myNoteGroup.groupDataAccessor.getObjectionToName(newName);
        if (!theComplaint.isEmpty()) {
            optionPane.showMessageDialog(theFrame, theComplaint,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Get the current list name -
        String oldName = getGroupName();

        // If the new name equals the old name, just do the save as the user
        //   has asked and don't tell them that they are an idiot.  But no
        //   other actions on the filesystem or the tree will be taken.
        if (newName.equals(oldName)) {
            preClosePanel();
            return false;
        } // end if

        // Check to see if the destination NoteGroup already exists.
        // If so then complain and refuse to do the saveAs.

        // Other applications might offer the option of overwriting the existing data.  This was considered
        // and rejected because of the possibility of overwriting data that is currently being shown in
        // another panel.  We could check for that as well, but decided not to because - why should we go to
        // heroic efforts to handle a user request where it seems like they may not understand what it is
        // that they are asking for?  This is the same approach that was taken in the 'rename' handling.
        ArrayList<String> groupNames = MemoryBank.dataAccessor.getGroupNames(GroupType.TODO_LIST, false);
        if(groupNames.contains(newName)) {
            ems = "An To Do List named " + newName + " already exists!\n";
            ems += "  operation cancelled.";
            optionPane.showMessageDialog(theFrame, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
            // After we refuse to do the operation due to a preexisting destination NoteGroup with the same name,
            // the user has several recourses, depending on what it was they really wanted to do - they could
            // delete the preexisting NoteGroup or rename it, after which a second attempt at this operation
            // would succeed, or they could realize that they had been having a senior moment and abandon the
            // effort, or they could choose a different new name and try again.
        }

        // Now change the name, update Properties and the data accessor, and save.
        //------------------------------------
        log.debug("Saving " + oldName + " as " + newName);
        GroupProperties myGroupProperties = myNoteGroup.getGroupProperties();

        // 'setGroupName' sets the name of the group, which translates into an
        // in-place change of the name of the list held by the TodoListKeeper.
        // Unfortunately, that list will still have the old title, so it still needs
        // to be removed from the keeper.  The calling context must take care of that.
        myGroupProperties.setGroupName(newName);
        GroupInfo myGroupInfo = new GroupInfo(myGroupProperties);

        // The data accessor (constructed along with this Panel) has the old name; need to update.
        myNoteGroup.groupDataAccessor = MemoryBank.dataAccessor.getNoteGroupDataAccessor(myGroupInfo);

        setGroupChanged(true);
        preClosePanel();

        return true;
    } // end saveAs

    public void setOptions() {
        TodoNoteComponent tempNote;

        // Preserve original value
        boolean blnOrigShowPriority = ((TodoGroupProperties) myNoteGroup.myProperties).showPriority;

        // Construct the Option Panel (TodoOpts) using the current TodoListProperties
        TodoOpts todoOpts = new TodoOpts(((TodoGroupProperties) myNoteGroup.myProperties));

        // Show a dialog whereby the options can be changed
        int doit = optionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(theBasePanel), todoOpts,
                "Set Options", JOptionPane.OK_CANCEL_OPTION);

        if (doit == -1) return; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return;

        // Get the values back out of the Option Panel
        myNoteGroup.setGroupProperties(todoOpts.getValues());
        setGroupChanged(true);

        // Was there a reset-worthy change?
        if (((TodoGroupProperties) myNoteGroup.myProperties).showPriority != blnOrigShowPriority) {
            System.out.println("Resetting the list and header");
            for (int i = 0; i < getHighestNoteComponentIndex(); i++) {
                tempNote = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
                tempNote.resetVisibility();
            } // end for
            listHeader.resetVisibility();
        } // end if - if view change
    } // end setOptions


    void setThreeMonthColumn(ThreeMonthColumn newTmc) {
        tmc = newTmc;
    }

    //  Action needed when a line has either gone active
    //    or inactive, called by the overridden noteActivated.
    void showComponent(TodoNoteComponent nc, boolean showit) {
        if (showit) {
            tNoteComponent = nc;
            TodoNoteData tnd = (TodoNoteData) nc.getNoteData();

            // Show the previously selected date
            if (tnd.getTodoDate() != null) {
                tmc.setChoice(tnd.getTodoDate());
            }
        } else {
            tNoteComponent = null;
            tmc.setChoice(null);
        } // end if
    } // end showComponent


    void sortPriority(int direction) {
        TodoNoteData todoData1, todoData2;
        int pri1, pri2;
        boolean doSwap;
        int items = myNoteGroup.noteGroupDataVector.size();

        AppUtil.localDebug(true);

//        preSort();
        unloadNotesPanel(theNotePager.getCurrentPage());
        MemoryBank.debug("TodoNoteGroup.sortPriority - Number of items in list: " + items);

        // Prettyprinting of sort conditions -
        if (direction == ASCENDING) MemoryBank.dbg("  ASCENDING \t");
        else MemoryBank.dbg("  DESCENDING \t");
        if (((TodoGroupProperties) myNoteGroup.myProperties).whenNoKey == TOP) MemoryBank.dbg("TOP");
        else if (((TodoGroupProperties) myNoteGroup.myProperties).whenNoKey == BOTTOM) MemoryBank.dbg("BOTTOM");
        else if (((TodoGroupProperties) myNoteGroup.myProperties).whenNoKey == STAY) MemoryBank.dbg("STAY");
        MemoryBank.dbg("\n");

        for (int i = 0; i < (items - 1); i++) {
            todoData1 = (TodoNoteData) myNoteGroup.noteGroupDataVector.elementAt(i);
            if (todoData1 == null) pri1 = 0;
            else pri1 = todoData1.getPriority();
            if (pri1 == 0) if (((TodoGroupProperties) myNoteGroup.myProperties).whenNoKey == STAY) continue; // No key; skip.
            for (int j = i + 1; j < items; j++) {
                doSwap = false;
                todoData2 = (TodoNoteData) myNoteGroup.noteGroupDataVector.elementAt(j);
                if (todoData2 == null) pri2 = 0;
                else pri2 = todoData2.getPriority();
                if (pri2 == 0) if (((TodoGroupProperties) myNoteGroup.myProperties).whenNoKey == STAY) continue; // No key; skip.

                if (direction == ASCENDING) {
                    if (((TodoGroupProperties) myNoteGroup.myProperties).whenNoKey == BOTTOM) {
                        if (((pri1 > pri2) && (pri2 != 0)) || (pri1 == 0)) doSwap = true;
                    } else {
                        // TOP and STAY have same behavior for ASCENDING, unless a
                        //   key was missing in which case we bailed out earlier.
                        if (pri1 > pri2) doSwap = true;
                    } // end if TOP/BOTTOM
                } else if (direction == DESCENDING) {
                    if (((TodoGroupProperties) myNoteGroup.myProperties).whenNoKey == TOP) {
                        if (((pri1 < pri2) && (pri1 != 0)) || (pri2 == 0)) doSwap = true;
                    } else {
                        // BOTTOM and STAY have same behavior for DESCENDING, unless a
                        //   key was missing in which case we bailed out earlier.
                        if (pri1 < pri2) doSwap = true;
                    } // end if TOP/BOTTOM
                } // end if ASCENDING/DESCENDING

                if (doSwap) {
                    MemoryBank.debug("  Moving Vector element " + i + " below " + j + "  (zero-based)");
                    myNoteGroup.noteGroupDataVector.setElementAt(todoData2, i);
                    myNoteGroup.noteGroupDataVector.setElementAt(todoData1, j);
                    pri1 = pri2;
                    todoData1 = todoData2;
                } // end if
            } // end for j
        } // end for i

        AppUtil.localDebug(false);

        // Display the same page, now with possibly different contents.
        checkColumnOrder();
        loadPage(theNotePager.getCurrentPage());
    } // end sortPriority


    private void updateProperties() {
        // Update the header text of the columns.
        ((TodoGroupProperties) myNoteGroup.myProperties).column1Label = listHeader.getColumnHeader(1);
        ((TodoGroupProperties) myNoteGroup.myProperties).column2Label = listHeader.getColumnHeader(2);
        ((TodoGroupProperties) myNoteGroup.myProperties).column3Label = listHeader.getColumnHeader(3);
        ((TodoGroupProperties) myNoteGroup.myProperties).columnOrder = listHeader.getColumnOrder();
    } // end updateProperties

} // end class TodoNoteGroup
