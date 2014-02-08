/* ***************************************************************************
 * File:    AppTree.java
 * Author:  D. Lee Chastain
 ****************************************************************************/
/**
 The primary control for the Memory Bank application; provides a menubar at
 the top, a 'tree' control on the left, and a viewing pane on the right
 */

// Quick-reference notes:
// 
// MenuBar events        - actionPerformed() --> handleMenuBar().
// Tree Selection events - valueChanged() --> treeSelectionChanged() in a new thread.
//
// Management of search results should be rewritten to be closer to
//   the way to do lists are handled.

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

public class AppTree extends JPanel implements TreeSelectionListener {
    static final long serialVersionUID = 1L;
    private static Logger log = LoggerFactory.getLogger(AppTree.class);

    public static AppTree ltTheTree;
    private JFrame theFrame;

    private static final int LIST_GONE = -3; // used in constr, createTree

    private static AppMenuBar amb;
    protected SearchResultComponent src;   // SearchResultData
    protected ThreeMonthColumn tmc;        // DateSelection (interface)
    //-------------------------------------------------------------------

    private JTree tree;
    private DefaultTreeModel treeModel;
    private JScrollPane rightPane;

    private TreePath viewPath;
    private TreePath notePath;
    private TreePath todoPath;
    private static JDialog dlgWorkingDialog;

    private NoteGroup theNoteGroup; // A reference to the current selection
    private GoalPanel theGoalPanel;
    private DayNoteGroup theAppDays;
    private MonthNoteGroup theAppMonths;
    private YearNoteGroup theAppYears;
    private MonthView theMonthView;
    private YearView theYearView;
    private EventNoteGroup theEvents;
    private Vector<NoteData> noteDataVector;   // For searching
    private Vector<NoteData> foundDataVector;  // Search results
    private Vector<String> exportDataVector;
    private TodoListKeeper theTodoListKeeper;  // keeper of all loaded 'to do' lists.
    private SearchPanel spTheSearchPanel;
    private AppImage abbowt;
    private JSplitPane splitPane;

    private static String ems;               // Error Message String
    private static Date currentDateChoice;
    private DefaultMutableTreeNode theRootNode;
    private SearchResultNode nodeSearchResults;

    // Predefined Tree Paths
    private TreePath dayNotesPath;
    private TreePath monthNotesPath;
    private TreePath yearNotesPath;
    private TreePath monthViewPath;
    private TreePath weekViewPath;
    private TreePath upcomingEventsPath;

    private AppOptions appOpts;

    // Used in Search / fix
    private Object ob1kenoby;

    private boolean blnRestoringSelection;

