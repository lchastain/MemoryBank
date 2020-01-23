import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Vector;

public class TodoNoteGroup extends NoteGroup implements DateSelection {
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(TodoNoteGroup.class);
    private static final int PAGE_SIZE = 20;

    // Values used in sorting.
    private static final int TOP = 0;
    static final int BOTTOM = 1;
    private static final int STAY = 2;
    static final int INORDER = 123;

    private TodoGroupHeader listHeader;
    private ThreeMonthColumn tmc;  // For Date selection
    private TodoNoteComponent tNoteComponent;

    private String theGroupFilename;
    static String areaName;

    // This is saved/loaded
    public TodoListProperties myVars; // Variables - flags and settings

    static {
        areaName = "TodoLists"; // Directory name under user data.
        MemoryBank.trace();
    } // end static

    public TodoNoteGroup(String fname) {
        this(fname, PAGE_SIZE);
    }

    public TodoNoteGroup(String fname, int pageSize) {
        super(pageSize);

        // Use an inherited (otherwise unused) method to store our list name.
        // It will be used by the 'saveAs' method.
        setName(fname.trim());
        log.debug("Constructing: " + getName());

        theGroupFilename = basePath() + "todo_" + fname + ".json";

        tmc = new ThreeMonthColumn();
        tmc.setSubscriber(this);

        // Create the window title
        JLabel lblListTitle = new JLabel();
        lblListTitle.setHorizontalAlignment(JLabel.CENTER);
        lblListTitle.setForeground(Color.white);
        lblListTitle.setFont(Font.decode("Serif-bold-20"));
        lblListTitle.setText(fname);

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
        //----------------------------------------------------------

        add(heading, BorderLayout.NORTH);

        // Wrapped tmc in a FlowLayout panel, to prevent stretching.
        JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnl1.add(tmc);
        add(pnl1, BorderLayout.EAST);

        updateGroup(); // This is where the file gets loaded (if it exists)
        myVars = new TodoListProperties();

        listHeader = new TodoGroupHeader(this);
        setGroupHeader(listHeader);
    } // end constructor


