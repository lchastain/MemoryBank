/**  This class displays a group of TodoNoteComponent.
 *   It is named to be consistent with other 'Group'
 *   classes in this app, but you may think of it as
 *   the TodoList.
 */

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.io.*;
import java.util.Date;

public class TodoNoteGroup extends NoteGroup implements DateSelection {
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(TodoBranchHelper.class);

    // Values used in sorting.
    private static final int TOP = 0;
    static final int BOTTOM = 1;
    public static final int STAY = 2;
    public static final int INORDER = 123;

    public TodoGroupHeader listHeader;
    public static JFileChooser filechooser;
    public static javax.swing.filechooser.FileFilter ff;

    private ThreeMonthColumn tmc;  // For Date selection
    private TodoNoteComponent tNoteComponent;

    private String strTheGroupFilename;

    // This is saved/loaded
    public TodoListProperties myVars; // Variables - flags and settings

    static {
        // The File Chooser supports the 'merge' functionality.
        filechooser = new JFileChooser(MemoryBank.userDataHome);
        ff = new javax.swing.filechooser.FileFilter() {
            public boolean accept(File f) {
                if (f != null) {
                    if (f.isDirectory()) return true;
                    String filename = f.getName().toLowerCase();
                    int i = filename.lastIndexOf('.');
                    if (i > 0 && i < filename.length() - 1) {
                        String extension = filename.substring(i + 1);
                        if (extension.equals("todolist")) return true;
                    } // end if
                } // end if
                return false;
            } // end accept

            public String getDescription() {
                return "To Do lists (*.todolist)";
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
        strTheGroupFilename += fname + ".todolist";

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


    public static String chooseFileName(String buttonLabel) {
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
                JOptionPane.showMessageDialog(null, ems,
                        "Warning", JOptionPane.WARNING_MESSAGE);
                return null;
            } else {
                return MemoryBank.userDataHome + File.separatorChar
                        + prettyName(filechooser.getSelectedFile().getAbsolutePath()) + ".todolist";
            } // end if badPlace
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
        tNoteComponent.setNoteData(tnd);
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


    public int getMaxPriority() {
        return myVars.maxPriority;
    } // end getMaxPriority


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


    //--------------------------------------------------------
    // Method Name: getNoteComponent
    //
    // Gives containers some access.
    //--------------------------------------------------------
    public TodoNoteComponent getNoteComponent(int i) {
        return (TodoNoteComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    //-------------------------------------------------------------------
    // Method Name: loadNoteComponent
    //
    // In this case, a TodoNoteComponent.
    //-------------------------------------------------------------------
//  public boolean loadNoteComponent(ObjectInputStream ois, int i) 
//    throws EOFException, ClassNotFoundException, IOException  {
//
//    TodoNoteComponent tempNote;
//    TodoNoteData tempNoteData;
//
//    // AppUtil.localDebug(true);
//
//    // Before reading the first item from a To do List, we need
//    //   to get (and read past) the TodoListProperties.
//    if(i==0) {
//      myVars = (TodoListProperties) ois.readObject();
//      checkColumnOrder();
//    } // end if
//    
//    tempNoteData = (TodoNoteData) ois.readObject();
//    
//    MemoryBank.debug("  Loaded index " + i + " ID: " + tempNoteData.getNoteId());
//    tempNote = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
//    tempNote.setNoteData(tempNoteData);
//    tempNote.setVisible(true);
//    
//    // AppUtil.localDebug(false);
//    return true;
//  } // end loadNoteComponent


    //-------------------------------------------------------------------
    // Method Name: makeNewNote
    //
    // Called by the NoteGroup (base class) constructor
    //-------------------------------------------------------------------
    protected JComponent makeNewNote(int i) {
        if (i == 0) myVars = new TodoListProperties();
        TodoNoteComponent tnc = new TodoNoteComponent(this, i);
        tnc.setVisible(false);
        return tnc;
    } // end makeNewNote


    public void merge() {
        String mergeFile = chooseFileName("Merge");
        if (mergeFile == null) return;

        Exception e = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        TodoNoteComponent tnc;

        try {
            fis = new FileInputStream(mergeFile);
            ois = new ObjectInputStream(fis);
            TodoListProperties tv = (TodoListProperties) ois.readObject();
            System.out.println("Number of Items to merge in: " + tv.numberOfItems);
            for (int i = 0; i < tv.numberOfItems; i++) {
                TodoNoteData tnd = (TodoNoteData) ois.readObject();
                if (tnd.hasText()) {
                    tnc = (TodoNoteComponent) groupNotesListPanel.getComponent(lastVisibleNoteIndex);
                    if (lastVisibleNoteIndex == getHighestNoteComponentIndex()) {
                        if (!tnc.initialized) tnc.setNoteData(tnd);
                        break;
                    } // end if
                    tnc.setNoteData(tnd);
                    lastVisibleNoteIndex++;
                } // end if there is text
            } // end for i
            ois.close();
            fis.close();
        } catch (ClassCastException cce) {
            e = cce;
        } catch (ClassNotFoundException cnfe) {
            e = cnfe;
        } catch (InvalidClassException ice) {
            e = ice;
        } catch (FileNotFoundException fnfe) {
            System.out.println(mergeFile + " was not found!  Merge cancelled.");
        } catch (EOFException eofe) {
            // The end of file may be reached before reading the 'numberOfItems'
            //   in some cases where the file was saved after having cleared
            //   items off the list.  The 'lastVisibleNoteIndex' in that case
            //   will not accurately reflect the true number of items.
            // Do nothing other than close; this is not a 'real' problem
            //   anymore since to do items are only loaded into pre-existing
            //   NoteComponents and not actually created.
            try {
                assert ois != null;
                ois.close();
                fis.close();
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }
        } catch (IOException ioe) {
            e = ioe;
        } // end try/catch

        if (e != null) {
            String ems = "Error in loading " + mergeFile + " !\n";
            ems = ems + e.toString();
            ems = ems + "\nList merge operation aborted.";
            JOptionPane.showMessageDialog(
                    JOptionPane.getFrameForComponent(this), ems, "Error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        } // end if
        setGroupChanged();
        preClose();
        updateGroup();  // need to check column order???
        myVars = (TodoListProperties) objGroupProperties;
    } // end merge


    //------------------------------------------------------------------
    // Method Name: pageNumberChanged
    //
    // Overrides the base class no-op method, to ensure the group
    //   columns are displayed in the correct order.
    //------------------------------------------------------------------
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


    //-----------------------------------------------------------------
    // Method Name:  prettyName
    //
    // A formatter for a filename specifier - drop off the path
    //   prefix and/or trailing '.todolist', if present.  Note
    //   that this method name was chosen so as to not conflict with
    //   the 'getName' of the Component ancestor of this class.
    //-----------------------------------------------------------------
    public static String prettyName(String s) {
        int i = s.lastIndexOf(File.separatorChar);
        s = s.substring(i + 1);
        i = s.lastIndexOf(".todolist");
        if (i == -1) return s;
        return s.substring(0, i);
    } // end prettyName


    //--------------------------------------------------------------------------
    // Method Name: printList
    //
    //--------------------------------------------------------------------------
    public void printList() {
        int t = strTheGroupFilename.lastIndexOf(".todolist");
        String dumpFileName;    // Formatted text for printout
        dumpFileName = strTheGroupFilename.substring(0, t) + ".dump";

        PrintWriter outFile = null;
        TodoNoteComponent tnc;
        TodoNoteData tnd;
        int i;
        int Priority;
        String todoText;
        String extText;

        // Open the output file.
        try {
            outFile = new PrintWriter(new BufferedWriter
                    (new FileWriter(dumpFileName)), true);
        } catch (IOException e) {
            System.out.println(e);
            System.exit(0);
        } // end try/catch

        int listSize = lastVisibleNoteIndex;
        // System.out.println("listSize = " + listSize);

        for (i = 0; i < listSize; i++) {
            tnc = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
            tnd = (TodoNoteData) tnc.getNoteData();

            // Print no priorities higher than pCutoff
            Priority = tnd.getPriority();
            if (myVars.pCutoff < Priority) continue;

            // Do not print items with no primary text
            todoText = tnd.getNoteString().trim();
            if (todoText.equals("")) continue;

            // spacing after the previous line.  By placing it here, there
            //   will be none after the last line.
            if (i > 0)
                for (int j = 0; j < myVars.lineSpace; j++) outFile.println("");

            // Completion space
            if (myVars.pCSpace) outFile.print("_______ ");

            // Priority
            if (myVars.pPriority) {
                outFile.print("(");
                if (Priority < 10) outFile.print(" "); // leading space
                outFile.print(Priority + ")  ");
            } // end if

            // Deadline
            if (myVars.pDeadline) {
                String pdead = "";  // tnc.getDeadText();

                if (!pdead.equals("")) {
                    // use the rest of the line for the deadline
                    outFile.println(pdead);

                    // indent the next line to account for:
                    if (myVars.pCSpace) outFile.print("        "); // Completion Space
                    if (myVars.pPriority) outFile.print("      "); // Priority
                } // end if
            } // end if

            // To Do Text
            outFile.println(todoText);

            // Extended Text
            if (myVars.pEText) {
                extText = tnd.getExtendedNoteString();
                if (!extText.equals("")) {
                    String indent = "    ";
                    String rs = indent; // ReturnString

                    for (int j = 0; j < extText.length(); j++) {
                        if (extText.substring(j).startsWith("\t"))
                            rs = rs + "        "; // convert tabs to 8 spaces.
                        else if (extText.substring(j).startsWith("\n"))
                            rs = rs + "\n" + indent;
                        else
                            rs = rs + extText.substring(j, j + 1);
                        // The 'j+1' will not exceed string length when j=length
                        //  because 'substring' works with one less on the 2nd int.
                    } // end for j

                    extText = rs;
                    outFile.println(extText);
                } // end if
            } // end if
        } // end for i
        outFile.close();

        TextFilePrinter tfp = new TextFilePrinter(dumpFileName,
                JOptionPane.getFrameForComponent(this));
        tfp.setOptions(myVars.pHeader, myVars.pFooter, myVars.pBorder);
        tfp.setVisible(true);
    } // end printList

    //-------------------------------------------------------------------
    // Method Name: reportFocusChange
    //
    // Called by the NoteComponent that gained or lost focus.
    // Overrides the (no-op) base class behavior in order to
    //   intercept those events.
    //-------------------------------------------------------------------
    protected void reportFocusChange(NoteComponent nc, boolean noteIsActive) {
        showComponent((TodoNoteComponent) nc, noteIsActive);
    } // end reportComponentChange


    //-----------------------------------------------------------------
    // Method Name:  saveAs
    //
    // Called (indirectly) from the menu bar.
    // Prompts the user for a new list name, checks it for validity,
    // then if ok, saves the file with that name.
    //-----------------------------------------------------------------
    public boolean saveAs() {
        Frame f = JOptionPane.getFrameForComponent(this);

        String thePrompt = "Please enter the new list name";
        int q = JOptionPane.QUESTION_MESSAGE;
        String newName = JOptionPane.showInputDialog(f, thePrompt, "Save As", q);

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

        // Note:
        //--------------------------------------------------------------
        // Other applications might offer the option of overwriting
        // the existing file.  This was considered and rejected
        // because of the possibility of overwriting a file that
        // is currently open.  We could check for that as well, but
        // decided not to because - why should we go to heroic
        // efforts to handle a user request where it seems like
        // they may not understand what it is they are asking for.
        // This is the same approach that was
        // taken in the 'rename' handling.

        // If we refuse the operation due to a preexisting destination
        // file name then the user has several recourses, depending on
        // what it was they really wanted to do - they could delete
        // the preexisting file or rename it, after which a second
        // attempt at this operation would succeed, or they could
        // realize that they had been having a senior moment and
        // abandon the effort, or they could choose a different
        // new name and try again, etc.
        //--------------------------------------------------------------
        String newFilename = MemoryBank.userDataHome + File.separatorChar;
        newFilename += newName + ".todolist";

        if ((new File(newFilename)).exists()) {
            ems = "A list named " + newName + " already exists!\n";
            ems += "  operation cancelled.";
            JOptionPane.showMessageDialog(f, ems,
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


    //-----------------------------------------------------------------
    // Method Name:  setFileName
    //
    // Provided as support for a 'save as' functionality.
    //  (By calling this first, then just calling 'save').  Any
    //  checking for validity is responsibility of calling context.
    //-----------------------------------------------------------------
    public void setFileName(String fname) {
        strTheGroupFilename = MemoryBank.userDataHome + File.separatorChar;
        strTheGroupFilename += fname.trim() + ".todolist";

        setName(fname.trim());  // Keep the 'pretty' name in the component.
        setGroupChanged();
    } // end setFileName


    public void setOptions() {
        TodoNoteComponent tempNote;

        TodoOpts to = new TodoOpts(myVars);
        int doit = JOptionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(this), to,
                "Set Options", JOptionPane.OK_CANCEL_OPTION);

        if (doit == -1) return; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return;

        boolean blnOrigShowPriority = myVars.showPriority;
        myVars = to.getValues();
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
    public void showComponent(TodoNoteComponent nc, boolean b) {
        if (b) {
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


    public void sortDeadline(int direction) {
        TodoNoteData todoData1, todoData2;
        TodoNoteComponent todoComponent1, todoComponent2;
        int i, j;
        long lngDate1, lngDate2;
        int sortMethod = 0;
        int items = lastVisibleNoteIndex;
        MemoryBank.debug("TodoNoteGroup sortDeadline - Number of items in list: " + items);

        // Bitmapping of the 6 possible sorting variants.
        //  Zero-values are ASCENDING, STAY (but that is not the default)
        if (direction == DESCENDING) sortMethod += 4;
        if (myVars.whenNoKey == TOP) sortMethod += 1;
        if (myVars.whenNoKey == BOTTOM) sortMethod += 2;

        switch (sortMethod) {
            case 0:         // ASCENDING, STAY
                // System.out.println("Sorting: Deadline, ASCENDING, STAY");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    if (lngDate1 == 0) continue; // No key; skip.
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (lngDate2 == 0) continue; // No key; skip.
                        if (lngDate1 > lngDate2) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 1:         // ASCENDING, TOP
                // System.out.println("Sorting: Deadline, ASCENDING, TOP");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (lngDate1 > lngDate2) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 2:         // ASCENDING, BOTTOM
                // System.out.println("Sorting: Deadline, ASCENDING, BOTTOM");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (((lngDate1 > lngDate2) && (lngDate2 != 0)) || (lngDate1 == 0)) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 4:         // DESCENDING, STAY
                // System.out.println("Sorting: Deadline, DESCENDING, STAY");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    if (lngDate1 == 0) continue; // No key; skip.
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (lngDate2 == 0) continue; // No key; skip.
                        if (lngDate1 < lngDate2) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 5:         // DESCENDING, TOP
                // System.out.println("Sorting: Deadline, DESCENDING, TOP");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (((lngDate1 < lngDate2) && (lngDate1 != 0)) || (lngDate2 == 0)) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
            case 6:         // DESCENDING, BOTTOM
                // System.out.println("Sorting: Deadline, DESCENDING, BOTTOM");
                for (i = 0; i < (items - 1); i++) {
                    todoComponent1 = (TodoNoteComponent) groupNotesListPanel.getComponent(i);
                    todoData1 = (TodoNoteData) todoComponent1.getNoteData();
                    if (todoData1 == null) lngDate1 = 0;
                    else lngDate1 = todoData1.getNoteDate().getTime();
                    for (j = i + 1; j < items; j++) {
                        todoComponent2 = (TodoNoteComponent) groupNotesListPanel.getComponent(j);
                        todoData2 = (TodoNoteData) todoComponent2.getNoteData();
                        if (todoData2 == null) lngDate2 = 0;
                        else lngDate2 = todoData2.getNoteDate().getTime();
                        if (lngDate1 < lngDate2) {
                            lngDate1 = lngDate2;
                            todoComponent1.swap(todoComponent2);
                        } // end if
                    } // end for j
                } // end for i
                break;
        } // end switch sortMethod
        //thisFrame.getContentPane().validate();
    } // end sortDeadline


    public void sortPriority(int direction) {
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


