/* ***************************************************************************
 * File:    LogTree.java
 * Author:  D. Lee Chastain
 ****************************************************************************/
/**
 The primary control for the Log application; provides a menubar
 at the top, a 'tree' control on the left, and a viewing pane on
 the right
 */

// Quick-reference notes:
// 
// MenuBar events        - actionPerformed.
// Tree Selection events - valueChanged.
// TodoListManager Apply - TodoListHandler$actionPerformed.
//
// Management of 'to do' lists was developed first, long before management
//   of search results.  The two methodologies are different, and the
//   search methodology may be more efficient - consider a 
//   rewrite of the 'to do' list approach.

import java.awt.Component;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.GridLayout;
import java.awt.Point;

import java.io.*;        // File, Exceptions, io Streams, Serializable.
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;

import javax.swing.*;    // JPanel, JScrollPane, Jx, Jy, Jz...
import javax.swing.plaf.FontUIResource;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;

public final class AppTree extends JPanel implements TreeSelectionListener {
    static final long serialVersionUID = 1L;

    public static AppTree ltTheTree;
    private JFrame theFrame;

    private static final int LIST_GONE = -3; // used in constr, createTree

    // All related to the menu bar and its menus -
    private static JMenuBar mb;
    private static JMenu menuEditDay;
    private static JMenu menuEditMonth;
    private static JMenu menuEditTodo;
    private static JMenu menuEditYear;
    private static JMenu menuFile;
    private static JMenu menuFileSearchResult;
    private static JMenu menuFileTodo;
    private static JMenu menuView;
    private static JMenu menuViewEvent;
    private static JMenu menuViewDate;

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

    private NoteGroup ngTheNoteGroup;
    private GoalPanel theGoalPanel;
    private DayNoteGroup theLogDays;
    private MonthNoteGroup theLogMonths;
    private YearNoteGroup theLogYears;
    private MonthView theMonthView;
    private YearView theYearView;
    private EventNoteGroup theEvents;
    private Vector<TodoLeaf> theTodoLeafVector;
    private Vector<NoteData> noteDataVector;  // For searching
    private Vector<NoteData> foundDataVector;  // Search results
    private Vector<String> exportDataVector;
    private TodoNoteGroup theTodoList;    // currently active list
    private SearchPanel spTheSearchPanel;
    private AppImage abbowt;
    private ViewportLayout normalLayout;
    private CenterViewportLayout centerLayout;

    private static String ems;               // Error Message String
    private static Date currentDateChoice;
    private static TreeOptions treeOpts;     // saved/loaded
    private TodoListHandler theTodoListHandler;
    private DefaultMutableTreeNode theRootNode;
    private DefaultMutableTreeNode nodeTodoLists;
    private SearchResultNode nodeSearchResults;

    // Predefined Tree Paths
    private TreePath dayNotesPath;
    private TreePath monthNotesPath;
    private TreePath yearNotesPath;
    private TreePath monthViewPath;
    private TreePath weekViewPath;
    private TreePath upcomingEventsPath;

    // Used in Search / fix
    private Object ob1kenoby;

    private boolean blnRestoringSelection;

    static {
        menuFile = new JMenu("File");
        menuFile.add(new JMenuItem("Search..."));
        menuFile.add(new JMenuItem("Export"));
        menuFile.add(new JMenuItem("Exit"));

        menuFileSearchResult = new JMenu("File");
        menuFileSearchResult.add(new JMenuItem("Close"));
        menuFileSearchResult.add(new JMenuItem("Search..."));
        menuFileSearchResult.add(new JMenuItem("Search these results..."));
        menuFileSearchResult.add(new JMenuItem("Review..."));
        menuFileSearchResult.add(new JMenuItem("Exit"));

        menuFileTodo = new JMenu("File");
        menuFileTodo.add(new JMenuItem("Search..."));
        menuFileTodo.add(new JMenuItem("Merge..."));
        menuFileTodo.add(new JMenuItem("Print..."));
        menuFileTodo.add(new JMenuItem("Save As..."));
        menuFileTodo.add(new JMenuItem("Exit"));

        menuEditDay = new JMenu("Edit");
        menuEditDay.add(new JMenuItem("undo"));
        menuEditDay.add(new JMenuItem("Clear Day"));

        menuEditMonth = new JMenu("Edit");
        menuEditMonth.add(new JMenuItem("undo"));
        menuEditMonth.add(new JMenuItem("Clear Month"));

        menuEditYear = new JMenu("Edit");
        menuEditYear.add(new JMenuItem("undo"));
        menuEditYear.add(new JMenuItem("Clear Year"));

        menuEditTodo = new JMenu("Edit");
        menuEditTodo.add(new JMenuItem("undo"));
        menuEditTodo.add(new JMenuItem("Clear Entire List"));
        menuEditTodo.addSeparator();
        menuEditTodo.add(new JMenuItem("Set Options..."));

        menuView = new JMenu("View");
        menuView.add(new JMenuItem("Set Look and Feel..."));

        menuViewEvent = new JMenu("View");
        // menuViewEvent.add(new JMenuItem("Date Format"));
        menuViewEvent.add(new JMenuItem("Refresh"));
        menuViewEvent.add(new JMenuItem("Set Look and Feel..."));

        menuViewDate = new JMenu("View");
        menuViewDate.add(new JMenuItem("Today"));
        menuViewDate.add(new JMenuItem("Set Look and Feel..."));

        JMenu menuHelp = new JMenu("Help");
        menuHelp.add(new JMenuItem("Contents"));
        menuHelp.add(new JMenuItem("About"));

        mb = new JMenuBar();
        mb.add(menuFile);
        mb.add(menuFileSearchResult);
        mb.add(menuFileTodo);
        mb.add(menuEditDay);
        mb.add(menuEditMonth);
        mb.add(menuEditYear);
        mb.add(menuEditTodo);
        mb.add(menuView);
        mb.add(menuViewEvent);
        mb.add(menuViewDate);

        // This puts the 'Help' on the far right side.
        mb.add(Box.createHorizontalGlue());
        mb.add(menuHelp);
        // mb.setHelpMenu(menuHelp);  // Not implemented in Java 1.4.2 ...
        // Still not implemented in Java 1.5.0_03

        // Initial visibility of all menu items is false.
        // That can change in 'manageMenus'
        menuEditDay.setVisible(false);
        menuEditMonth.setVisible(false);
        menuEditTodo.setVisible(false);
        menuEditYear.setVisible(false);
        menuFileTodo.setVisible(false);
        menuFileSearchResult.setVisible(false);
        menuView.setVisible(false);
        menuViewEvent.setVisible(false);
        menuViewDate.setVisible(false);

        // Global setting for tool tips
        UIManager.put("ToolTip.font",
                new FontUIResource("SansSerif", Font.BOLD, 12));

    } // end static