    static String basePath() {
        return NoteGroup.basePath(areaName);
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
            tempNote.resetColumnOrder(myVars.columnOrder);
        } // end for
    } // end checkColumnOrder

    private File chooseMergeFile() {
        File dataDir = new File(basePath());
        String myName = prettyName(theGroupFilename);

        // Get the complete list of Todo List filenames, except this one.
        String[] theFileList = dataDir.list(
                new FilenameFilter() {
                    // Although this filter does not account for directories, it is
                    // known that the basePath will not under normal program
                    // operation contain directories.
                    public boolean accept(File f, String s) {
                        if (myName.equals(prettyName(s))) return false;
                        return s.startsWith("todo_");
                    }
                }
        );

        // Reformat the list for presentation in the selection control.
        // ie, drop the prefix and file extension.
        ArrayList<String> todoListNames = new ArrayList<>();
        if (theFileList != null) {
            for (String aName : theFileList) {
                todoListNames.add(prettyName(aName));
            } // end for i
        }
        Object[] theNames = new String[todoListNames.size()];
        theNames = todoListNames.toArray(theNames);


        String message = "Choose a list to merge with " + myName;
        String title = "Merge TodoLists";
        String theChoice = optionPane.showInputDialog(this, message,
                title, JOptionPane.PLAIN_MESSAGE, null, theNames, null);

        System.out.println("The choice is: " + theChoice);
        if (theChoice == null) return null;
        return new File(basePath() + "todo_" + theChoice + ".json");
    } // end chooseMergeFile

    //-------------------------------------------------------------
    // Method Name:  dateSelected
    //
    // Interface to the Three Month Calendar; called by the tmc.
    //-------------------------------------------------------------
    public void dateSelected(LocalDate ld) {
        MemoryBank.debug("Date selected on TMC = " + ld);

        if (tNoteComponent == null) {
            String s;
            s = "You must select an item before a date can be linked!";
            setMessage(s);
            tmc.setChoice(null);
            return;
        } // end if

        TodoNoteData tnd = (TodoNoteData) (tNoteComponent.getNoteData());
        tnd.setTodoDate(ld);
        tNoteComponent.setTodoNoteData(tnd);
    } // end dateSelected


    public String getGroupFilename() {
        return theGroupFilename;
    }

    int getMaxPriority() {
        return myVars.maxPriority;
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


    //--------------------------------------------------------------
    // Method Name: getProperties
    //
    //  Called by saveGroup.
    //  Returns an actual object, vs the overriden method
    //    in the base class that returns a null.
    //--------------------------------------------------------------
    protected Object getProperties() {
        return myVars;
    } // end getProperties


    // Provided for Tests
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
        if (i == 0) myVars = new TodoListProperties();
        TodoNoteComponent tnc = new TodoNoteComponent(this, i);
        tnc.setVisible(false);
        return tnc;
    } // end makeNewNote


    public void merge() {
        File mergeFile = chooseMergeFile();
        if (mergeFile == null) return;

        // Load the file to merge in -
        Object[] theGroup = AppUtil.loadNoteGroupData(mergeFile);
        //System.out.println("Merging NoteGroup data from JSON file: " + AppUtil.toJsonString(theGroup));
        Vector<TodoNoteData> mergeVector;
        NoteData.loading = true; // We don't want to affect the lastModDates!
        mergeVector = AppUtil.mapper.convertValue(theGroup[1], new TypeReference<Vector<TodoNoteData>>() {} );
        NoteData.loading = false; // Restore normal lastModDate updating.

        // Create a 'set', to contain only unique items from both lists.
        LinkedHashSet<NoteData> theUniqueSet = new LinkedHashSet<>(groupDataVector);
        theUniqueSet.addAll(mergeVector);

        // Make a new Vector from the unique set, and set our group data to the new merged data vector.
        groupDataVector = new Vector<>(theUniqueSet);
        setGroupData(groupDataVector);
        setGroupChanged(true);
    } // end merge

    //------------------------------------------------------------------
    // Method Name: pageNumberChanged
    //
    // Overrides the base class no-op method, to ensure the group
    //   columns are displayed in the correct order.
    //------------------------------------------------------------------
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
    protected void preClose() {
        saveProperties();
        super.preClose();
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


    //-----------------------------------------------------------------
    // Method Name:  saveAs
    //
    // Called from the menu bar:
    // AppTreePanel.handleMenuBar() --> saveGroupAs() --> saveAs()
    // Prompts the user for a new list name, checks it for validity,
    // then if ok, saves the file with that name.
    //-----------------------------------------------------------------
    boolean saveAs() {
        Frame theFrame = JOptionPane.getFrameForComponent(this);

        String thePrompt = "Please enter the new list name";
        int q = JOptionPane.QUESTION_MESSAGE;
        String newName = optionPane.showInputDialog(theFrame, thePrompt, "Save As", q);

        // The user cancelled; return with no complaint.
        if (newName == null) return false;

        newName = newName.trim(); // eliminate outer space.

        // Test new name validity.
        String theComplaint = BranchHelperInterface.checkFilename(newName, basePath());
        if (!theComplaint.isEmpty()) {
            JOptionPane.showMessageDialog(theFrame, theComplaint,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Get the current list name -
        String oldName = getName();

        // If the new name equals the old name, just do the save as the user
        //   has asked and don't tell them that they are an idiot.  But no
        //   other actions on the filesystem or the tree will be taken.
        if (newName.equals(oldName)) {
            preClose();
            return false;
        } // end if

        // Check to see if the destination file name already exists.
        // If so then complain and refuse to do the saveAs.

        // Other applications might offer the option of overwriting
        // the existing file.  This was considered and rejected
        // because of the possibility of overwriting a file that
        // is currently open.  We could check for that as well, but
        // decided not to because - why should we go to heroic
        // efforts to handle a user request where it seems like
        // they may not understand what it is they are asking for?
        // This is the same approach that was taken in the 'rename' handling.

        // After we refuse the operation due to a preexisting destination
        // file name the user has several recourses, depending on
        // what it was they really wanted to do - they could delete
        // the preexisting file or rename it, after which a second
        // attempt at this operation would succeed, or they could
        // realize that they had been having a senior moment and
        // abandon the effort, or they could choose a different
        // new name and try again.
        //--------------------------------------------------------------
        String newFilename = basePath() + "todo_" + newName + ".json";
        if ((new File(newFilename)).exists()) {
            ems = "A list named " + newName + " already exists!\n";
            ems += "  operation cancelled.";
            optionPane.showMessageDialog(theFrame, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } // end if

        // Now change the name and save.
        //------------------------------------
        log.debug("Saving " + oldName + " as " + newName);

        // 'setName' sets the name of the component itself, which translates into an
        // in-place change of the name of the list held by the TodoListKeeper.
        // Unfortunately, that list will still have the old title, so it still needs
        // to be removed from the keeper.  The calling context will take care of that.
        theGroupFilename = basePath() + "todo_" + newName + ".json";
        setName(newName);  // Put the new 'pretty' name in the component.
        setGroupChanged(true);

        // Since this is effectively a new file, before we save we need to ensure that
        // the app will not fail in an attempt to remove the (nonexistent) old file.
        AppUtil.localArchive(true);
        preClose();
        AppUtil.localArchive(false);

        return true;
    } // end saveAs

    private void saveProperties() {
        // Update the header text of the columns.
        myVars.column1Label = listHeader.getColumnHeader(1);
        myVars.column2Label = listHeader.getColumnHeader(2);
        myVars.column3Label = listHeader.getColumnHeader(3);
        myVars.columnOrder = listHeader.getColumnOrder();
    } // end saveProperties


    @Override
    void setGroupData(Object[] theGroup) {
        myVars = AppUtil.mapper.convertValue(theGroup[0], TodoListProperties.class);
        NoteData.loading = true; // We don't want to affect the lastModDates!
        groupDataVector = AppUtil.mapper.convertValue(theGroup[1], new TypeReference<Vector<TodoNoteData>>() {
        });
        NoteData.loading = false; // Restore normal lastModDate updating.
    }

    // Used by test methods
    // BUT - later versions will just directly set it, no need for a test-only method.  Remove this when feasible.
    public void setNotifier(Notifier newNotifier) {
        optionPane = newNotifier;
    }

    public void setOptions() {
        TodoNoteComponent tempNote;

        // Preserve original value
        boolean blnOrigShowPriority = myVars.showPriority;

        // Construct the Option Panel (TodoOpts) using the current TodoListProperties
        TodoOpts todoOpts = new TodoOpts(myVars);

        // Show a dialog whereby the options can be changed
        int doit = optionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(this), todoOpts,
                "Set Options", JOptionPane.OK_CANCEL_OPTION);

        if (doit == -1) return; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return;

        // Get the values back out of the Option Panel
        myVars = todoOpts.getValues();
        setGroupChanged(true);

        // Was there a reset-worthy change?
        if (myVars.showPriority != blnOrigShowPriority) {
            System.out.println("Resetting the list and header");
            for (int i = 0; i < getHighestNoteComponentIndex(); i++) {
                tempNote = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
                tempNote.resetVisibility();
            } // end for
            listHeader.resetVisibility();
        } // end if - if view change
    } // end setOptions


    //--------------------------------------------------------------
    // Method Name: showComponent
    //
    //  Several actions needed when a line has
    //    either gone active or inactive.
    //--------------------------------------------------------------
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
        int items = groupDataVector.size();

        AppUtil.localDebug(true);

        preSort();
        MemoryBank.debug("TodoNoteGroup.sortPriority - Number of items in list: " + items);

        // Prettyprinting of sort conditions -
        if (direction == ASCENDING) MemoryBank.dbg("  ASCENDING \t");
        else MemoryBank.dbg("  DESCENDING \t");
        if (myVars.whenNoKey == TOP) MemoryBank.dbg("TOP");
        else if (myVars.whenNoKey == BOTTOM) MemoryBank.dbg("BOTTOM");
        else if (myVars.whenNoKey == STAY) MemoryBank.dbg("STAY");
        MemoryBank.dbg("\n");

        for (int i = 0; i < (items - 1); i++) {
            todoData1 = (TodoNoteData) groupDataVector.elementAt(i);
            if (todoData1 == null) pri1 = 0;
            else pri1 = todoData1.getPriority();
            if (pri1 == 0) if (myVars.whenNoKey == STAY) continue; // No key; skip.
            for (int j = i + 1; j < items; j++) {
                doSwap = false;
                todoData2 = (TodoNoteData) groupDataVector.elementAt(j);
                if (todoData2 == null) pri2 = 0;
                else pri2 = todoData2.getPriority();
                if (pri2 == 0) if (myVars.whenNoKey == STAY) continue; // No key; skip.

                if (direction == ASCENDING) {
                    if (myVars.whenNoKey == BOTTOM) {
                        if (((pri1 > pri2) && (pri2 != 0)) || (pri1 == 0)) doSwap = true;
                    } else {
                        // TOP and STAY have same behavior for ASCENDING, unless a
                        //   key was missing in which case we bailed out earlier.
                        if (pri1 > pri2) doSwap = true;
                    } // end if TOP/BOTTOM
                } else if (direction == DESCENDING) {
                    if (myVars.whenNoKey == TOP) {
                        if (((pri1 < pri2) && (pri1 != 0)) || (pri2 == 0)) doSwap = true;
                    } else {
                        // BOTTOM and STAY have same behavior for DESCENDING, unless a
                        //   key was missing in which case we bailed out earlier.
                        if (pri1 < pri2) doSwap = true;
                    } // end if TOP/BOTTOM
                } // end if ASCENDING/DESCENDING

                if (doSwap) {
                    MemoryBank.debug("  Moving Vector element " + i + " below " + j + "  (zero-based)");
                    groupDataVector.setElementAt(todoData2, i);
                    groupDataVector.setElementAt(todoData1, j);
                    pri1 = pri2;
                    todoData1 = todoData2;
                } // end if
            } // end for j
        } // end for i

        AppUtil.localDebug(false);

        // Display the same page, now with possibly different contents.
        checkColumnOrder();
        loadInterface(theNotePager.getCurrentPage());
    } // end sortPriority


    void sortText(int direction) {
        TodoNoteData todoData1, todoData2;
        String str1, str2;
        boolean doSwap;
        int items = groupDataVector.size();

        AppUtil.localDebug(true);

        preSort();
        MemoryBank.debug("TodoNoteGroup.sortText - Number of items in list: " + items);

        // Prettyprinting of sort conditions -
        if (direction == ASCENDING) MemoryBank.dbg("  ASCENDING \t");
        else MemoryBank.dbg("  DESCENDING \t");
        if (myVars.whenNoKey == TOP) MemoryBank.dbg("TOP");
        else if (myVars.whenNoKey == BOTTOM) MemoryBank.dbg("BOTTOM");
        else if (myVars.whenNoKey == STAY) MemoryBank.dbg("STAY");
        MemoryBank.dbg("\n");

        for (int i = 0; i < (items - 1); i++) {
            todoData1 = (TodoNoteData) groupDataVector.elementAt(i);
            if (todoData1 == null) str1 = "";
            else str1 = todoData1.getNoteString().trim();
            if (str1.equals("")) if (myVars.whenNoKey == STAY) continue; // No key; skip.
            for (int j = i + 1; j < items; j++) {
                doSwap = false;
                todoData2 = (TodoNoteData) groupDataVector.elementAt(j);
                if (todoData2 == null) str2 = "";
                else str2 = todoData2.getNoteString().trim();
                if (str2.equals("")) if (myVars.whenNoKey == STAY) continue; // No key; skip.

                if (direction == ASCENDING) {
                    if (myVars.whenNoKey == BOTTOM) {
                        if (((str1.compareTo(str2) > 0) && (!str2.equals(""))) || (str1.equals(""))) doSwap = true;
                    } else {
                        // TOP and STAY have same behavior for ASCENDING, unless a
                        //   key was missing in which case we bailed out earlier.
                        if (str1.compareTo(str2) > 0) doSwap = true;
                    } // end if TOP/BOTTOM
                } else if (direction == DESCENDING) {
                    if (myVars.whenNoKey == TOP) {
                        if (((str1.compareTo(str2) < 0) && (!str1.equals(""))) || (str2.equals(""))) doSwap = true;
                    } else {
                        // BOTTOM and STAY have same behavior for DESCENDING, unless a
                        //   key was missing in which case we bailed out earlier.
                        if (str1.compareTo(str2) < 0) doSwap = true;
                    } // end if TOP/BOTTOM
                } // end if ASCENDING/DESCENDING

                if (doSwap) {
                    MemoryBank.debug("  Moving data element " + i + " below " + j);
                    groupDataVector.setElementAt(todoData2, i);
                    groupDataVector.setElementAt(todoData1, j);
                    str1 = str2;
                    todoData1 = todoData2;
                } // end if
            } // end for j
        } // end for i

        AppUtil.localDebug(false);

        // Display the same page, now with possibly different contents.
        checkColumnOrder();
        loadInterface(theNotePager.getCurrentPage());
    } // end sortText
} // end class TodoNoteGroup


