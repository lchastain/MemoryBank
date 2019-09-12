import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Vector;

public class TodoNoteGroup extends NoteGroup implements DateSelection {
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(TodoBranchHelper.class);

    // Values used in sorting.
    private static final int TOP = 0;
    static final int BOTTOM = 1;
    private static final int STAY = 2;
    static final int INORDER = 123;

    private TodoGroupHeader listHeader;
    private ThreeMonthColumn tmc;  // For Date selection
    private TodoNoteComponent tNoteComponent;

    private String strTheGroupFilename;
    private static Notifier optionPane;
    public static FileChooser filechooser;

    // This is saved/loaded
    public TodoListProperties myVars; // Variables - flags and settings

    static {
        // The File Chooser supports the 'merge' functionality.
        filechooser = new FileChooser() {
        };
        filechooser.setCurrentDirectory(new File(MemoryBank.userDataHome));
        javax.swing.filechooser.FileFilter ff = new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f != null) {
                    if (f.isDirectory()) return true;
                    String filename = f.getName().toLowerCase();
                    return filename.startsWith("todo_");
                } // end if
                return false;
            } // end accept

            public String getDescription() {
                return "To Do lists (todo_*.json)";
            } // end getDescription
        };
        filechooser.addChoosableFileFilter(ff);
        filechooser.setAcceptAllFileFilterUsed(false);
        filechooser.setFileSystemView(FileSystemView.getFileSystemView());
        MemoryBank.init();
    } // end static

    public TodoNoteGroup(String fname) {
        super("Todo Item"); // Sets the default subject, in case items are moved to another group type.

        setName(fname.trim()); // The component-level name is null, otherwise.
        log.debug("Constructing: " + getName());

        enc.remove(0); // Remove the subjectChooser.
        // We may want to make this operation less numeric in the future,
        //   but this works for now and no ENC structural changes are expected.

        strTheGroupFilename = MemoryBank.userDataHome + File.separatorChar;
        strTheGroupFilename += "todo_" + fname + ".json";

        tmc = new ThreeMonthColumn();
        tmc.setSubscriber(this);
        optionPane = new Notifier() {
        }; // Uses all default methods.

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
        //   since other items on this row make it slightly 'higher'
        //   than the pager control.
        npThePager.setBackground(heading.getBackground());
        heading.add(npThePager, "East");
        //----------------------------------------------------------


        add(heading, BorderLayout.NORTH);

        // Wrapped tmc in a FlowLayout panel, to prevent stretching.
        JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnl1.add(tmc);
        add(pnl1, BorderLayout.EAST);

        updateGroup(); // This is where the file gets loaded (if it exists)
        if (objGroupProperties != null) {
            myVars = (TodoListProperties) objGroupProperties;
        } else {
            myVars = new TodoListProperties();
        } // end if
        if (myVars.columnOrder != INORDER) checkColumnOrder();

        listHeader = new TodoGroupHeader(this);
        setGroupHeader(listHeader);
    } // end constructor


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


    private static String chooseFileName(String buttonLabel) {
        int returnVal = filechooser.showDialog(null, buttonLabel);
        boolean badPlace = false;

        String s = filechooser.getCurrentDirectory().getAbsolutePath();
        String ems;

        // Check here to see if directory changed, reset if so.
        // System.out.println("Final directory: " + s);
        if (!s.equals(MemoryBank.userDataHome)) {
            filechooser.setCurrentDirectory(new File(MemoryBank.userDataHome));
            badPlace = true;
        } // end if

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (badPlace) {
                // Warn user that they are not allowed to navigate.
                ems = "Navigation outside of your data directory is not allowed!";
                ems += "\n           " + buttonLabel + " operation cancelled.";
                optionPane.showMessageDialog(null, ems,
                        "Warning", JOptionPane.WARNING_MESSAGE);
                return null;
            } else {
                return filechooser.getSelectedFile().getAbsolutePath();
            } // end if badPlace / else
        } else return null;
    } // end chooseFileName


    //-------------------------------------------------------------
    // Method Name:  dateSelected
    //
    // Interface to the Three Month Calendar; called by the tmc.
    //-------------------------------------------------------------
    public void dateSelected(Date d) {
        System.out.println("LogTodo - date selected on TMC = " + d);

        if (tNoteComponent == null) {
            String s;
            s = "You must select an item before a date can be linked!";
            setMessage(s);
            tmc.setChoice(null);
            return;
        } // end if

        TodoNoteData tnd = (TodoNoteData) (tNoteComponent.getNoteData());
        tnd.setTodoDate(d);

        System.out.println(d);
        tNoteComponent.setTodoNoteData(tnd);
    } // end dateSelected


    // -------------------------------------------------------------------
    // Method Name: getGroupFilename
    //
    // This method returns the name of the file where the data for this
    //   group of notes is loaded / saved.
    // -------------------------------------------------------------------
    public String getGroupFilename() {
        return strTheGroupFilename;
    }// end getGroupFilename


    int getMaxPriority() {
        return myVars.maxPriority;
    } // end getMaxPriority


    //--------------------------------------------------------
    // Method Name: getNoteComponent
    //
    // Returns a TodoNoteComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    //--------------------------------------------------------
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
        String mergeFile = chooseFileName("Merge");
        if (mergeFile == null) return;

        try {
            String text = FileUtils.readFileToString(new File(mergeFile), StandardCharsets.UTF_8.name());
            Object[] theGroup = AppUtil.mapper.readValue(text, Object[].class);
            //System.out.println("Merging NoteGroup data from JSON file: " + AppUtil.toJsonString(theGroup));
            Vector<TodoNoteData> mergeVector;
            mergeVector = AppUtil.mapper.convertValue(theGroup[1], new TypeReference<Vector<TodoNoteData>>() {
            });
            System.out.println("Number of Items to merge in: " + mergeVector.size());
            for (TodoNoteData tnd : mergeVector) {
                if (tnd.hasText()) {
                    TodoNoteComponent tnc = (TodoNoteComponent) groupNotesListPanel.getComponent(lastVisibleNoteIndex);
                    tnc.setTodoNoteData(tnd); // this sets his 'initialized' to true
                    if (lastVisibleNoteIndex == getHighestNoteComponentIndex()) break;
                    lastVisibleNoteIndex++;
                } // end if there is text
            } // end for
        } catch (IOException ex) {
            ex.printStackTrace();
            String ems = "Error in loading " + mergeFile + " !\n";
            ems = ems + ex.getMessage();
            ems = ems + "\nList merge operation aborted.";
            JOptionPane.showMessageDialog(
                    JOptionPane.getFrameForComponent(this), ems, "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        } // end try/catch

        setGroupChanged();
        preClose();
        updateGroup();  // need to check column order???
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

    //-----------------------------------------------------------------
    // Method Name:  prettyName
    //
    // A formatter for a filename specifier - drop off the path
    //   prefix and/or trailing '.json', if present.  Note
    //   that this method name was chosen so as to not conflict with
    //   the 'getName' of the Component ancestor of this class.
    //-----------------------------------------------------------------
    public static String prettyName(String theLongName) {
        int i = theLongName.lastIndexOf(File.separatorChar);
        String thePrettyName = theLongName;
        if (i > 0) { // if it has the sep char then it also has the 'todo_'
            thePrettyName = theLongName.substring(i + 6);
        } else {  // we may only be prettifying the filename, vs path+filename.
            if (theLongName.startsWith("todo_")) {
                thePrettyName = theLongName.substring(5);
            }
        }
        i = thePrettyName.lastIndexOf(".json");
        if (i == -1) return thePrettyName;
        return thePrettyName.substring(0, i);
    } // end prettyName


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

    //-------------------------------------------------------------------
    // Method Name: reportFocusChange
    //
    // Called by the NoteComponent that gained or lost focus.
    // This overrides the (no-op) base class behavior in order to
    //   intercept those events.
    //-------------------------------------------------------------------
    @Override
    void reportFocusChange(NoteComponent nc, boolean noteIsActive) {
        showComponent((TodoNoteComponent) nc, noteIsActive);
    } // end reportFocusChange


    //-----------------------------------------------------------------
    // Method Name:  saveAs
    //
    // Called (indirectly) from the menu bar.
    // Prompts the user for a new list name, checks it for validity,
    // then if ok, saves the file with that name.
    //-----------------------------------------------------------------
    boolean saveAs() {
        Frame f = JOptionPane.getFrameForComponent(this);

        String thePrompt = "Please enter the new list name";
        int q = JOptionPane.QUESTION_MESSAGE;
        String newName = optionPane.showInputDialog(f, thePrompt, "Save As", q);

        // The user cancelled; return with no complaint.
        if (newName == null) return false;

        newName = newName.trim(); // eliminate outer space.

        // Test new name validity.
        if (!TodoBranchHelper.nameCheck(newName, f)) return false;

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
        // they may not understand what it is they are asking for.
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
        String newFilename = MemoryBank.userDataHome + File.separatorChar;
        newFilename += "todo_" + newName + ".json";

        if ((new File(newFilename)).exists()) {
            ems = "A list named " + newName + " already exists!\n";
            ems += "  operation cancelled.";
            optionPane.showMessageDialog(f, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } // end if

        // Now change the name and save.
        //------------------------------------
        log.debug("Saving " + oldName + " as " + newName);

        // 'setFileName' wants the 'pretty' name as parameter, even though we already
        // have its long form from when we checked for pre-existence, above.  But one
        // other HUGE consideration is that it also sets the name of the component
        // itself, which translates into an in-place change of the name of the list
        // held by the TodoListKeeper.  Unfortunately, that list will still have the
        // old title, so it still needs to be removed from the keeper.  The calling
        // context will take care of that.
        setFileName(newName);

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


    //-----------------------------------------------------------------
    // Method Name:  setFileName
    //
    // Provided as support for a 'save as' functionality.
    //  (By calling this first, then just calling 'save').  Any
    //  checking for validity is responsibility of calling context.
    //-----------------------------------------------------------------
    private void setFileName(String fname) {
        strTheGroupFilename = MemoryBank.userDataHome + File.separatorChar;
        strTheGroupFilename += "todo_" + fname.trim() + ".json";

        setName(fname.trim());  // Keep the 'pretty' name in the component.
        setGroupChanged();
    } // end setFileName


    @Override
    void setGroupData(Object[] theGroup) {
        myVars = AppUtil.mapper.convertValue(theGroup[0], TodoListProperties.class);
        NoteData.loading = true; // We don't want to affect the lastModDates!
        vectGroupData = AppUtil.mapper.convertValue(theGroup[1], new TypeReference<Vector<TodoNoteData>>() {
        });
        NoteData.loading = false; // Restore normal lastModDate updating.
    }

    // Used by test methods
    public void setNotifier(Notifier newNotifier) {
        optionPane = newNotifier;
    }

    public void setFilechooser(FileChooser newFileChooser) {
        filechooser = newFileChooser;
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
        setGroupChanged();

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
    private void showComponent(TodoNoteComponent nc, boolean showit) {
        if (showit) {
            tNoteComponent = nc;
            TodoNoteData tnd = (TodoNoteData) nc.getNoteData();

            // Show the previously selected date
            if (tnd.getNoteDate() != null) {
                tmc.setBaseDate(tnd.getNoteDate());
                tmc.setChoice(tnd.getNoteDate());
            }
        } else {
            tNoteComponent = null;
            tmc.setChoice(null);
        } // end if
    } // end showComponent


    // Disabled this 9/02/2019 so that it does not pull down code coverage for tests.
    // Did not write a test for it because the feature is in serious question -
    // Did not just remove entirely because the work to create this method was significant, and
    // it may become relevant again at some point.  Currently a 'deadline' is not seen as
    // that in a Todolist, but we do still set a Date on a Todo item that roughly means 'do
    // this on or before this date', and we may want to become more specific as to what that
    // date really means - deadline is only one option; it could also mean 'do after', or
    // 'do on exactly this date and not before or after', and possibly other variants, and
    // possibly tied in with linkages.  So - keep this here but disabled until those questions
    // are answered or we know for a FACT that it will never again be needed.
//    void sortDeadline(int direction) {
//        TodoNoteData todoData1, todoData2;
//        TodoNoteComponent todoComponent1, todoComponent2;
//        int i, j;
//        long lngDate1, lngDate2;
//        int sortMethod = 0;
//        int items = lastVisibleNoteIndex;
//        MemoryBank.debug("TodoNoteGroup sortDeadline - Number of items in list: " + items);
//
//        // Bitmapping of the 6 possible sorting variants.
//        //  Zero-values are ASCENDING, STAY (but that is not the default)
//        if (direction == DESCENDING) sortMethod += 4;
//        if (myVars.whenNoKey == TOP) sortMethod += 1;
//        if (myVars.whenNoKey == BOTTOM) sortMethod += 2;
//
//        switch (sortMethod) {
//            case 0:         // ASCENDING, STAY
//                // System.out.println("Sorting: Deadline, ASCENDING, STAY");
//                for (i = 0; i < (items - 1); i++) {
//                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
//                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
//                    if (todoData1 == null) lngDate1 = 0;
//                    else lngDate1 = todoData1.getNoteDate().getTime();
//                    if (lngDate1 == 0) continue; // No key; skip.
//                    for (j = i + 1; j < items; j++) {
//                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
//                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
//                        if (todoData2 == null) lngDate2 = 0;
//                        else lngDate2 = todoData2.getNoteDate().getTime();
//                        if (lngDate2 == 0) continue; // No key; skip.
//                        if (lngDate1 > lngDate2) {
//                            lngDate1 = lngDate2;
//                            todoComponent1.swap(todoComponent2);
//                        } // end if
//                    } // end for j
//                } // end for i
//                break;
//            case 1:         // ASCENDING, TOP
//                // System.out.println("Sorting: Deadline, ASCENDING, TOP");
//                for (i = 0; i < (items - 1); i++) {
//                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
//                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
//                    if (todoData1 == null) lngDate1 = 0;
//                    else lngDate1 = todoData1.getNoteDate().getTime();
//                    for (j = i + 1; j < items; j++) {
//                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
//                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
//                        if (todoData2 == null) lngDate2 = 0;
//                        else lngDate2 = todoData2.getNoteDate().getTime();
//                        if (lngDate1 > lngDate2) {
//                            lngDate1 = lngDate2;
//                            todoComponent1.swap(todoComponent2);
//                        } // end if
//                    } // end for j
//                } // end for i
//                break;
//            case 2:         // ASCENDING, BOTTOM
//                // System.out.println("Sorting: Deadline, ASCENDING, BOTTOM");
//                for (i = 0; i < (items - 1); i++) {
//                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
//                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
//                    if (todoData1 == null) lngDate1 = 0;
//                    else lngDate1 = todoData1.getNoteDate().getTime();
//                    for (j = i + 1; j < items; j++) {
//                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
//                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
//                        if (todoData2 == null) lngDate2 = 0;
//                        else lngDate2 = todoData2.getNoteDate().getTime();
//                        if (((lngDate1 > lngDate2) && (lngDate2 != 0)) || (lngDate1 == 0)) {
//                            lngDate1 = lngDate2;
//                            todoComponent1.swap(todoComponent2);
//                        } // end if
//                    } // end for j
//                } // end for i
//                break;
//            case 4:         // DESCENDING, STAY
//                // System.out.println("Sorting: Deadline, DESCENDING, STAY");
//                for (i = 0; i < (items - 1); i++) {
//                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
//                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
//                    if (todoData1 == null) lngDate1 = 0;
//                    else lngDate1 = todoData1.getNoteDate().getTime();
//                    if (lngDate1 == 0) continue; // No key; skip.
//                    for (j = i + 1; j < items; j++) {
//                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
//                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
//                        if (todoData2 == null) lngDate2 = 0;
//                        else lngDate2 = todoData2.getNoteDate().getTime();
//                        if (lngDate2 == 0) continue; // No key; skip.
//                        if (lngDate1 < lngDate2) {
//                            lngDate1 = lngDate2;
//                            todoComponent1.swap(todoComponent2);
//                        } // end if
//                    } // end for j
//                } // end for i
//                break;
//            case 5:         // DESCENDING, TOP
//                // System.out.println("Sorting: Deadline, DESCENDING, TOP");
//                for (i = 0; i < (items - 1); i++) {
//                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
//                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
//                    if (todoData1 == null) lngDate1 = 0;
//                    else lngDate1 = todoData1.getNoteDate().getTime();
//                    for (j = i + 1; j < items; j++) {
//                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
//                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
//                        if (todoData2 == null) lngDate2 = 0;
//                        else lngDate2 = todoData2.getNoteDate().getTime();
//                        if (((lngDate1 < lngDate2) && (lngDate1 != 0)) || (lngDate2 == 0)) {
//                            lngDate1 = lngDate2;
//                            todoComponent1.swap(todoComponent2);
//                        } // end if
//                    } // end for j
//                } // end for i
//                break;
//            case 6:         // DESCENDING, BOTTOM
//                // System.out.println("Sorting: Deadline, DESCENDING, BOTTOM");
//                for (i = 0; i < (items - 1); i++) {
//                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
//                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
//                    if (todoData1 == null) lngDate1 = 0;
//                    else lngDate1 = todoData1.getNoteDate().getTime();
//                    for (j = i + 1; j < items; j++) {
//                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
//                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
//                        if (todoData2 == null) lngDate2 = 0;
//                        else lngDate2 = todoData2.getNoteDate().getTime();
//                        if (lngDate1 < lngDate2) {
//                            lngDate1 = lngDate2;
//                            todoComponent1.swap(todoComponent2);
//                        } // end if
//                    } // end for j
//                } // end for i
//                break;
//        } // end switch sortMethod
//        //thisFrame.getContentPane().validate();
//    } // end sortDeadline


    void sortPriority(int direction) {
        TodoNoteData todoData1, todoData2;
        int pri1, pri2;
        boolean doSwap;
        int items = vectGroupData.size();

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
            todoData1 = (TodoNoteData) vectGroupData.elementAt(i);
            if (todoData1 == null) pri1 = 0;
            else pri1 = todoData1.getPriority();
            if (pri1 == 0) if (myVars.whenNoKey == STAY) continue; // No key; skip.
            for (int j = i + 1; j < items; j++) {
                doSwap = false;
                todoData2 = (TodoNoteData) vectGroupData.elementAt(j);
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
                    vectGroupData.setElementAt(todoData2, i);
                    vectGroupData.setElementAt(todoData1, j);
                    pri1 = pri2;
                    todoData1 = todoData2;
                } // end if
            } // end for j
        } // end for i

        AppUtil.localDebug(false);

        // Display the same page, now with possibly different contents.
        postSort();
    } // end sortPriority


    void sortText(int direction) {
        TodoNoteData todoData1, todoData2;
        String str1, str2;
        boolean doSwap;
        int items = vectGroupData.size();

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
            todoData1 = (TodoNoteData) vectGroupData.elementAt(i);
            if (todoData1 == null) str1 = "";
            else str1 = todoData1.getNoteString().trim();
            if (str1.equals("")) if (myVars.whenNoKey == STAY) continue; // No key; skip.
            for (int j = i + 1; j < items; j++) {
                doSwap = false;
                todoData2 = (TodoNoteData) vectGroupData.elementAt(j);
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
                    vectGroupData.setElementAt(todoData2, i);
                    vectGroupData.setElementAt(todoData1, j);
                    str1 = str2;
                    todoData1 = todoData2;
                } // end if
            } // end for j
        } // end for i

        AppUtil.localDebug(false);

        // Display the same page, now with possibly different contents.
        postSort();
    } // end sortText
} // end class TodoNoteGroup


