import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import javax.swing.tree.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Vector;

public class TodoNoteGroup extends NoteGroup implements DateSelection {
    private static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(TodoNoteGroup.class);

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
    static String areaName;

    // This is saved/loaded
    public TodoListProperties myVars; // Variables - flags and settings

    static {
        areaName = "TodoLists"; // Directory name under user data.
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
        super();

        // Use an inherited (otherwise unused) method to store our list name.
        // It will be used by the 'saveAs' method.
        setName(fname.trim());
        log.debug("Constructing: " + getName());

        strTheGroupFilename = NoteGroup.basePath(areaName) + "todo_" + fname + ".json";

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
        myVars = new TodoListProperties();

        listHeader = new TodoGroupHeader(this);
        setGroupHeader(listHeader);
    } // end constructor


    static void addNewList(JTree jt) {
        String newName = "";
        String prompt = "Enter a name for the new To Do List";
        String title = "Add a new To Do List";

        newName = (String) JOptionPane.showInputDialog(
                jt,                           // parent component - for modality
                prompt,                       // prompt
                title,                        // pane title bar
                JOptionPane.QUESTION_MESSAGE, // type of pane
                null,                   // icon
                null,           // list of choices
                newName);                     // initial value

        if (newName == null) return;      // No user entry; dialog was Cancelled.
        newName = nameAdjust(newName);

        DefaultTreeModel theTreeModel = (DefaultTreeModel) jt.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        DefaultMutableTreeNode dmtn = BranchHelperInterface.getNodeByName(theRoot, "To Do Lists");
        if (dmtn != null) {
            // Declare a tree node for the new list.
            DefaultMutableTreeNode newList;

            // Allowing 'add' to act as a back-door selection of a list
            // that actually already exists is ok, but do
            // not add this choice to the branch if it is already there.
            boolean addNodeToBranch = true;
            newList = (DefaultMutableTreeNode) getChild(dmtn, newName);
            if (newList == null) {
                newList = new DefaultMutableTreeNode(newName);
            } else {
                addNodeToBranch = false;
                // This also means that we don't need the checkFilename.
            }

            if (addNodeToBranch) {
                // Ensure that the new name meets our requirements.
                String theComplaint = BranchHelperInterface.checkFilename(newName, NoteGroup.basePath(TodoNoteGroup.areaName));
                if (!theComplaint.isEmpty()) {
                    JOptionPane.showMessageDialog(jt, theComplaint,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Add the new list name to the tree
                dmtn.add(newList);
                theTreeModel.nodeStructureChanged(dmtn);
            }

            // Select the list.
            TreePath tp = getPath(dmtn);
            jt.expandPath(tp);
            jt.setSelectionPath(new TreePath(newList.getPath()));
        }
    } // end addNewList


    @SuppressWarnings("rawtypes") // Adding a type then causes 'unchecked' problem.
    private static MutableTreeNode getChild(DefaultMutableTreeNode dmtn, String name) {
        Enumeration children = dmtn.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode achild = (DefaultMutableTreeNode) children.nextElement();
            if (achild.toString().equals(name)) {
                return achild;
            }
        }
        return null;
    }

    // Called when adding a new list.  First, it trims any leading and trailing spaces.
    // Then it checks to see if the file already exists.  But rather than considering that
    // to be an error condition, we allow this situation to be a back-door selection method
    // rather than an 'Add'.  The only thing is - on a case-insensitive file system the file
    // may exist but not necessarily with the same casing as the name that was entered by
    // the user.  So - if we find that it does exist, we adopt that name and casing which
    // may be different.  After that, they can change the casing if desired, via the rename
    // mechanism.  But note that it would have to be a two-step process; a case-only name
    // change would look like a same-file conflict (unlike 'Add', the rename operation does
    // consider that to be an error).  So for an example of changing case via a rename:
    // 'upper' ==> 'UPPER' could be accomplished by 'upper' ==> 'upper1' ==> "UPPER".
    private static String nameAdjust(String name) {
        String adjustedName;
        adjustedName = name.trim();
        if (!adjustedName.isEmpty()) {
            String newNamedFile = MemoryBank.userDataHome + File.separatorChar + "TodoLists" + File.separatorChar;
//            newNamedFile = NoteGroup.basePath(areaName) + "todo_" + adjustedName + ".json";
            File f = new File(newNamedFile);
            if (f.exists()) {
                try {
                    String longCaseName = f.getCanonicalPath();
                    adjustedName = NoteGroup.prettyName(longCaseName);
                } catch (IOException ioe) {
                    System.out.println(ioe.getMessage());
                }
            }
        }
        return adjustedName;
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


    private static String chooseFileName() {
        int returnVal = filechooser.showDialog(null, "Merge");
        boolean badPlace = false;

        String s = filechooser.getCurrentDirectory().getAbsolutePath();
        String ems;

        // Check here to see if directory changed, reset if so.
        // System.out.println("Final directory: " + s);
        if (!s.equals(NoteGroup.basePath(areaName))) {
            filechooser.setCurrentDirectory(new File(NoteGroup.basePath(areaName)));
            badPlace = true;
        } // end if

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (badPlace) {
                // Warn user that they are not allowed to navigate.
                ems = "Navigation outside of your data directory is not allowed!";
                ems += "\n           " + "Merge" + " operation cancelled.";
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


    public static TreePath getPath(TreeNode treeNode) {
        List<Object> nodes = new ArrayList<>();
        if (treeNode != null) {
            nodes.add(treeNode);
            treeNode = treeNode.getParent();
            while (treeNode != null) {
                nodes.add(0, treeNode);
                treeNode = treeNode.getParent();
            }
        }

        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
    }

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
        String mergeFile = chooseFileName();
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


    // Call this method to do a 'programmatic' rename of a TodoList
    // node on the Tree, as opposed to doing it manually via the
    // TreeBranchEditor.  It operates only on the tree and not with
    // any corresponding files; you must do that separately.
    // See the 'Save As...' methodology for a good example.
    static void renameTodoListLeaf(String oldname, String newname) {
        boolean changeWasMade = false;
        JTree jt = AppTreePanel.theInstance.getTree();
        DefaultTreeModel tm = (DefaultTreeModel) jt.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) tm.getRoot();
        DefaultMutableTreeNode theTodoBranch = BranchHelperInterface.getNodeByName(theRoot,"To Do Lists");

        // The tree is set for single-selection, so the selection will not be a collection but
        // a single value.  Nonetheless, Swing only provides a get for min and max and either
        // one will work for us.  Note that the TreePath returned by getSelectionPath()
        // will probably NOT work for reselection after we do the rename, so we use the row.
        int returnToRow = jt.getMaxSelectionRow();

        int numLeaves = theTodoBranch.getChildCount();
        DefaultMutableTreeNode leafLink;

        leafLink = theTodoBranch.getFirstLeaf();

        // Search the leaves for the old name.
        while (numLeaves-- > 0) {
            String leaf = leafLink.toString();
            if (leaf.equals(oldname)) {
                String msg = "Renaming tree node from " + oldname;
                msg += " to " + newname;
                log.debug(msg);
                changeWasMade = true;
                leafLink.setUserObject(newname);
                break;
            } // end if

            leafLink = leafLink.getNextLeaf();
        } // end while

        if (!changeWasMade) return;

        // Force the renamed node to redisplay,
        // which also causes its deselection.
        tm.nodeStructureChanged(theTodoBranch);

        // Reselect this tree node.
        jt.setSelectionRow(returnToRow);

    } // end renameTodoListLeaf


    //-----------------------------------------------------------------
    // Method Name:  saveAs
    //
    // Called from the menu bar:
    // AppTreePanel.handleMenuBar() --> saveTodoListAs() --> saveAs()
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
        String theComplaint = BranchHelperInterface.checkFilename(newName, NoteGroup.basePath(areaName));
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
        String newFilename = NoteGroup.basePath(areaName) + "todo_" + newName + ".json";
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
    // Provided as support for a 'Save As' functionality.
    //  (By calling this first, then just calling 'save').  Any
    //  checking for validity is responsibility of calling context.
    //-----------------------------------------------------------------
    private void setFileName(String fname) {
        strTheGroupFilename = NoteGroup.basePath(areaName) + "todo_" + fname.trim() + ".json";
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