    public AppTree(JFrame aFrame, AppOptions appOpts) {
        super(new GridLayout(1, 0));
        amb = new AppMenuBar();
        theFrame = aFrame;
        theFrame.setJMenuBar(amb);
        ltTheTree = this;
        this.appOpts = appOpts;

        // Make the 'Working...' dialog.
        dlgWorkingDialog = new JDialog(theFrame, "Working", true);
        JLabel lbl = new JLabel("Please Wait...");
        lbl.setFont(Font.decode("Dialog-bold-16"));
        String strWorkingIcon = MemoryBank.userDataDirPathName + File.separatorChar;
        strWorkingIcon += "icons/animated/const_anim.gif";
        lbl.setIcon(new AppIcon(strWorkingIcon));
        lbl.setVerticalTextPosition(JLabel.TOP);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        dlgWorkingDialog.add(lbl);
        dlgWorkingDialog.pack();
        dlgWorkingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        dlgWorkingDialog.setLocationRelativeTo(this);

        // Initialize the Search Panel from a new thread.
        nodeSearchResults = null;
        new Thread(new Runnable() {
            public void run() {
                spTheSearchPanel = new SearchPanel();
            }
        }).start();

        //---------------------------------------------------
        // Create the menubar handler, but fire it from a
        //   thread so we can quickly return and then if
        //   any of those items need to show the 'Working...'
        //   dialog, they will be able to.
        //---------------------------------------------------
        ActionListener al = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                final String what = ae.getActionCommand();
                new Thread(new Runnable() {
                    public void run() {
                        handleMenuBar(what);
                    }
                }).start(); // Start the thread
            }
        };
        //---------------------------------------------------

        //---------------------------------------------------------
        // Add the above handler to all menu items.
        //---------------------------------------------------------
        // Note - if you need cascading menus in the future, use
        //   the recursive version of this as implemented in
        //   LogPane.java, a now archived predecessor to AppTree.
        //---------------------------------------------------------
        int numMenus = amb.getMenuCount();
        // MemoryBank.debug("Number of menus found: " + numMenus);
        for (int i = 0; i < numMenus; i++) {
            JMenu jm = amb.getMenu(i);
            if (jm == null) continue;

            for (int j = 0; j < jm.getItemCount(); j++) {
                JMenuItem jmi = jm.getItem(j);
                if (jmi == null) continue; // Separator
                jmi.addActionListener(al);
            } // end for j
        } // end for i
        //---------------------------------------------------------

        setOpaque(true);

        MemoryBank.update("Recreating the previous Tree configuration");
        createTree();  // Create the tree.

        // Listen for when the selection changes.
        // We need to do this now so that the proper initialization
        //   occurs when we restore the previous selection, below.
        tree.addTreeSelectionListener(this);

        // Create the scroll pane and add the tree to it.
        JScrollPane treeView = new JScrollPane(tree);

        // Create the viewing pane and start with the 'about' graphic.
        abbowt = new AppImage(MemoryBank.logHome + "/images/ABOUT.gif", false);
        JPanel aboutPanel = new JPanel(new GridBagLayout());
        aboutPanel.add(abbowt); // Nested the image in a panel with a flexible layout, for centering.
        rightPane = new JScrollPane(aboutPanel);

        // Add the scroll panes to a split pane.
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(treeView);
        if (appOpts.paneSeparator > 0) splitPane.setDividerLocation(appOpts.paneSeparator);
        else splitPane.setDividerLocation(140);

        // Do not let focus escape from the right side.
        tree.setFocusable(false);
        treeView.setFocusable(false);

        splitPane.setRightComponent(rightPane);

        // Only the location of the Divider seems to care about the
        //   minimum width.  Otherwise, as long as the frame is resizable
        //   it can get as small as Windows allows.
        Dimension minimumSize = new Dimension(100, 50);
        rightPane.setMinimumSize(minimumSize);
        treeView.setMinimumSize(minimumSize);

        splitPane.setPreferredSize(new Dimension(820, 520));

        // Add the split pane to this panel.
        add(splitPane);

        currentDateChoice = new Date();

        // Restore the last selection.
        MemoryBank.update("Restoring the previous selection");
        blnRestoringSelection = true;
        if (appOpts.theSelectionRow >= 0)
            tree.setSelectionRow(appOpts.theSelectionRow);
        else {
            if (appOpts.theSelectionRow == LIST_GONE) {
                appOpts.theSelectionRow = tree.getRowForPath(todoPath);
                tree.setSelectionRow(appOpts.theSelectionRow);
            } // end if
            // Note - the 'else' here is that there was not a selection;
            //  ie, the first time run.  In that case, leave as-is.
        } // end if

        blnRestoringSelection = false;
    } // end constructor for AppTree


    // Adds a search result branch to the tree.
    //   s = the visible name
    //   i = number of results
    public void addSearchResult(String s, int i) {
        // Remove the tree selection listener while we
        //   rebuild this portion of the tree.
        tree.removeTreeSelectionListener(this);

        // Make a new tree node for the result file whose path
        //   is given in the input parameter
        DefaultMutableTreeNode tmpNode;
        tmpNode = new SearchResultNode(s, i);

        // Check to see if the 'Search Results' node has been
        //   created.  If not then create and add it now.
        if (nodeSearchResults == null) {
            // Preserve current branch expansions
            updateTreeState(false);

            nodeSearchResults = new SearchResultNode(null, 0);
            theRootNode.add(nodeSearchResults);

            // Display the new node in the tree
            resetTreeState();
        } // end if

        // Add to the tree under the Search Results branch
        nodeSearchResults.add(tmpNode);
        treeModel.nodeStructureChanged(nodeSearchResults);

        // Select the new list.
        TreeNode[] pathToRoot = tmpNode.getPath();
        tree.addTreeSelectionListener(this);
        tree.setSelectionPath(new TreePath(pathToRoot));
    } // end addSearchResult


    //-------------------------------------------------------
    // Method Name: closeSearchResult
    //
    // Called from the 'Close' button in the header of the
    //   SearchResultGroup that is currently being displayed.
    //
    // Removes the associated results file and then calls the
    //   method to remove the search result leaf.
    //-------------------------------------------------------
    public void closeSearchResult() {
        // Obtain a reference to the current tree selection.
        SearchResultNode node = (SearchResultNode)
                tree.getLastSelectedPathComponent();
        if (node == null) return;

        // Give a confirmation warning here, allowing the user to cancel.
        // If there are nodes below here, list them and explain in the
        //   warning that they will also be removed.
        String string1 = "Yes, remove";
        String string2 = "Cancel";
        String theWarning = "Closing these search results will remove them from \n"
                + "the tree and you will not be able to restore them.  Even with \n"
                + "a new, identical search you may not see these same results, \n"
                + "if any information has changed since this search was \n"
                + "originally performed.  Are you sure?";
        Object[] options = {string1, string2};
        if (node.getChildCount() > 0) {
            // customize the text to list the child nodes that will be removed.
            // Not needed until child nodes are possible.
            System.out.println("Problem! Child nodes detected under a branch to be removed.");
        } // end if
        int choice = JOptionPane.showOptionDialog(this,
                theWarning,
                "Confirm the 'Close' action",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,     //don't use a custom Icon
                options,  //the titles of buttons
                string2); //the title of the default button
        if (choice != JOptionPane.OK_OPTION) return;

        // Remove the results file
        File f = new File(node.strFileName);
        if (f.exists()) {
            if (!f.delete()) System.out.println("Failed to remove " + node.strFileName);
        }

        removeSearchNode(node);
    } // end closeSearchResult


    //-----------------------------------------------------------------
    // Method Name: createTree
    //
    // Creates the 'MemoryBank' tree.
    //-----------------------------------------------------------------
    private void createTree() {
        int intRowCounter = 0;  // The root
        int intViewRow;  // Used in expansion state retention
        int intNoteRow;  // Used in expansion state retention
        int intTodoRow;  // Used in expansion state retention

        MemoryBank.debug("Creating the tree");
        DefaultMutableTreeNode trunk = new DefaultMutableTreeNode("App");
        theRootNode = trunk;

        DefaultMutableTreeNode branch;
        DefaultMutableTreeNode leaf;
        TreeNode[] pathToRoot;

        leaf = new DefaultMutableTreeNode("Upcoming Events");
        trunk.add(leaf);
        pathToRoot = leaf.getPath();
        upcomingEventsPath = new TreePath(pathToRoot);

        intRowCounter++;

        leaf = new DefaultMutableTreeNode("Goals");
        trunk.add(leaf);
        intRowCounter++;

        //---------------------------------------------------
        // Graphical views
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode("Views");
        trunk.add(branch);
        intRowCounter++;
        intViewRow = intRowCounter;

        leaf = new DefaultMutableTreeNode("Year View");
        branch.add(leaf);

        leaf = new DefaultMutableTreeNode("Month View");
        branch.add(leaf);

        // Convert this node to a TreePath to be used later,
        //   in selection events.  With expandable branches,
        //   the selection row can be variable and is not a good
        //   way to specify the change.  This conversion is done
        //   again for week and day, below (without this comment).
        pathToRoot = leaf.getPath();
        monthViewPath = new TreePath(pathToRoot);

        leaf = new DefaultMutableTreeNode("Week View");
        branch.add(leaf);
        pathToRoot = leaf.getPath();
        weekViewPath = new TreePath(pathToRoot);
        //---------------------------------------------------

        //---------------------------------------------------
        // NoteGroup types
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode("Notes");
        trunk.add(branch);
        intRowCounter++;
        intNoteRow = intRowCounter;

        leaf = new DefaultMutableTreeNode("Day Notes");
        branch.add(leaf);
        pathToRoot = leaf.getPath();
        dayNotesPath = new TreePath(pathToRoot);

        leaf = new DefaultMutableTreeNode("Month Notes");
        branch.add(leaf);
        pathToRoot = leaf.getPath();
        monthNotesPath = new TreePath(pathToRoot);

        leaf = new DefaultMutableTreeNode("Year Notes");
        branch.add(leaf);
        pathToRoot = leaf.getPath();
        yearNotesPath = new TreePath(pathToRoot);

        //---------------------------------------------------
        // To Do Lists
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode("To Do Lists");
        String theName;
        theTodoListKeeper = new TodoListKeeper();
        trunk.add(branch);
        intRowCounter++;
        intTodoRow = intRowCounter;

        for (String s : appOpts.todoLists) {

            // First check to see that the file is 'here'.
            theName = MemoryBank.userDataDirPathName + File.separatorChar + s + ".todolist";
            if (new File(theName).exists()) {
                MemoryBank.debug("  Adding List: " + s);

                // Add to the tree
                leaf = new DefaultMutableTreeNode(s);
                branch.add(leaf);

            } else { // List not found.
                MemoryBank.debug("  The " + s + " file was not found");
                if (s.equals(appOpts.theSelection)) {
                    appOpts.theSelection = "To Do Lists";
                    appOpts.theSelectionRow = LIST_GONE;
                } // end if
            } // end if
        } // end for
        //--------------------

        leaf = new DefaultMutableTreeNode("Icon Manager");
        trunk.add(leaf);
        //intRowCounter++;

        // Restore previous search results, if any.
        if (appOpts.searchResults != null) {
            nodeSearchResults = appOpts.searchResults;
            trunk.add(nodeSearchResults);
        } // end if

        // Create a default model based on the 'App' node that
        //   we've been growing, and create the tree from that model.
        treeModel = new DefaultTreeModel(trunk);
        tree = new JTree(treeModel);

        // At creation, all paths are collapsed.  This is how we know
        //   the (initial) row for the three expandable branches.
        // We know the rows at this moment but later it can change, so
        //   obtain a reliable stable reference to these nodes.
        viewPath = tree.getPathForRow(intViewRow);
        notePath = tree.getPathForRow(intNoteRow);
        todoPath = tree.getPathForRow(intTodoRow);

        // Expand branches based on last saved configuration.
        resetTreeState();

        // Set to single selection mode.
        tree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Do not show the 'Log' root of the tree.
        tree.setRootVisible(false);

        // But do show the link that all children have to it.
        tree.setShowsRootHandles(true);
    } // end createTree

    //----------------------------------------------------------------
    // Method Name:  deepClone
    //
    // Used to fully clone a tree node, since the system-provided
    // method only does the first level.
    //----------------------------------------------------------------
    @SuppressWarnings("rawtypes") // Adding a type then causes 'unchecked' problem.
    public static DefaultMutableTreeNode deepClone(DefaultMutableTreeNode root){
        DefaultMutableTreeNode newRoot = (DefaultMutableTreeNode)root.clone();
        for(Enumeration childEnum = root.children(); childEnum.hasMoreElements();){
            newRoot.add(deepClone((DefaultMutableTreeNode)childEnum.nextElement()));
        }
        return newRoot;
    }

    private void doExport() {
        showWorkingDialog(true); // Show the 'Working...' dialog

        // Make sure that the most recent changes, if any,
        //   will be included in the export.
        if (theNoteGroup != null) {
            theNoteGroup.preClose();
        } // end if

        // Now make a Vector that can collect the search results.
        exportDataVector = new Vector<String>(0, 1);

        // Now scan the user's data area for data files -
        // We do a recursive directory search and each
        //   file is examined as soon as it is found,
        //   provided that it passes the file-level filters.
        MemoryBank.debug("Data location is: " + MemoryBank.userDataDirPathName);
        File f = new File(MemoryBank.userDataDirPathName);
        exportDataDir(f, 0); // Indirectly fills the exportDataVector
        writeExportFile();

        showWorkingDialog(false);
    } // end doExport


    //--------------------------------------------------------
    // Method Name:  exportDataDir
    //
    // This method scans a directory for data files.  If it
    //   finds a directory rather than a file, it will
    //   recursively call itself for that directory.
    //--------------------------------------------------------
    private void exportDataDir(File theDir, int level) {
        MemoryBank.dbg("Scanning " + theDir.getName());

        File theFiles[] = theDir.listFiles();
        assert theFiles != null;
        int howmany = theFiles.length;
        MemoryBank.debug("\t\tFound " + howmany + " data files");
        MemoryBank.debug("Level " + level);

        boolean goLook;
        for (File theFile1 : theFiles) {
            goLook = false;
            String theFile = theFile1.getName();
            if (theFile1.isDirectory()) {
                if (theFile.equals("Archives")) continue;
                if (theFile.equals("icons")) continue;
                exportDataDir(theFile1, level + 1);
            } else {
                if (theFile.equals("UpcomingEvents")) goLook = true;
                if (theFile.endsWith(".todolist")) goLook = true;
                if ((theFile.startsWith("D")) && (level > 0)) goLook = true;
                if ((theFile.startsWith("M")) && (level > 0)) goLook = true;
                if ((theFile.startsWith("Y")) && (level > 0)) goLook = true;
            } // end if / else

            if (goLook) exportDataFile(theFile1);
        }//end for i
    }//end exportDataDir


    //---------------------------------------------------------
    // Method Name: exportDataFile
    //
    //---------------------------------------------------------
    private void exportDataFile(File dataFile) {
        MemoryBank.debug("Searching: " + dataFile.getName());
        noteDataVector = new Vector<NoteData>();
        loadNoteData(dataFile);

        // Construct an Excel-readable string for every Note
        for (NoteData ndTemp : noteDataVector) {
            String multiline = convertLinefeeds(ndTemp.extendedNoteString);

            String s = ndTemp.noteString + "|";
            s += multiline + "|";
            s += ndTemp.subjectString;

            // Get the Date for this note, if available
            Date dateTmp = ndTemp.getNoteDate();
            if (dateTmp == null) dateTmp = AppUtil.getDateFromFilename(dataFile);

            if (null != dateTmp) s += "|" + dateTmp.toString();
            exportDataVector.add(s);

        } // end for
    }//end exportDataFile


    // This mechanism will be used so that a multiline string
    // may be exported on a single line for Excel import.  Then
    // it will be subject to a (user-initiated) global search
    // and replace (replacing with alt-0010).
    private String convertLinefeeds(String extendedNoteString) {
        String retVal = "";
        if (null == extendedNoteString) return retVal;
        if (extendedNoteString.trim().equals("")) return retVal;

        retVal = extendedNoteString.replaceAll("\n", "[linefeed]");

        return retVal;
    }

    public TodoListKeeper getTodoListKeeper() { return theTodoListKeeper; }
    public JTree getTree() { return tree; }

    private void handleMenuBar(String what) {
        if (what.equals("Exit")) System.exit(0);
        else if (what.equals("About")) showAbout();
        else if (what.equals("Add New List...")) TodoBranchHelper.addNewList(tree);
        else if (what.equals("Close")) closeSearchResult();
        else if (what.equals("Clear Day")) theAppDays.clearGroupData();
        else if (what.equals("Clear Month")) theAppMonths.clearGroupData();
        else if (what.equals("Clear Year")) theAppYears.clearGroupData();
        else if (what.equals("Clear Entire List")) theNoteGroup.clearGroupData();
        else if (what.equals("Contents")) showHelp();
        else if (what.equals("Export")) doExport();
        else if (what.equals("Search...")) showSearchDialog();
        else if (what.equals("Set Options...")) ((TodoNoteGroup) theNoteGroup).setOptions();
        else if (what.startsWith("Merge")) ((TodoNoteGroup) theNoteGroup).merge();
        else if (what.startsWith("Print")) ((TodoNoteGroup) theNoteGroup).printList();
        else if (what.equals("Refresh")) theEvents.refresh();
        else if (what.equals("Review...")) System.out.println("Review was selected.");
        else if (what.startsWith("Save As")) saveTodoListAs();
        else if (what.equals("Today")) showToday();
        else if (what.equals("Set Look and Feel...")) showPlafDialog();  // move to MemoryBank?
        else if (what.equals("undo")) {
            String s = appOpts.theSelection;
            if (s.equals("Day Notes")) theAppDays.recalc();
            else if (s.equals("Month Notes")) theAppMonths.recalc();
            else if (s.equals("Year Notes")) theAppYears.recalc();
            else theNoteGroup.updateGroup(); // reload without save
        } else {
            AppUtil.localDebug(true);
            MemoryBank.debug("  " + what);
            AppUtil.localDebug(false);
        } // end if/else
    } // end handleMenuBar

    private void showPlafDialog() {
        PlafEditorPanel pep = new PlafEditorPanel();
        int doit = JOptionPane.showConfirmDialog(
                theFrame, pep,
                "Select a new Look and Feel", JOptionPane.OK_CANCEL_OPTION);

        if (doit == -1) return; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return;

        // This is where we would set the options...
        //boolean blnOrigShowPriority = myVars.showPriority;
        //myVars = to.getValues();

        try {
            UIManager.setLookAndFeel(pep.getSelectedPlaf());
            appOpts.thePlaf = pep.getSelectedPlaf();
            SwingUtilities.updateComponentTreeUI(theFrame);
            // It looks like a nullPointerException stack trace is being printed as a result of
            // the above command, which seems to complete successfully anyway, after that.
            // This exception is not getting trapped by the catch section below.
            // I think it may be happening because of my implementation of the custom
            // scrollpane (need to find/review that code).  Seems to go thru without any other
            // trouble, tho, so we may be able to ignore this indefinitely.
            //System.out.println("updatedComponentTreeUI"); // Shows that the above succeeded.
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            //e.printStackTrace();
        }
    }


    // This is a 'generic' NoteData loader that can handle the loading
    //   of data for ANY NoteGroup child class.  It is used only for
    //   searching (although it may retain one additional 'object', in
    //   support of the 'fixit' mechanism).
    private void loadNoteData(File theDayFile) {
        Exception e = null;
        Object tempObject = null;
        NoteData tempNoteData;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        int index = 0;
        String s;

        ob1kenoby = null;
        try {
            fis = new FileInputStream(theDayFile);
            ois = new ObjectInputStream(fis);

            // The assumption is that there is at
            //  least one element of data.
            while (true) {
                try {
                    tempObject = ois.readObject();
                    tempNoteData = (NoteData) tempObject;

                    s = tempObject.getClass().getName();
                    MemoryBank.debug("    loaded " + s + " at index " + index);
                    index++;
                    noteDataVector.addElement(tempNoteData);
                } catch (ClassCastException cce) {
                    // If this is not a CalendarNoteGroup, the first data element will be
                    //   the group properties and not a NoteData.  In that case, we can
                    //   hold onto it for saving later, and continue on; otherwise
                    //   we have a problem.
                    if (index == 0) {
                        ob1kenoby = tempObject;
                    } else {
                        e = cce;
                        break;
                    } // end if
                } // end try/catch
            }//end while
        } catch (ClassNotFoundException cnfe) {
            e = cnfe;
        } catch (InvalidClassException ice) {
            e = ice;
        } catch (FileNotFoundException fnfe) {
            e = fnfe;
        } catch (EOFException eofe) { // Normal, expected.
        } catch (IOException ioe) {
            e = ioe;
        } finally {
            try {
                if (ois != null) ois.close();
                if (fis != null) fis.close();
            } catch (IOException ioe) {
                System.out.println("Exception: " + ioe.getMessage());
            } // end try/catch
        }//end try/catch

        if (e != null) {
            // e.printStackTrace();
            ems = "Error in loading " + theDayFile.getName() + " !\n";
            ems = ems + e.toString();
            ems = ems + "\nNote data load operation aborted.";
            JOptionPane.showMessageDialog(this,
                    ems, "Error", JOptionPane.ERROR_MESSAGE);

        }//end if
    }//end loadNoteData


    //------------------------------------------------------------
    // Method Name:  preClose
    //
    //------------------------------------------------------------
    public void preClose() {
        // Only for the currently active NoteGroup; any others would
        //   have been saved when the view changed away from them.
        if (theNoteGroup != null) theNoteGroup.preClose();

        updateTreeState(true); // Update appOpts
        saveOpts();
    } // end preClose


    private void removeSearchNode(SearchResultNode node) {
        // Remove these results from the tree but do not let
        //   the removal result in the need to process another
        //   selection event.
        tree.removeTreeSelectionListener(this);
        node.removeFromParent();
        treeModel.nodeStructureChanged(nodeSearchResults);
        tree.addTreeSelectionListener(this);

        // Do not attempt to save this one prior
        //   to changing the tree selection.
        theNoteGroup = null;

        // Select the Search Results branch
        TreeNode[] pathToRoot = nodeSearchResults.getPath();
        tree.setSelectionPath(new TreePath(pathToRoot));

        // Check to see if any Search Results are left; if not then:
        if (nodeSearchResults.getChildCount() == 0) {
            //   Remove the 'Search Results' node.
            tree.removeTreeSelectionListener(this);
            theRootNode.remove(nodeSearchResults);
            nodeSearchResults = null;
            resetTreeState();
            tree.addTreeSelectionListener(this);

            // Remove any extraneous '.sresults' files.
            MemoryBank.debug("Data location is: " + MemoryBank.userDataDirPathName);
            File theDir = new File(MemoryBank.userDataDirPathName);
            File theFiles[] = theDir.listFiles();
            assert theFiles != null;
            int howmany = theFiles.length;
            MemoryBank.debug("\t\tFound " + howmany + " data files");
            for (File theFile1 : theFiles) {
                String theFile = theFile1.getName();
                if (theFile1.isFile()) {
                    if (theFile.endsWith(".sresults")) {
                        if (!theFile1.delete()) System.out.println("Failed to remove " + theFile);
                    }
                } // end if
            } // end for

            // With no more search results remaining, display the 'About' view.
            showAbout();
        } // end if
    } // end removeSearchNode


    //------------------------------------------------------------------
    // Method Name: resetTreeState
    //
    // Call this method after changes have been made to the tree, so
    //   that they will be displayed while retaining original branch
    //   expansions.
    //------------------------------------------------------------------
    private void resetTreeState() {
        treeModel.nodeStructureChanged(theRootNode); // collapses branches

        // Expand branches based on last configuration.
        if (appOpts.ViewsExpanded) tree.expandPath(viewPath);
        if (appOpts.NotesExpanded) tree.expandPath(notePath);
        if (appOpts.TodoListsExpanded) tree.expandPath(todoPath);

        if (nodeSearchResults != null) {
            if (nodeSearchResults.blnExpanded) {
                // Reset the expansion state of the Search Results
                TreeNode[] pathToRoot = nodeSearchResults.getPath();
                TreePath path = new TreePath(pathToRoot);
                tree.expandPath(path);
            }
        }

    } // end resetTreeState


    //--------------------------------------------------------------
    // Method Name: saveNoteData
    //
    // This is a 'generic' NoteData save method that can handle the saving
    //   of data for ANY NoteGroup child class.  It is used to save
    //   search results as well as in support of the 'fixit' mechanism.
    // The 'object' at index zero may be the properties of the Group.  If
    //   it has none then prior to calling this method ob1kenoby should
    //   be set to null; otherwise it should be that properties object.
    //--------------------------------------------------------------
    private int saveNoteData(File theFile) {

        try {
            FileOutputStream fos = new FileOutputStream(theFile);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            // Some NoteGroups have a 'properties' object as
            //   their first item in the file.
            if (ob1kenoby != null) {
                oos.writeObject(ob1kenoby);
                ob1kenoby = null;
            } // end if

            for (NoteData tempData : noteDataVector) {
                oos.writeObject(tempData);
            }//end for i
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException ioe) {
            ioe.printStackTrace(System.err);
            return 1; // Problem
        }//end try/catch

        return 0;  // Success
    }//end saveNoteData


    //------------------------------------------------------------------------
    // Method Name:  saveOpts
    //
    //------------------------------------------------------------------------
    private void saveOpts() {
        String FileName = MemoryBank.userDataDirPathName + File.separatorChar + "app.options";
        MemoryBank.debug("Saving application option data in " + FileName);

        try {
            FileOutputStream fos = new FileOutputStream(FileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(appOpts);
            oos.flush();
            oos.close();
            fos.close();
        } catch (IOException ioe) {
            // This method is only called internally from preClose.  Since
            // preClose is to be called via a shutdown hook that is not going to
            // wait around for the user to 'OK' an error dialog, any error in saving
            // will only be reported in a printout via MemoryBank.debug because
            // otherwise the entire process will hang up waiting for the user's 'OK'
            // on the dialog that will NOT be showing.

            // A normal user will not see the debug error printout but
            // they will most likely see other popups such as filesystem full, access
            // denied, etc, that a sysadmin type can resolve for them, that will
            // also fix this issue.
            ems = ioe.getMessage();
            ems = ems + "\nMemory Bank options save operation aborted.";
            MemoryBank.debug(ems);
            // This popup caused a hangup and the vm had to be 'kill'ed.
            // JOptionPane.showMessageDialog(null,
            //    ems, "Error", JOptionPane.ERROR_MESSAGE);
            // Yes, even though the parent was null.
        } // end try/catch
    } // end saveOpts


    //------------------------------------------------------------------------
    // Method Name:  saveTodoListAs
    //
    //------------------------------------------------------------------------
    private void saveTodoListAs() {
        String oldName = theNoteGroup.getName();
        if(((TodoNoteGroup) theNoteGroup).saveAs()) {
            String newName = theNoteGroup.getName();

            // When the tree selection changes, any open NoteGroup is automatically saved,
            // and the tree selection will change automatically when we do the rename of
            // the leaf on the tree below.  But in this case we do not want that behavior,
            // because we have already saved the file, milliseconds ago.  It wouldn't hurt
            // to let it save again, but why allow it, when all it takes to stop it is:
            theNoteGroup = null;

            // Removal from the TodoListKeeper is needed, to force a file reload
            // during the rename of the leaf (below), because even though the saveAs
            // operation changed the name of the list held by the todoListKeeper, it
            // still shows a title that was developed from the old file name.
            // Reloading from the file with the new name will fix that.
            theTodoListKeeper.remove(newName);

            // Rename the leaf, refresh the To Do branch, and reselect the same tree
            // row to cause a reload and redisplay of the list.  Note that not only
            // does the leaf name change, but the reload also changes the list title.
            TodoBranchHelper.renameTodoListLeaf(oldName, newName);
        }
    }

    //--------------------------------------------------------
    // Method Name:  scanDataDir
    //
    // This method scans a directory for data files.  If it
    //   finds a directory rather than a file, it will
    //   recursively call itself for that directory.
    //
    // The SearchPanel interface follows a 'filter out'
    //   plan.  Similarly, this method starts with the
    //   idea that all files will be searched and then
    //   considers the filters, to eliminate candidate
    //   files.  If a file is not eliminated after the
    //   filters have been considered, the search
    //   method is called.
    //--------------------------------------------------------
    private void scanDataDir(File theDir, int level) {
        MemoryBank.dbg("Scanning " + theDir.getName());

        File theFiles[] = theDir.listFiles();
        assert theFiles != null;
        int howmany = theFiles.length;
        MemoryBank.debug("\t\tFound " + howmany + " data files");
        boolean goLook;
        Date dateNoteDate;
        MemoryBank.debug("Level " + level);

        for (File theFile1 : theFiles) {
            String theFile = theFile1.getName();
            if (theFile1.isDirectory()) {
                if (theFile.equals("Archives")) continue;
                if (theFile.equals("icons")) continue;
                scanDataDir(theFile1, level + 1);
            } else {
                goLook = true;
                //dateNoteDate = null;
                if (theFile.equals("Goals")) {
                    if (!spTheSearchPanel.searchGoals()) {
                        goLook = false;
                    }
                } else if (theFile.equals("UpcomingEvents")) {
                    if (!spTheSearchPanel.searchEvents()) {
                        goLook = false;
                    }
                } else if (theFile.endsWith(".todolist")) {
                    if (!spTheSearchPanel.searchLists()) {
                        goLook = false;
                    }
                } else if ((theFile.startsWith("D")) && (level > 0)) {
                    if (!spTheSearchPanel.searchDays()) goLook = false;
                } else if ((theFile.startsWith("M")) && (level > 0)) {
                    if (!spTheSearchPanel.searchMonths()) goLook = false;
                } else if ((theFile.startsWith("Y")) && (level > 0)) {
                    if (!spTheSearchPanel.searchYears()) goLook = false;
                } else { // Files that do not contain Notes.
                    goLook = false;
                } // end if / else if

                // Check the Note date, possibly filter out based on 'when'.
                if (goLook) {
                    dateNoteDate = AppUtil.getDateFromFilename(theFile1);
                    if (dateNoteDate != null) {
                        if (!spTheSearchPanel.filterWhen(dateNoteDate)) goLook = false;
                    } // end if
                } // end if


                // The Last Modified date of the FILE is not necessarily
                //   the same as the Note, but it CAN be considered
                //   when looking for a last mod AFTER a certain date,
                //   because the last mod to ANY note in the file CANNOT
                //   be later than the last mod to the file itself.
                if (goLook) {
                    Date dateLastMod = new Date(theFile1.lastModified());
                    if (spTheSearchPanel.getLastModSetting() == SearchPanel.AFTER) {
                        if (!spTheSearchPanel.filterLastMod(dateLastMod)) goLook = false;
                    } // end if
                } // end if

                if (goLook) {
                    searchDataFile(theFile1);
                } // end if
            } // end if
        }//end for i
    }//end scanDataDir


    //---------------------------------------------------------
    // Method Name: searchDataFile
    //
    // File-level (but not item-level) date filtering will
    //   have been done prior to this method being called.
    //---------------------------------------------------------
    private void searchDataFile(File dataFile) {
        MemoryBank.debug("Searching: " + dataFile.getName());
        noteDataVector = new Vector<NoteData>();
        loadNoteData(dataFile);

        //  if(true) return; // early bailout; if you only needed to load.

        //----------------------------------------------------------------
        // The 'fixit' mechanism -
        //----------------------------------------------------------------
        // A search will not normally make a change to the data.  However,
        //   this search mechanism has evolved from earlier MemoryBank versions
        //   that were themselves evolving and in the process, sometimes
        //   needed their data to be 'fixed'.  This fix facility should
        //   be kept, as a way to easily migrate data from one version to
        //   the next, as the MemoryBank program continues to evolve.  To use,
        //   simply detect whatever changes are needed (the 'if' condition),
        //   then make the changes and set the flag to true.
        // This code will remain here but disabled in a comment block, until needed.
        /* * /
        boolean madeChange = false;
        for (NoteData tempNoteData : noteDataVector) {
            if (some condition,if not ALL){
                <change the data >
                        madeChange = true;
            } // end if
        } // end for

        // Later, after one or more complete searches have run without
        //   seeing either of the printouts below, this entire section
        //   may be commented out until it is needed again.
        if (madeChange) {
            int result = saveNoteData(dataFile);
            if (result == 0)
                System.out.println("Saved modified file: " + dataFile.getName());
            else {
                System.out.println("Problem saving modified file: " + dataFile.getName());
                System.exit(1);
            }//end else
        }//end if
        //----------------------------------------------------------------
        /* */

        // Now get on with the search -
        for (NoteData ndTemp : noteDataVector) {

            if (spTheSearchPanel.foundIt(ndTemp)) {
                // Add this note (NOT the entire group) to the search results.

                SearchResultData srd = new SearchResultData(ndTemp);

                // The copy constructor used above will preserve the
                //   dateLastMod of the original note.  Members specific
                //   to a SearchResultData must be set explicitly.
                srd.setFileFoundIn(dataFile);

                // Get the Date for this note, if available
                Date dateTmp = ndTemp.getNoteDate();
                if (dateTmp == null) dateTmp = AppUtil.getDateFromFilename(dataFile);

                // It may still be null, but set it for this result
                srd.setNoteDate(dateTmp);

                foundDataVector.add(srd);

                // System.out.println(ndTemp.getNoteString());
            } // end if
        } // end for
    }//end searchDataFile


    //--------------------------------------------------------------
    // Method Name:  showAbout
    //
    // This method will put the 'About' graphic into the right
    //   side of the display.  However, if invoked a second time
    //   without any other tree selection in between, it will
    //   restore the original selection.  To implement this
    //   'toggle' functionality, it is also necessary to capture
    //   and restore the expanded/collapsed state, possibly
    //   overriding the user's more recent settings if they had
    //   made such changes without also making a new selection.
    //--------------------------------------------------------------
    public void showAbout() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                tree.getLastSelectedPathComponent();

        if (node == null) {
            // Reset tree to the state it was in before.
            if (appOpts.ViewsExpanded) tree.expandPath(viewPath);
            else tree.collapsePath(viewPath);
            if (appOpts.NotesExpanded) tree.expandPath(notePath);
            else tree.collapsePath(notePath);
            if (appOpts.TodoListsExpanded) tree.expandPath(todoPath);
            else tree.collapsePath(todoPath);
            tree.setSelectionRow(appOpts.theSelectionRow);
        } else {
            // Capture the current state; we may have to 'toggle' back to it.
            updateTreeState(false);
            tree.clearSelection();

            // A 'nesting' trick, to get the image centered in the JScrollPane
            JPanel jp = new JPanel(new GridBagLayout());
            jp.add(abbowt);
            rightPane.setViewportView(jp);

            amb.manageMenus("");
        } // end if
    } // end showAbout


    // Called from YearView or MonthView, mouse dbl-click on date.
    public void showDay() {
        MemoryBank.debug("showDay called.");
        tree.setSelectionPath(dayNotesPath);
    } // end showDay


    //------------------------------------------------------------
    // Method Name:  showFoundIn
    //
    //------------------------------------------------------------
    public void showFoundIn(SearchResultData srd) {
        // Determine the treepath to be shown, based on
        //   the result's file name/path.
        String fname = srd.getFileFoundIn().getName();
        String fpath = srd.getFileFoundIn().getParent();

        if (fname.endsWith(".todolist")) {
            String pn = TodoNoteGroup.prettyName(fname);
            if (!(srd.getFileFoundIn()).exists()) {
                String s;
                s = "Error in loading " + pn + " !\n";
                s += "The original list no longer exists.";
                JOptionPane.showMessageDialog(this,
                        s, "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                // The beauty of this action is that the path we are
                // selecting does not actually have to be present
                // for this to work and display the list.
                tree.setSelectionPath(TodoBranchHelper.getTodoPathFor(tree, pn));
            } // end if
        } else if (fname.equals("UpcomingEvents")) {
            tree.setSelectionPath(upcomingEventsPath);
        } else if (!fpath.endsWith("MemoryBank")) {
            // If the path does not end at the top level data
            //   directory, then (at least at this writing) it
            //   means that we are down one of the calendar-
            //   based 'Year' paths.

            // Note that we should be able to utilize srd.getNoteDate()
            //   here, rather than having to develop it from the filename.
            //   need to look into why that is not working...
            if (fname.startsWith("Y")) {
                currentDateChoice = AppUtil.getDateFromFilename(srd.getFileFoundIn());
                tree.setSelectionPath(yearNotesPath);
            } else if (fname.startsWith("M")) {
                currentDateChoice = AppUtil.getDateFromFilename(srd.getFileFoundIn());
                tree.setSelectionPath(monthNotesPath);
            } else if (fname.startsWith("D")) {
                currentDateChoice = AppUtil.getDateFromFilename(srd.getFileFoundIn());
                tree.setSelectionPath(dayNotesPath);
            } // end if
        } // end if

        AppUtil.localDebug(false);
    } // end showFoundIn


    public void showHelp() {
        try {
            String s = MemoryBank.logHome + File.separatorChar;
            Runtime.getRuntime().exec("hh " + s + "MemoryBank.chm");
        } catch (IOException ioe) {
            MemoryBank.debug(ioe.getMessage());
        } // end try/catch
    } // end showHelp


    public void showMonth() {
        MemoryBank.debug("showMonth called.");
        // This method is called from an external context.
        tree.setSelectionPath(monthViewPath);
    } // end showMonth


    private void showSearchDialog() {
        Frame theFrame = JOptionPane.getFrameForComponent(this);

        // Now display the dialog.
        String string1 = "Search Now";
        String string2 = "Cancel";
        Object[] options = {string1, string2};
        int choice = JOptionPane.showOptionDialog(theFrame,
                spTheSearchPanel,
                "Search - Please specify the conditions for your quest",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,     //don't use a custom Icon
                options,  //the titles of buttons
                string1); //the title of the default button

        if (choice != JOptionPane.OK_OPTION) return;

        if (!spTheSearchPanel.hasWhere()) {
            JOptionPane.showMessageDialog(this,
                    " No location to search was chosen!",
                    "Search conditions specification error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        } // end if no search location was specified.

        showWorkingDialog(true); // Show the 'Working...' dialog

        // Make sure that the most recent changes, if any,
        //   will be included in the search.
        if (theNoteGroup != null) {
            theNoteGroup.preClose();
        } // end if

        // Now make a Vector that can collect the search results.
        foundDataVector = new Vector<NoteData>(0, 1);

        // Now scan the user's data area for data files -
        // We do a recursive directory search and each
        //   file is examined as soon as it is found,
        //   provided that it passes the file-level filters.
        MemoryBank.debug("Data location is: " + MemoryBank.userDataDirPathName);
        File f = new File(MemoryBank.userDataDirPathName);
        scanDataDir(f, 0); // Indirectly fills the foundDataVector
        noteDataVector = foundDataVector;

        // We will display the results of the search, even if it found nothing.
        SearchResultGroupProperties srgp = new SearchResultGroupProperties();
        srgp.setSearchSettings(spTheSearchPanel.getSettings());
        ob1kenoby = srgp;

        // Make a unique filename for the results
        String strResultsFileName = "S" + AppUtil.getTimestamp();
        strResultsFileName += ".sresults";

        String strResultsPath = MemoryBank.userDataDirPathName + File.separatorChar;
        System.out.println(strResultsFileName + " results: " + foundDataVector.size());

        // Make the File, then save the results into it.
        File rf = new File(strResultsPath + strResultsFileName);
        saveNoteData(rf); // Saves ob1kenoby and then the noteDataVector

        // Make a new tree node for these results and select it
        addSearchResult(strResultsFileName, noteDataVector.size());

        showWorkingDialog(false);
    } // end showSearchDialog


    //--------------------------------------------------------
    // Method Name:  showToday
    //
    // If the current tree view is one of the ones that are
    // date-based and the date they are centered on is NOT
    // today, then this method will change the date of that
    // view to today.
    //
    // If the view is NOT date-based, it will change to a
    // textual representation of today's date.  The value
    // of this variation is questionable but it remains
    // below and can be enabled by placing a 'Today' on a
    // non-date-based group's menubar (currently that menu
    // choice is not present for a non date-centered view).
    //
    // If the view is date-based and the selected date is
    // already today, no apparent change is made but it
    // could be made into a toggle between the date-based
    // view and the textual panel as described above, like
    // the 'showAbout' behavior.  To implement a toggle on
    // date-based groups, will need to first determine if
    // 'Today' is already showing.
    //   See 'getChoiceString' from DayNoteGroup; may need
    //   a common method between all 5 groups, like isToday()
    //   or choiceIsDate(today).
    //--------------------------------------------------------
    public void showToday() {
        // Make sure that the most recent changes, if any, are preserved.
        preClose();

        TreePath tp = tree.getSelectionPath();

        currentDateChoice = new Date();

        String theCurrentView = tp.getLastPathComponent().toString();
        System.out.println("AppTree.showToday path=" + theCurrentView);

        if (theCurrentView.equals("Year View")) {
            theYearView.setChoice(currentDateChoice);
            return;
        } else if (theCurrentView.equals("Month View")) {
            theMonthView.setChoice(currentDateChoice);
            return;
        } else if (theCurrentView.equals("Day Notes")) {
            theAppDays.setChoice(currentDateChoice);
            return;
        } else if (theCurrentView.equals("Month Notes")) {
            theAppMonths.setChoice(currentDateChoice);
            return;
        } else if (theCurrentView.equals("Year Notes")) {
            theAppYears.setChoice(currentDateChoice);
            return;
        } // end if

        // If we got thru to here, it means that we were already
        //   displaying today's date when the user requested us
        //   to do so.  So - we do it bigger -

        SimpleDateFormat sdf = new SimpleDateFormat();
        sdf.applyPattern("EEEE, MMMM d, yyyy");

        JLabel dayTitle = new JLabel();
        dayTitle.setHorizontalAlignment(JLabel.CENTER);
        dayTitle.setForeground(Color.blue);
        dayTitle.setFont(Font.decode("Serif-bold-24"));
        dayTitle.setText(sdf.format(currentDateChoice));

        rightPane.setViewportView(dayTitle);
    } // end showToday


    public void showWeek() {
        MemoryBank.debug("showWeek called.");
        // This method is called from an external context.
        tree.setSelectionPath(weekViewPath);
    } // end showWeek


    //-----------------------------------------------------------
    // Method Name: showWorkingDialog
    //
    // Shows a small animated gif as a 'Working...' message,
    //   in a modal dialog.  Call this method before you start
    //   a potentially long task that the user must wait for,
    //   then call:  dlgWorkingDialog.setVisible(false);
    //   to go on.
    //-----------------------------------------------------------
    public static void showWorkingDialog(boolean b) {
        if (b) {
            // Create a new thread and setVisible within the thread.
            new Thread(new Runnable() {
                public void run() {
                    dlgWorkingDialog.setVisible(true);
                }
            }).start(); // Start the thread so that the dialog will show.
        } else {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        while (!dlgWorkingDialog.isShowing()) {
                            // First, wait for it to appear.
                            Thread.sleep(100);
                        } // end while
                    } catch (Exception e) {
                        System.out.println("Exception: " + e.getMessage());
                    }

                    // Then kill it.
                    dlgWorkingDialog.setVisible(false);
                }
            }).start();
        } // end if show else hide
    } // end showWorkingDialog


    private void treeSelectionChanged(TreePath tp) {
        String strSelectionType; // used in menu management

        // Obtain a reference to the new selection.
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                tree.getLastSelectedPathComponent();
        if (node == null) return;

        // We have started to handle the user's request; now
        //   disallow further input until we're finished.
        showWorkingDialog(true);

        // Update the current selection row
        appOpts.theSelectionRow = tree.getMaxSelectionRow();

        if (theNoteGroup != null) {
            theNoteGroup.preClose();
        } // end if

        //-------------------------------------------------------------
        // There are five (5) situations where the previous selection
        //   may have allowed the user to change the displayed date.
        //   If 'theLastSelection' is one of them then obtain the date
        //   from that group, so that it can be used later to set the
        //   date to be shown before we display the newly selected
        //   group.
        //-------------------------------------------------------------
        if (tp != null) {
            String theLastSelection = tp.getLastPathComponent().toString();
            MemoryBank.debug("Last Selection was: " + theLastSelection);

            if (theLastSelection.equals("Year View")) {
                Date d = theYearView.getChoice();
                // Unlike the others, a YearView choice MAY be null.
                if (d != null) currentDateChoice = d;
            } else if (theLastSelection.equals("Month View")) {
                currentDateChoice = theMonthView.getChoice();
            } else if (theLastSelection.equals("Day Notes")) {
                currentDateChoice = theAppDays.getChoice();
            } else if (theLastSelection.equals("Month Notes")) {
                currentDateChoice = theAppMonths.getChoice();
            } else if (theLastSelection.equals("Year Notes")) {
                currentDateChoice = theAppYears.getChoice();
            } // end if
            //-------------------------------------------------------------
        } // end if

        String theText = node.toString();
        String theParent = node.getParent().toString();
        MemoryBank.debug("New tree selection: " + theText);
        //System.out.println("New tree selection: " + theText);
        appOpts.theSelection = theText; // Preserved exactly.
        strSelectionType = theText;  // May be generalized

        //-----------------------------------------------------
        // These two booleans will help us to avoid going down
        //   the wrong branch in a case where some bozo named
        //   their To Do list - 'To Do Lists'.  Other cases
        //   where a list may be named 'Year View' or any other
        //   tree branch, are caught by the fact that we first
        //   look to see if the parent is 'To Do Lists', before
        //   we start considering the text of the selection.
        //-----------------------------------------------------
        boolean isTodoBranch = theText.equals("To Do Lists");
        boolean isTopLevel = theParent.equals("App");

        theNoteGroup = null; // initialize

        if (isTodoBranch && isTopLevel) {
            // To Do List management - select, deselect, rename, reorder, remove
            // The 'tree' may change often.  We instantiate a new helper
            // and editor each time, to be sure all are in sync.
            TodoBranchHelper tbh = new TodoBranchHelper(tree);
            TreeBranchEditor tbe = new TreeBranchEditor(node, tbh);
            rightPane.setViewportView(tbe);
        } else if (!node.isLeaf()) {
            // Looking at other expandable nodes
            JTree jt = new JTree(node);
            //jt.setEditable(true);
            jt.setShowsRootHandles(true);
            rightPane.setViewportView(jt);
        } else if (theParent.equals("To Do Lists")) {
            // Selection of a To Do List
            strSelectionType = "To Do List";  // For manageMenus
            TodoNoteGroup tng;

            // If the list has been previously loaded during this session,
            // retrieve it from the list keeper.
            tng = theTodoListKeeper.get(theText);

            // Otherwise, prepare to load it.
            if(tng == null) {
                log.debug("Loading " + theText + " from filesystem");
                tng = new TodoNoteGroup(theText);
                theTodoListKeeper.add(tng);
            } else {
                log.debug("Retrieved " + theText + " from the keeper");
            }

            theNoteGroup = tng;
            rightPane.setViewportView(theNoteGroup);
        } else if (theText.equals("Goals")) {
            if (theGoalPanel == null) {
                theGoalPanel = new GoalPanel();
            }
            rightPane.setViewportView(theGoalPanel);
        } else if (theText.equals("Year View")) {
            if (theYearView == null) {
                theYearView = new YearView(this);
            } // end if
            theYearView.setChoice(currentDateChoice);
            rightPane.setViewportView(theYearView);
        } else if (theText.equals("Month View")) {
            if (theMonthView == null) {
                theMonthView = new MonthView(this);
            }
            theMonthView.setChoice(currentDateChoice);
            rightPane.setViewportView(theMonthView);
        } else if (theText.equals("Day Notes")) {
            if (theAppDays == null) {
                theAppDays = new DayNoteGroup();
            }
            theNoteGroup = theAppDays;
            theAppDays.setChoice(currentDateChoice);
            rightPane.setViewportView(theAppDays);
        } else if (theText.equals("Month Notes")) {
            if (theAppMonths == null) {
                theAppMonths = new MonthNoteGroup();
            }
            theNoteGroup = theAppMonths;
            theAppMonths.setChoice(currentDateChoice);
            rightPane.setViewportView(theAppMonths);
        } else if (theText.equals("Year Notes")) {
            if (theAppYears == null) {
                theAppYears = new YearNoteGroup();
            }
            theNoteGroup = theAppYears;
            theAppYears.setChoice(currentDateChoice);
            rightPane.setViewportView(theAppYears);
        } else if (theText.equals("Upcoming Events")) {
            if (theEvents == null) {
                theEvents = new EventNoteGroup();
            }
            theNoteGroup = theEvents;
            rightPane.setViewportView(theEvents);
        } else if (node.isNodeAncestor(nodeSearchResults)) {
            strSelectionType = "Search Result";
            SearchResultNode srn = (SearchResultNode) node;

            // If we have performed this search (or previously
            //   selected these results) during the current session,
            //   the Search Result Group will not be null.  Otherwise,
            //   instruct the node to read in the file.
            if (srn.srg == null) {
                System.out.println("name: " + srn.strFileName + " size: " + srn.intGroupSize);

                // Strip the path from the node's filename, then rebuild it.
                // This is to handle the case where the results file was moved
                //   from one filesystem/user to another.  Note that this is
                //   only needed currently (2/27/2008) for transitional data
                //   and that newer search results will not be stored with
                //   the full path in the first place.
                String s = SearchResultNode.prettyName(srn.strFileName);
                s = MemoryBank.userDataDirPathName + File.separatorChar + s;
                s += ".sresults";

                if (new File(s).exists()) {
                    // As a transient type, the srg was not reestablished during a load.
                    srn.srg = new SearchResultGroup(s);
                } // end if there is a file
            } // end if

            if (srn.srg == null) {
                // We just attempted to load it; if still null then
                //   it means the file is not there or not accessible.

                // Show a warning that the node will be removed, UNLESS
                //   this is a 'restore selection' operation that occurs
                //   during program start, in which case we just remove
                //   it without notice because there is already a modal
                //   dialog running.
                if (!blnRestoringSelection) {
                    // The user clicked on this node
                    JOptionPane.showMessageDialog(this,
                            "Cannot read in the search results.\n" +
                                    "This search results selection will be removed.",
                            "Results not accessible", JOptionPane.WARNING_MESSAGE);
                } // end if

                // Now remove the node, even if it has children.
                removeSearchNode((SearchResultNode) node);
            } else {
                theNoteGroup = srn.srg;
                rightPane.setViewportView(theNoteGroup);
            } // end if
        } else {
            // Any other as-yet unhandled node on the tree.
            // Currently - Week View, Icon Manager
            JPanel jp = new JPanel(new GridBagLayout());
            jp.add(new JLabel(theText));
            rightPane.setViewportView(jp);
        } // end if/else if

        amb.manageMenus(strSelectionType);
        showWorkingDialog(false);
    } // end treeSelectionChanged


    //-------------------------------------------------
    // Method Name:  updateTreeState
    //
    // Capture the current tree configuration
    //   and put it into appOpts (AppOptions class).
    //-------------------------------------------------
    private void updateTreeState(boolean updateLists) {
        appOpts.ViewsExpanded = tree.isExpanded(viewPath);
        appOpts.NotesExpanded = tree.isExpanded(notePath);
        appOpts.TodoListsExpanded = tree.isExpanded(todoPath);

        //System.out.println("Divider Location: " + splitPane.getDividerLocation());
        appOpts.paneSeparator = splitPane.getDividerLocation();

        // Preserve the expansion state of the Search Results
        if (nodeSearchResults != null) {
            TreeNode[] pathToRoot = nodeSearchResults.getPath();
            TreePath path = new TreePath(pathToRoot);
            nodeSearchResults.blnExpanded = tree.isExpanded(path);
        }

        appOpts.theSelectionRow = tree.getMaxSelectionRow();
        // Current selection text was captured when the last selection
        //    was made, but the row may have changed due to expansion
        //    or collapsing of nodes above the selection.

        if (!updateLists) return;

        // Preserve the active To Do Lists
        DefaultMutableTreeNode theTodoNode = TodoBranchHelper.getTodoNode(theRootNode);
        DefaultMutableTreeNode leafLink;
        int numLeaves;
        appOpts.todoLists.clear();

        numLeaves = theTodoNode.getChildCount();
        if (numLeaves > 0) {
            leafLink = theTodoNode.getFirstLeaf();
            while (numLeaves-- > 0) {
                String s = leafLink.toString();
                //MemoryBank.debug("  Preserving list: " + s);
                appOpts.todoLists.addElement(s);
                leafLink = leafLink.getNextLeaf();
            } // end while
        } // end if

        //------------------------------------------

        // Preserve the active Search Results
        appOpts.searchResults = nodeSearchResults;
    } // end updateTreeState

    //-------------------------------------------------------------
    // Method Name: valueChanged
    //
    // Required by TreeSelectionListener interface.
    //
    // We handle this event by starting a separate new thread so we can
    //   quickly return from here.  Otherwise, the 'working' dialog
    //   would never update until after all actions had completed,
    //   and that defeats its entire purpose.
    //-------------------------------------------------------------
    public void valueChanged(TreeSelectionEvent e) {
//new Exception("Lee - no real problem").printStackTrace();

        final TreePath tp = e.getOldLeadSelectionPath();
        if (blnRestoringSelection) {
            // We don't need to handle this event from a separate
            //   thread because we don't need the 'working' dialog
            //   when restoring a previous selection because this
            //   only happens during program restart, when
            //   we already have a progress bar with the
            //   'Restoring Previous Selection' message.
            treeSelectionChanged(tp);
        } else {
            // This is a user-directed selection;
            //   handle from a separate thread.
            new Thread(new Runnable() {
                public void run() {
                    // AppUtil.localDebug(true);
                    treeSelectionChanged(tp);
                    // AppUtil.localDebug(false);
                }
            }).start(); // Start the thread
        }
    } // end valueChanged

    private int writeExportFile() {
        try {
            // Make a unique filename for the results
            String strResultsFileName = "Export" + AppUtil.getTimestamp();
            strResultsFileName += ".txt";

            String strResultsPath = MemoryBank.userDataDirPathName + File.separatorChar;
            System.out.println(strResultsFileName + " results: " + exportDataVector.size());

            // Make the File, then save the results into it.
            FileWriter fstream = new FileWriter(strResultsPath + strResultsFileName);
            //saveNoteData(rf); // Saves ob1kenoby and then the noteDataVector

            PrintWriter out = new PrintWriter(fstream);
            for (String tempData : exportDataVector) {
                out.println(tempData);
            }//end for i
            out.flush();
            out.close();
            fstream.close();
        } catch (IOException ioe) {
            System.err.println("Error: " + ioe.getMessage());
            return 1; // Problem
        }//end try/catch

        return 0;  // Success
    }//end writeExportFile

} // end AppTree class

interface iconKeeper {
    public abstract AppIcon getDefaultIcon();

    public abstract void setDefaultIcon(AppIcon li);
} // end iconKeeper