    public AppTree(JFrame aFrame) {
        super(new GridLayout(1, 0));
        theFrame = aFrame;
        theFrame.setJMenuBar(mb);
        ltTheTree = this;

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
            } // end valueChanged
        };
        //---------------------------------------------------

        //---------------------------------------------------------
        // Add the above handler to all menu items.
        //---------------------------------------------------------
        // Note - if you need cascading menus in the future, use
        //   the recursive version of this as implemented in
        //   LogPane.java, a now archived predecessor to AppTree.
        //---------------------------------------------------------
        int numMenus = mb.getMenuCount();
        // MemoryBank.debug("Number of menus found: " + numMenus);
        for (int i = 0; i < numMenus; i++) {
            JMenu jm = mb.getMenu(i);
            if (jm == null) continue;

            for (int j = 0; j < jm.getItemCount(); j++) {
                JMenuItem jmi = jm.getItem(j);
                if (jmi == null) continue; // Separator
                jmi.addActionListener(al);
            } // end for j
        } // end for i
        //---------------------------------------------------------

        setOpaque(true);

        // Load the user settings
        treeOpts = new TreeOptions(); // Start with default values.
        MemoryBank.update("Loading the previous Tree configuration");
        loadOpts(); // If available, overrides defaults.

        createTree();  // Create the tree.

        // Listen for when the selection changes.
        // We need to do this now so that the proper initialization
        //   occurs when we restore the previous selection, below.
        tree.addTreeSelectionListener(this);

        // Create the scroll pane and add the tree to it.
        JScrollPane treeView = new JScrollPane(tree);

        // Create the viewing pane and start with the 'about' graphic.
        abbowt = new AppImage(MemoryBank.logHome + "/images/ABOUT.gif", false);
        rightPane = new JScrollPane(abbowt);

        centerLayout = new CenterViewportLayout();
        normalLayout = (ViewportLayout) rightPane.getViewport().getLayout();

        rightPane.getViewport().setLayout(centerLayout);

        // Add the scroll panes to a split pane.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(treeView);

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
        splitPane.setDividerLocation(140);

        splitPane.setPreferredSize(new Dimension(820, 520));

        // Add the split pane to this panel.
        add(splitPane);

        currentDateChoice = new Date();

        // Restore the last selection.
        MemoryBank.update("Restoring the previous selection");
        blnRestoringSelection = true;
        if (treeOpts.theSelectionRow >= 0)
            tree.setSelectionRow(treeOpts.theSelectionRow);
        else {
            if (treeOpts.theSelectionRow == LIST_GONE) {
                treeOpts.theSelectionRow = tree.getRowForPath(todoPath);
                tree.setSelectionRow(treeOpts.theSelectionRow);
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
            updateTreeOpts(false);

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
            if(!f.delete()) System.out.println("Failed to remove " + node.strFileName);
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
        DefaultMutableTreeNode trunk = new DefaultMutableTreeNode("Log");
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
        // The root branch
        nodeTodoLists = new DefaultMutableTreeNode("To Do Lists");
        branch = nodeTodoLists;
        String theName;
        theTodoLeafVector = new Vector<TodoLeaf>(0, 1);
        theTodoListHandler = new TodoListHandler(branch);
        trunk.add(branch);
        intRowCounter++;
        intTodoRow = intRowCounter;

        for (String s : treeOpts.todoLists) {

            // First check to see that the file is 'here'.
            theName = MemoryBank.userDataDirPathName + File.separatorChar + s + ".todolist";
            if (new File(theName).exists()) {
                MemoryBank.debug("  Adding List: " + s);

                // Add to the tree
                leaf = new DefaultMutableTreeNode(s);
                branch.add(leaf);

            } else { // List not found.
                MemoryBank.debug("  The " + s + " file was not found");
                if (s.equals(treeOpts.theSelection)) {
                    treeOpts.theSelection = "To Do Lists";
                    treeOpts.theSelectionRow = LIST_GONE;
                } // end if
            } // end if
        } // end for
        //--------------------

        leaf = new DefaultMutableTreeNode("Icon Manager");
        trunk.add(leaf);
        //intRowCounter++;

        // Restore previous search results, if any.
        if (treeOpts.searchResults != null) {
            nodeSearchResults = treeOpts.searchResults;
            trunk.add(nodeSearchResults);
        } // end if

        // Create a default model based on the 'Log' node that
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


    private void doExport() {
        showWorkingDialog(true); // Show the 'Working...' dialog

        // Make sure that the most recent changes, if any,
        //   will be included in the export.
        if (ngTheNoteGroup != null) {
            ngTheNoteGroup.preClose();
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


    private void handleMenuBar(String what) {
        if (what.equals("Exit")) System.exit(0);
        else if (what.equals("About")) showAbout();
        else if (what.equals("Close")) closeSearchResult();
        else if (what.equals("Clear Day")) theLogDays.clearGroupData();
        else if (what.equals("Clear Month")) theLogMonths.clearGroupData();
        else if (what.equals("Clear Year")) theLogYears.clearGroupData();
        else if (what.equals("Clear Entire List")) theTodoList.clearGroupData();
        else if (what.equals("Contents")) showHelp();
        else if (what.equals("Export")) doExport();
        else if (what.equals("Search...")) showSearchDialog();
        else if (what.equals("Set Options...")) ((TodoNoteGroup) ngTheNoteGroup).setOptions();
        else if (what.startsWith("Merge")) mergeTodoList();
        else if (what.startsWith("Print")) ((TodoNoteGroup) ngTheNoteGroup).printList();
        else if (what.equals("Refresh")) theEvents.refresh();
        else if (what.equals("Review...")) System.out.println("Review was selected.");
        else if (what.startsWith("Save As")) theTodoListHandler.saveAs();
        else if (what.equals("Today")) showToday();
        else if (what.equals("Set Look and Feel...")) showPlafDialog();
        else if (what.equals("undo")) {
            String s = treeOpts.theSelection;
            if (s.equals("Day Notes")) theLogDays.recalc();
            else if (s.equals("Month Notes")) theLogMonths.recalc();
            else if (s.equals("Year Notes")) theLogYears.recalc();
            else ngTheNoteGroup.updateGroup(); // reload without save
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


    //------------------------------------------------------
    // Method Name: loadOpts
    //
    // Load the last known state of the tree, if any.  This
    //  includes which nodes are expanded and which additional
    //  leaves have been added.
    //------------------------------------------------------
    private void loadOpts() {
        Exception e = null;
        FileInputStream fis;
        TreeOptions tmp;

        String FileName = MemoryBank.userDataDirPathName + File.separatorChar + "tree.options";

        try {
            fis = new FileInputStream(FileName);
            ObjectInputStream ois = new ObjectInputStream(fis);
            tmp = (TreeOptions) ois.readObject();
            treeOpts = tmp;
            ois.close();
            fis.close();
        } catch (ClassCastException cce) {
            e = cce;
        } catch (ClassNotFoundException cnfe) {
            e = cnfe;
        } catch (InvalidClassException ice) {
            e = ice;
        } catch (FileNotFoundException fnfe) {
            // not a problem; use defaults.
            MemoryBank.debug("User tree options not found; using defaults");
        } catch (EOFException eofe) {
            e = eofe;
        } catch (IOException ioe) {
            e = ioe;
        } // end try/catch

        if (e != null) {
            ems = "Error in loading " + FileName + " !\n";
            ems = ems + e.toString();
            ems = ems + "\nOptions load operation aborted.";
            JOptionPane.showMessageDialog(null,
                    ems, "Error", JOptionPane.ERROR_MESSAGE);
        } // end if
    } // end loadOpts


    private void manageMenus(String strMenuType) {

        // Set the default of having the 'File' and 'View' menus only;
        //   let the specific cases below make any needed alterations.
        //-----------------------------------------
        menuEditDay.setVisible(false);
        menuEditMonth.setVisible(false);
        menuEditTodo.setVisible(false);
        menuEditYear.setVisible(false);
        menuFile.setVisible(true);
        menuFileTodo.setVisible(false);
        menuFileSearchResult.setVisible(false);
        menuView.setVisible(true);
        menuViewEvent.setVisible(false);
        menuViewDate.setVisible(false);

        if (strMenuType.equals("Day Notes")) { // Day Notes
            menuEditDay.setVisible(true);
            menuView.setVisible(false);
            menuViewDate.setVisible(true);
        } else if (strMenuType.equals("Month Notes")) { // Month Notes
            menuEditMonth.setVisible(true);
            menuView.setVisible(false);
            menuViewDate.setVisible(true);
        } else if (strMenuType.equals("Month View")) { // Month View
            menuView.setVisible(false);
            menuViewDate.setVisible(true);
        } else if (strMenuType.equals("Year View")) { // Year View
            menuView.setVisible(false);
            menuViewDate.setVisible(true);
        } else if (strMenuType.equals("Search Result")) { // Search Results
            menuFile.setVisible(false);
            menuFileSearchResult.setVisible(true);
            menuView.setVisible(false);
            menuViewDate.setVisible(true); // Temporary; should go away.
        } else if (strMenuType.equals("Year Notes")) { // Year Notes
            menuEditYear.setVisible(true);
            menuView.setVisible(false);
            menuViewDate.setVisible(true);
        } else if (strMenuType.equals("Upcoming Events")) { // Upcoming Events
            menuView.setVisible(false);
            menuViewEvent.setVisible(true);
        } else if (strMenuType.equals("To Do List")) { // A List
            menuFile.setVisible(false);
            menuFileTodo.setVisible(true);
            menuEditTodo.setVisible(true);
        } // end if
    } // end manageMenus


    public void mergeTodoList() {
        String mf = TodoNoteGroup.chooseFileName("Merge");
        if (mf != null) ((TodoNoteGroup) ngTheNoteGroup).merge(mf);
    } // end mergeTodoList


    //------------------------------------------------------------
    // Method Name:  preClose
    //
    //------------------------------------------------------------
    public void preClose() {
        // Only for the currently active NoteGroup; any others would
        //   have been saved when the view changed away from them.
        if (ngTheNoteGroup != null) ngTheNoteGroup.preClose();

        updateTreeOpts(true); // Update treeOpts
        saveOpts();
    } // end preClose


    //----------------------------------------------------------------
    // Method Name:  removeLeafFromVector
    //
    // Scan the reference vector looking for the
    // indicated leaf and if found, remove.
    //----------------------------------------------------------------
    public void removeLeafFromVector(String s) {
        TodoLeaf tmpTl; // Keep a temporary reference to a TodoLeaf
        tmpTl = null; // Initialize the tmp ref to null.

        // Search the TodoLeaf Vector for the list.
        for (TodoLeaf tl : theTodoLeafVector) {
            if (s.equals(tl.theLeafTodoListName)) {
                tmpTl = tl;
                // Note: cannot remove from within this loop;
                // ConcurrentModificationException.
                break;
            } // end if
        } // end for

        // If found, then remove.  Otherwise no action needed.
        if (tmpTl != null) {
            MemoryBank.debug("  Removing " + s + " from the TodoLeaf Vector");
            theTodoLeafVector.removeElement(tmpTl);
        } // end if
    } // end removeLeafFromVector


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
        ngTheNoteGroup = null;

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
                        if(!theFile1.delete()) System.out.println("Failed to remove " + theFile);
                    }
                } // end if
            } // end for

            // Display the 'About' view.
            // Code below was copied from the relevant section
            //   of 'showAbout', without the toggle feature.
            rightPane.getViewport().setLayout(centerLayout);
            rightPane.setViewportView(abbowt);
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
        if (treeOpts.ViewsExpanded) tree.expandPath(viewPath);
        if (treeOpts.NotesExpanded) tree.expandPath(notePath);
        if (treeOpts.TodoListsExpanded) tree.expandPath(todoPath);

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
        String FileName = MemoryBank.userDataDirPathName + File.separatorChar + "tree.options";
        MemoryBank.debug("Saving tree option data in " + FileName);

        try {
            FileOutputStream fos = new FileOutputStream(FileName);
            ObjectOutputStream oos = new ObjectOutputStream(fos);
            oos.writeObject(treeOpts);
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
            ems = ems + "\nTree options save operation aborted.";
            MemoryBank.debug(ems);
            // This popup caused a hangup and the vm had to be 'kill'ed.
            // JOptionPane.showMessageDialog(null,
            //    ems, "Error", JOptionPane.ERROR_MESSAGE);
            // Yes, even though the parent was null.
        } // end try/catch
    } // end saveOpts


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
            if (treeOpts.ViewsExpanded) tree.expandPath(viewPath);
            else tree.collapsePath(viewPath);
            if (treeOpts.NotesExpanded) tree.expandPath(notePath);
            else tree.collapsePath(notePath);
            if (treeOpts.TodoListsExpanded) tree.expandPath(todoPath);
            else tree.collapsePath(todoPath);
            tree.setSelectionRow(treeOpts.theSelectionRow);
        } else {
            // Capture the current state; we may have to 'toggle' back to it.
            updateTreeOpts(false);
            tree.clearSelection();
            rightPane.getViewport().setLayout(centerLayout);
            rightPane.setViewportView(abbowt);
            manageMenus("");
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
                theTodoListHandler.selectList(pn);
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
        if (ngTheNoteGroup != null) {
            ngTheNoteGroup.preClose();
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
    // view to today.  Otherwise, the view will change to a
    // textual representation of today's date.
    // Note: The 'otherwise' stopped working 12/2006 for
    //   date-based-but-same-date selections.  In 3/2008
    //   it stopped working for non-date-based selections,
    //   when 'Today' moved from a tree selection to a
    //   menu choice that is not always displayed.  The
    //   value of the 'otherwise' action is questionable but
    //   it remains below and can be accessed by placing a
    //   'Today' on a non-date-based group's menubar.  To
    //   reinstate a toggle on date-based groups, will need
    //   to first determine if 'Today' is already showing.
    //   See 'getChoiceString' from DayNoteGroup; may need
    //   a common method between all 5 groups, like isToday()
    //   or choiceIsDate(today).
    //   Then, can implement something like the 'showAbout'
    //   toggle.
    //--------------------------------------------------------
    public void showToday() {
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
            theLogDays.setChoice(currentDateChoice);
            return;
        } else if (theCurrentView.equals("Month Notes")) {
            theLogMonths.setChoice(currentDateChoice);
            return;
        } else if (theCurrentView.equals("Year Notes")) {
            theLogYears.setChoice(currentDateChoice);
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

        rightPane.getViewport().setLayout(centerLayout);
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
        treeOpts.theSelectionRow = tree.getMaxSelectionRow();

        // null this value so that if the new selection is NOT a
        // To Do list, the reference will not be active.  This
        // condition is a critical decision factor in manageMenus.
        theTodoList = null;

        if (ngTheNoteGroup != null) {
            ngTheNoteGroup.preClose();
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
                currentDateChoice = theLogDays.getChoice();
            } else if (theLastSelection.equals("Month Notes")) {
                currentDateChoice = theLogMonths.getChoice();
            } else if (theLastSelection.equals("Year Notes")) {
                currentDateChoice = theLogYears.getChoice();
            } // end if
            //-------------------------------------------------------------
        } // end if

        String theText = node.toString();
        String theParent = node.getParent().toString();
        MemoryBank.debug("New tree selection: " + theText);
        //System.out.println("New tree selection: " + theText);
        treeOpts.theSelection = theText; // Preserved exactly.
        strSelectionType = theText;  // May be generalized

        // Set (or reset) the default layout to the norm.
        rightPane.getViewport().setLayout(normalLayout);

        //-----------------------------------------------------
        // These two booleans will help us to avoid going down
        //   the wrong branch in a case where some bozo named
        //   their To Do list - 'To Do Lists'.  Other cases
        //   where a list may be named 'Year View' or any other
        //   tree branch, are caught by the fact that we first
        //   look to see if the parent is 'To Do Lists', before
        //   we start considering the text of the selection.
        //-----------------------------------------------------
        boolean isListManager = theText.equals("To Do Lists");
        boolean isTopLevel = theParent.equals("Log");

        ngTheNoteGroup = null; // initialize

        if (isListManager && isTopLevel) {
            // To Do List selection / deselection
            theTodoListHandler.showListManager(TodoListManager.SELECT_MODE);

        } else if (!node.isLeaf()) {
            // Looking at other expandable nodes
            JTree jt = new JTree(node);
            jt.setRootVisible(false);
            //jt.setShowsRootHandles(true);
            rightPane.setViewportView(jt);

        } else if (theParent.equals("To Do Lists")) {
            // Selection of a To Do List
            strSelectionType = "To Do List";
            boolean foundInVector = false;

            // Search the TodoLeaf Vector for this list.
            for (TodoLeaf tl : theTodoLeafVector) {
                if (theText.equals(tl.theLeafTodoListName)) {
                    theTodoList = tl.theLeafTodoList;
                    foundInVector = true;
                    break;
                } // end if
            } // end for
            MemoryBank.debug("Found List in Vector: " + foundInVector);

            if (!foundInVector) {
                // This just means that we can add it now.

                TodoLeaf tl = new TodoLeaf(theText);
                tl.theLeafTodoList = new TodoNoteGroup(theText);
                theTodoList = tl.theLeafTodoList;
                theTodoLeafVector.addElement(tl);
            } // end if
            ngTheNoteGroup = theTodoList;

            rightPane.setViewportView(theTodoList);
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
            if (theLogDays == null) {
                theLogDays = new DayNoteGroup();
            }
            ngTheNoteGroup = theLogDays;
            theLogDays.setChoice(currentDateChoice);
            rightPane.setViewportView(theLogDays);
        } else if (theText.equals("Month Notes")) {
            if (theLogMonths == null) {
                theLogMonths = new MonthNoteGroup();
            }
            ngTheNoteGroup = theLogMonths;
            theLogMonths.setChoice(currentDateChoice);
            rightPane.setViewportView(theLogMonths);
        } else if (theText.equals("Year Notes")) {
            if (theLogYears == null) {
                theLogYears = new YearNoteGroup();
            }
            ngTheNoteGroup = theLogYears;
            theLogYears.setChoice(currentDateChoice);
            rightPane.setViewportView(theLogYears);
        } else if (theText.equals("Upcoming Events")) {
            if (theEvents == null) {
                theEvents = new EventNoteGroup();
            }
            ngTheNoteGroup = theEvents;
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
                ngTheNoteGroup = srn.srg;
                rightPane.setViewportView(ngTheNoteGroup);
            } // end if
        } else {
            rightPane.getViewport().setLayout(centerLayout);
            rightPane.setViewportView(new JLabel(theText));
        } // end if/else if

        manageMenus(strSelectionType);
        showWorkingDialog(false);
    } // end treeSelectionChanged


    //-------------------------------------------------
    // Method Name:  updateTreeState
    //
    // Capture the variables in the tree configuration
    //   and put them in treeOpts (TreeOptions class).
    //-------------------------------------------------
    private void updateTreeOpts(boolean updateLists) {
        treeOpts.ViewsExpanded = tree.isExpanded(viewPath);
        treeOpts.NotesExpanded = tree.isExpanded(notePath);
        treeOpts.TodoListsExpanded = tree.isExpanded(todoPath);

        // Preserve the expansion state of the Search Results
        if (nodeSearchResults != null) {
            TreeNode[] pathToRoot = nodeSearchResults.getPath();
            TreePath path = new TreePath(pathToRoot);
            nodeSearchResults.blnExpanded = tree.isExpanded(path);
        }

        treeOpts.theSelectionRow = tree.getMaxSelectionRow();
        // Current selection text was captured when the last selection
        //    was made, but the row may have changed due to expansion
        //    or collapsing of nodes above the selection.

        if (!updateLists) return;

        // Used in loops below -
        DefaultMutableTreeNode leafLink;
        int numLeaves;

        // Preserve the active To Do Lists
        treeOpts.todoLists.clear();

        numLeaves = nodeTodoLists.getChildCount();
        if (numLeaves > 0) {
            leafLink = nodeTodoLists.getFirstLeaf();
            while (numLeaves-- > 0) {
                String s = leafLink.toString();
                //MemoryBank.debug("  Preserving list: " + s);
                treeOpts.todoLists.addElement(s);
                leafLink = leafLink.getNextLeaf();
            } // end while
        } // end if

        //------------------------------------------

        // Preserve the active Search Results
        treeOpts.searchResults = nodeSearchResults;
    } // end updateTreeOpts

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


    //------------------------------------------------------------
    // Inner classes
    //------------------------------------------------------------

    //------------------------------------------------------------
    // Class Name:  CenterViewportLayout
    //
    // A custom layout manager to allow centering of the viewport
    // view component, when it is smaller than the space allotted.
    //------------------------------------------------------------
    final class CenterViewportLayout extends ViewportLayout {
        private static final long serialVersionUID = 3968894610762011575L;

        private static final boolean useCenterLayout = true;

        /**
         * This method is copied directly from
         * ViewportLayout.layoutContainer() useCenterLayout marks all
         * changed behavior
         */
        public void layoutContainer(Container parent) {
            javax.swing.JViewport vp = (javax.swing.JViewport) parent;
            Component view = vp.getView();
            javax.swing.Scrollable scrollableView = null;

            if (view == null) return;

            try {
                scrollableView = (javax.swing.Scrollable) view;
            } catch (ClassCastException cce) {
                // do nothing
            } // end try/catch

            // All of the dimensions below are in view coordinates, except
            //   vpSize which we're converting.
            Dimension viewPrefSize = view.getPreferredSize();
            Dimension vpSize = vp.getSize();
            Dimension extentSize = vp.toViewCoordinates(vpSize);
            Dimension viewSize = new Dimension(viewPrefSize);

            if (scrollableView != null) {
                if (scrollableView.getScrollableTracksViewportWidth()) {
                    viewSize.width = vpSize.width;
                } // end if
                if (scrollableView.getScrollableTracksViewportHeight()) {
                    System.out.println("!!! tracking height");
                    viewSize.height = vpSize.height;
                } // end if
            } // end if

            Point viewPosition = vp.getViewPosition();

      /* If the new viewport size would leave empty space to the
       * right of the view, right justify the view or left justify
       * the view when the width of the view is smaller than the
       * container.
       */
            if (useCenterLayout) {
                if (extentSize.width > viewSize.width) {
                    viewPosition.x = (viewSize.width - extentSize.width) / 2;
                }
            } else {
                if (scrollableView == null ||
                        vp.getParent() == null ||
                        vp.getParent().getComponentOrientation().isLeftToRight()) {
                    if ((viewPosition.x + extentSize.width) > viewSize.width) {
                        viewPosition.x = Math.max(0, viewSize.width - extentSize.width);
                    }
                } else {
                    if (extentSize.width > viewSize.width) {
                        viewPosition.x = viewSize.width - extentSize.width;
                    } else {
                        viewPosition.x = Math.max(0,
                                Math.min(viewSize.width - extentSize.width, viewPosition.x));
                    }
                }
            }

      /* If the new viewport size would leave empty space below the
       * view, bottom justify the view or top justify the view when
       * the height of the view is smaller than the container.
       */
            if ((viewPosition.y + extentSize.height) > viewSize.height) {
                viewPosition.y = Math.max(0, viewSize.height - extentSize.height);
            }

      /* If we haven't been advised about how the viewports size 
       * should change wrt to the viewport, i.e. if the view isn't
       * an instance of Scrollable, then adjust the views size as follows.
       * 
       * If the origin of the view is showing and the viewport is
       * bigger than the views preferred size, then make the view
       * the same size as the viewport.
       */

            if (useCenterLayout) {
                if (extentSize.height > viewSize.height) {
                    viewPosition.y = (viewSize.height - extentSize.height) / 2;
                }
            } else {
                if (scrollableView == null) {
                    if ((viewPosition.x == 0) && (vpSize.width > viewPrefSize.width)) {
                        viewSize.width = vpSize.width;
                    }
                    if ((viewPosition.y == 0) && (vpSize.height > viewPrefSize.height)) {
                        viewSize.height = vpSize.height;
                    }
                }
            }

            vp.setViewSize(viewSize);
            vp.setViewPosition(viewPosition);
        }
    } // end class CenterViewportLayout


    //------------------------------------------------------------
    // Class Name:  TodoListHandler
    //
    //------------------------------------------------------------
    private final class TodoListHandler implements ActionListener {
        private DefaultMutableTreeNode theTodoBranch;
        private Vector<String> selections;
        private TodoListManager todoMan;
        private TreeNode[] pathToRoot;
        private DefaultMutableTreeNode tmpNode;

        public TodoListHandler(DefaultMutableTreeNode dmtn) {
            theTodoBranch = dmtn;
        } // end constructor


        public void actionPerformed(ActionEvent e) {
            String doWhat = e.getActionCommand();
            MemoryBank.debug("AppTree$TodoListHandler " + doWhat);
            if (doWhat.equals("Select")) {
                doSelect();
            } else if (doWhat.equals("Add")) {
                // After the TodoListManager's checks on name validity,
                //   do not know of any reason why we would fail to
                //   create the list, if it does not already exist.  So -

                // Get the new list name
                String theNewList = selections.elementAt(0);

                // and make it happen
                selectList(theNewList);
            } else if (doWhat.equals("Rename")) {
                String oldName = selections.elementAt(0);
                String newName = selections.elementAt(1);
                String didit = selections.elementAt(2);

                // We need to remove the old list name from the Vector, in the case
                //   that the original list name had previously been a selection.
                removeLeafFromVector(oldName);

                // Is the operation complete?
                if (didit.equals("yes")) return;

                // Otherwise, 'no' - in which case the renamed list is
                // also a current selection.  Rename the leaf.
                // Since we're not 'on' it, we won't need to reselect it.
                renameTreeNode(oldName, newName);

            } else if (doWhat.equals("Delete")) {
                doDelete();
            } // end if/else if

        } // end actionPerformed


        //-------------------------------------------------------
        // Method Name:  doDelete
        //
        // Called after the TodoListManager has deleted one or
        // more TD lists.  Now, (if present) remove the deleted
        // lists from the tree and Vector.
        //-------------------------------------------------------
        private void doDelete() {
            MemoryBank.debug("Reconfiguring tree based on deletions");
            boolean changeWasMade = false;
            int numLeaves;
            DefaultMutableTreeNode leafLink;
            boolean inBranch;

            for (String s : selections) { // For each deletion
                // Remove from Vector.
                // If this deletion had previously been selected and viewed
                // then it is in the reference Vector and should be removed.
                // This is for later, in the case where a new list could be
                // created with this name or an existing list is renamed to
                // it.  If we still had a good reference to the deleted list
                // then during a vector search we might find that one first,
                // rather than the one we're really looking for.
                removeLeafFromVector(s);

                numLeaves = theTodoBranch.getChildCount();
                leafLink = theTodoBranch.getFirstLeaf();

                // Search the tree branch for this deleted list.
                inBranch = false;
                while (numLeaves-- > 0) {
                    String leaf = leafLink.toString();
                    if (leaf.equals(s)) {
                        inBranch = true;
                        break;
                    } // end if
                    leafLink = leafLink.getNextLeaf();
                } // end while

                // If the deletion is present, remove from branch.
                if (inBranch) {
                    MemoryBank.debug("  Pruning " + s + " from the 'To Do Lists' branch");
                    changeWasMade = true;
                    theTodoBranch.remove(leafLink);
                } // end if
            } // end for each deletion

            if (changeWasMade) treeModel.nodeStructureChanged(theTodoBranch);
        } // end doDelete


        //-------------------------------------------------------------------
        // Method Name:  doSelect
        //
        // Examine each node in the tree and verify that it is still in the
        //   selections.  If not then delete that node.
        //
        // Then examine each selection and verify that it is in the tree.
        //   If not then add it to the 'end'.
        //
        // The intent is not to rebuild the tree each time but to prune and
        //   grow it as the way to let the user specify the order of the
        //   lists that it contains.
        //-------------------------------------------------------------------
        private void doSelect() {
            MemoryBank.debug("Reconfiguring tree based on selections");
            boolean changeWasMade = false;
            int numLeaves = theTodoBranch.getChildCount();
            int tmpNum;
            DefaultMutableTreeNode leafLink;
            DefaultMutableTreeNode tmpLeaf;

            //---------------------------------------
            // This section implements 'deselection'.
            //---------------------------------------
            leafLink = theTodoBranch.getFirstLeaf();
            boolean inSelections;

            // Search the selections for this leaf.
            while (numLeaves-- > 0) {
                inSelections = false;
                String leaf = leafLink.toString();
                for (String sel : selections) {
                    if (leaf.equals(sel)) {
                        inSelections = true;
                        break;
                    } // end if
                } // end for

                // Keep a reference to this leaf, in case -
                tmpLeaf = leafLink;
                leafLink = leafLink.getNextLeaf();

                // If not a current selection, remove from tree.
                if (!inSelections) {
                    MemoryBank.debug("  deselecting " + leaf);
                    changeWasMade = true;
                    theTodoBranch.remove(tmpLeaf);
                } // end if
            } // end while

            //---------------------------------------
            // This section implements 'selection'.
            //---------------------------------------
            boolean foundInTree;

            // We get this value once, outside the loops below, because we
            //   don't need to search through leaves that we just added as
            //   the list grows, in the case of selecting an additional ten
            //   lists, for example.
            tmpNum = theTodoBranch.getChildCount(); // get current # of leaves.

            for (String sel : selections) {  // for each selection -
                foundInTree = false; // initialize.
                numLeaves = tmpNum;  // (re)set to the original count.
                tmpLeaf = theTodoBranch.getFirstLeaf();
                while (numLeaves-- > 0) {
                    String leaf = tmpLeaf.toString();
                    if (sel.equals(leaf)) {
                        foundInTree = true;
                        MemoryBank.debug("  " + sel + " already in Tree");
                        break; // leave the 'while' - no need to search the rest.
                    } // end if
                    tmpLeaf = tmpLeaf.getNextLeaf();
                } // end while

                if (!foundInTree) {
                    MemoryBank.debug("  Added " + sel);
                    theTodoBranch.add(new DefaultMutableTreeNode(sel));
                    changeWasMade = true;
                } // end if

            } // end for

            if (changeWasMade) treeModel.nodeStructureChanged(theTodoBranch);
        } // end doSelect


        //----------------------------------------------------------------
        // Method Name:  renameTreeNode
        //
        //----------------------------------------------------------------
        private void renameTreeNode(String oldname, String newname) {
            boolean changeWasMade = false;
            int numLeaves = theTodoBranch.getChildCount();
            DefaultMutableTreeNode leafLink;

            leafLink = theTodoBranch.getFirstLeaf();

            // Search the leaves for the old name.
            while (numLeaves-- > 0) {
                String leaf = leafLink.toString();
                if (leaf.equals(oldname)) {
                    MemoryBank.dbg("Renaming tree node from " + oldname);
                    MemoryBank.debug(" to " + newname);
                    changeWasMade = true;
                    leafLink.setUserObject(newname);
                    break;
                } // end if

                leafLink = leafLink.getNextLeaf();
            } // end while

            if (!changeWasMade) return;

            // Force the renamed node to redisplay, which also
            //   causes its deselection.
            treeModel.nodeStructureChanged(theTodoBranch);
        } // end renameTreeNode


        //-------------------------------------------------------
        // Method Name:  saveAs
        //
        // Called from the menu bar.
        //-------------------------------------------------------
        private void saveAs() {
            Frame f = JOptionPane.getFrameForComponent(tree);

            String thePrompt = "Please enter the new list name";
            int q = JOptionPane.QUESTION_MESSAGE;
            String newName = JOptionPane.showInputDialog(f, thePrompt, "Save As", q);

            // The user cancelled; return with no complaint.
            if (newName == null) return;

            newName = newName.trim(); // eliminate outer space.

            // Test new name validity.
            if (!TodoListManager.nameCheck(newName, f)) return;

            // Get the current file name -
            String oldName = theTodoList.getGroupFilename();
            oldName = TodoNoteGroup.prettyName(oldName);

            // If the new name equals the old name, just do as the user
            //   has asked and don't tell them that they are an idiot.
            if (newName.equals(oldName)) {
                theTodoList.preClose();
                return;
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
            // This condition of 'save as' is the same approach that was
            // taken in the 'rename' handling in the TodoListManager.

            // If we refuse the operation due to a preexisting destination
            // file name then the user has several recourses, depending on
            // what it was they really wanted to do - they could delete
            // the preexisting file or rename it, after which a second
            // attempt at this operation would succeed, or they could
            // realize that they had been having a senior moment and
            // abandon the effort, or they could choose a different
            // new name and try again, etc.
            //--------------------------------------------------------------
            String newFilename = MemoryBank.userDataDirPathName + File.separatorChar;
            newFilename += newName + ".todolist";

            if ((new File(newFilename)).exists()) {
                ems = "A list named " + newName + " already exists!\n";
                ems += "  operation cancelled.";
                JOptionPane.showMessageDialog(f, ems,
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            } // end if

            // Now change the name and save.
            //------------------------------------
            MemoryBank.debug("Saving " + oldName + " as " + newName);
            theTodoList.setFileName(newName);

            // Before we save, ensure that the 'old' file is not removed.
            AppUtil.localArchive(true);
            theTodoList.preClose();
            AppUtil.localArchive(false);

            // Rename the leaf.
            renameTreeNode(oldName, newName);

            // Removal from the Vector is needed because if not and the
            //   original list were to be reselected, it would be found and
            //   displayed with any changes that had been made before the
            //   saveAs, even though the file on the filesystem does not
            //   (and should not) have those changes.
            removeLeafFromVector(oldName);

            // Do not let the 'old' list save itself when we go back and
            //   reselect this node; lose the last handle to it and let the
            //   garbage collector do its thing.
            theTodoList = null;

            // Reselect this tree node because after renameTreeNode
            //   there is no current selection.  This will also have
            //   the effect of once again correctly setting theTodoList.
            tree.setSelectionRow(treeOpts.theSelectionRow);
        } // end saveAs


        //-------------------------------------------------------------
        // Method Name: selectList
        //
        // This method will select the list, adding it to the tree
        //   if it is not already there.  If the file does not exist,
        //   a new list will be created.  The input parameter is the
        //   simple name, not the entire file path/name.
        //-------------------------------------------------------------
        public void selectList(String theListToSelect) {
            // Check to see if it is already a displayed list -
            //   If so - grab a reference to the node.
            int numLeaves = theTodoBranch.getChildCount();
            tmpNode = null;
            DefaultMutableTreeNode tmpLeaf = theTodoBranch.getFirstLeaf();
            while (numLeaves-- > 0) {
                String s = tmpLeaf.toString();
                if (s.equalsIgnoreCase(theListToSelect)) {
                    tmpNode = tmpLeaf;
                    break;
                } // end if
                tmpLeaf = tmpLeaf.getNextLeaf();
            } // end while

            // Add the new list to the tree, if needed.
            if (tmpNode == null) {
                tmpNode = new DefaultMutableTreeNode(theListToSelect);
                theTodoBranch.add(tmpNode);
                treeModel.nodeStructureChanged(theTodoBranch);
            } // end if

            // Select the list.
            pathToRoot = tmpNode.getPath();
            tree.setSelectionPath(new TreePath(pathToRoot));
            // The actual creation of the item in the tracking vector
            //  and the construction of the TodoList will be done in
            //  'treeSelectionChanged', as part of selection handling.
            //
        } // end selectList

        public void showListManager(int mode) {
            int numLeaves = theTodoBranch.getChildCount();

            // Develop a list of pre-selected items.
            MemoryBank.dbg("AppTree$TodoListHandler number of To Do Lists ");
            MemoryBank.debug("in the tree: " + numLeaves);
            selections = new Vector<String>(numLeaves, 1);
            DefaultMutableTreeNode tmpLeaf = theTodoBranch.getFirstLeaf();
            while (numLeaves-- > 0) {
                String s = tmpLeaf.toString();
                // MemoryBank.debug("  " + s);
                selections.addElement(s);
                tmpLeaf = tmpLeaf.getNextLeaf();
            } // end while

            todoMan = new TodoListManager(selections, this, mode);
            rightPane.setViewportView(todoMan);
        } // end showListManager

    } // end class TodoListHandler


    //------------------------------------------------------------
    // Class Name:  TodoLeaf
    //
    //------------------------------------------------------------
    private final class TodoLeaf {
        private String theLeafTodoListName;
        private TodoNoteGroup theLeafTodoList;

        TodoLeaf(String s) {
            theLeafTodoListName = s;
            theLeafTodoList = null;
        } // end constructor
    } // end class TodoLeaf

} // end AppTree class


//-------------------------------------------------------------------------
// Class Name:  SearchResultNode
//
// Holds the data for the 'Search Results' tree node.
// The first one (top level) will have a null filename and the
//   node name will be 'Search Results'.
//-------------------------------------------------------------------------
class SearchResultNode extends DefaultMutableTreeNode implements Serializable {
    static final long serialVersionUID = 1955502766506973356L;

    public String strFileName;
    public String strNodeName;
    public int intGroupSize;
    public boolean blnExpanded;

    // This member is transient so that it will not be saved.  This
    //   allows the SearchResultNode to be quickly restored from
    //   saved data, even if it is a handle to a large SearchResultGroup.
    //   Upon initial construction of the node, it will be 'filled' but
    //   later, upon reload of this node, it will be null.  Then, the
    //   group will be loaded only if the node is clicked by the user.
    public transient SearchResultGroup srg;

    public SearchResultNode(String s, int i) {
        intGroupSize = i;
        if ((s == null) || (s.equals(""))) {
            strNodeName = "Search Results";
        } else {
            strFileName = s;  // will be null at the top level ONLY
            strNodeName = prettyName(s);

            // Note: an earlier version sent the full path in 's'.  Now,
            //   we expect to only have the filename.
            String strFilePath = MemoryBank.userDataDirPathName + File.separatorChar;
//      srg = new SearchResultGroup(strFilePath + strFileName, intGroupSize);
            srg = new SearchResultGroup(strFilePath + strFileName);
        } // end else

        blnExpanded = true;
    } // end constructor


    public static String prettyName(String s) {
        int i;
        char slash = File.separatorChar;

        i = s.lastIndexOf(slash);
        if (i != -1) {
            s = s.substring(i + 1);
        } // end if

        // Even though a Windows path separator char should be a
        //   backslash, in Java a forward slash is often also accepted.
        i = s.lastIndexOf("/");
        if (i != -1) {
            s = s.substring(i + 1);
        } // end if

        // Drop the suffix
        i = s.lastIndexOf(".sresults");
        if (i == -1) return s;
        return s.substring(0, i);
    } // end prettyName


    public String toString() {
        return strNodeName;
    }
} // end class SearchResultNode


interface iconKeeper {
    public abstract AppIcon getDefaultIcon();

    public abstract void setDefaultIcon(AppIcon li);
} // end iconKeeper

