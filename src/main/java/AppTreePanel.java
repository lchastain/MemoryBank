/*
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

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Vector;


public class AppTreePanel extends JPanel implements TreeSelectionListener {
    static final long serialVersionUID = 1L; // JPanel wants this but we will not serialize.
    static AppTreePanel theInstance;  // A tricky way for a static context to call an instance method.
    static AppMenuBar appMenuBar;

    private static Logger log = LoggerFactory.getLogger(AppTreePanel.class);

    Notifier optionPane;
    //-------------------------------------------------------------------

    private JTree theTree;
    private DefaultTreeModel treeModel;
    private JScrollPane rightPane;

    // Paths to expandable 'parent' nodes
    private TreePath viewsPath;
    private TreePath notesPath;
    private TreePath todolistsPath;
    private TreePath searchresultsPath;

    static JDialog theWorkingDialog;
    private NoteGroup theNoteGroup; // A reference to the current selection
    private NoteGroup deletedNoteGroup;
    private GoalGroup theGoalGroup;
    DayNoteGroup theAppDays;
    MonthNoteGroup theAppMonths;
    YearNoteGroup theAppYears;
    MonthView theMonthView;
    YearView theYearView;
    private Vector<NoteData> foundDataVector;  // Search results
    private FileGroupKeeper theGoalListKeeper;       // keeper of all loaded Goals.
    private FileGroupKeeper theEventListKeeper;      // keeper of all loaded Event lists.
    private FileGroupKeeper theTodoListKeeper;       // keeper of all loaded To Do lists.
    private FileGroupKeeper theSearchResultsKeeper;  // keeper of all loaded SearchResults.
    SearchPanel spTheSearchPanel;
    private JPanel aboutPanel;
    private JSplitPane splitPane;

    private LocalDate selectedDate;  // The selected date
    private LocalDate viewedDate;    // A date to be shown but not as a 'choice'.
    private ChronoUnit viewedDateGranularity;

    private DefaultMutableTreeNode theRootNode;

    // Predefined Tree Paths to 'leaf' nodes.
    TreePath dayNotesPath;
    TreePath monthNotesPath;
    TreePath yearNotesPath;
    TreePath yearViewPath;
    TreePath monthViewPath;
    private TreePath weekViewPath;
    private TreePath eventsPath;
    private TreePath goalsPath;

    private AppOptions appOpts;

    boolean restoringPreviousSelection;
    boolean searching;

    public AppTreePanel(JFrame aFrame, AppOptions appOpts) {
        super(new GridLayout(1, 0));
        appMenuBar = new AppMenuBar();
        aFrame.setJMenuBar(appMenuBar);
        theInstance = this; // This works because we will always only have one.
        this.appOpts = appOpts;

        //<editor-fold desc="Make the 'Working...' dialog">
        theWorkingDialog = new JDialog(aFrame, "Working", true);
        JLabel lbl = new JLabel("Please Wait...");
        lbl.setFont(Font.decode("Dialog-bold-16"));
        String strWorkingIcon = MemoryBank.logHome + File.separatorChar;
        strWorkingIcon += "icons" + File.separatorChar + "animated" + File.separatorChar + "const_anim.gif";
        lbl.setIcon(new AppIcon(strWorkingIcon));
        lbl.setVerticalTextPosition(JLabel.TOP);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        theWorkingDialog.add(lbl);
        theWorkingDialog.pack();
        theWorkingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        theWorkingDialog.setLocationRelativeTo(this);
        //</editor-fold>

        //<editor-fold desc="Initialize the Search Panel from a new thread">
        searching = false;
        new Thread(new Runnable() {
            public void run() {
                spTheSearchPanel = new SearchPanel();
            }
        }).start();
        //</editor-fold>

        optionPane = new Notifier() {
        }; // Uses all default methods.

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
        //---------------------------------------------------------
        // Add the above handler to all menu items.
        //---------------------------------------------------------
        // Note - if you need cascading menus in the future, use
        //   the recursive version of this as implemented in
        //   LogPane.java, a now archived predecessor to AppTreePanel.
        //---------------------------------------------------------
        int numMenus = appMenuBar.getMenuCount();
        // MemoryBank.debug("Number of menus found: " + numMenus);
        for (int i = 0; i < numMenus; i++) {
            JMenu jm = appMenuBar.getMenu(i);
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
        theTree.addTreeSelectionListener(this);

        // Create the scroll pane and add the tree to it.
        JScrollPane treeView = new JScrollPane(theTree);

        // Create the viewing pane and start with the 'about' graphic.
        AppImage abbowt = new AppImage(MemoryBank.logHome + "/images/ABOUT.gif");
        aboutPanel = new JPanel(new GridBagLayout());
        aboutPanel.add(abbowt); // Nested the image in a panel with a flexible layout, for centering.
        rightPane = new JScrollPane(aboutPanel);

        // Add the scroll panes to a split pane.
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setLeftComponent(treeView);
        if (appOpts.paneSeparator > 0) splitPane.setDividerLocation(appOpts.paneSeparator);
        else splitPane.setDividerLocation(140);

        // Do not let focus escape from the right side.
        theTree.setFocusable(false);
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

        // Initialize Dates
        selectedDate = LocalDate.now();
        viewedDate = selectedDate;
        viewedDateGranularity = ChronoUnit.DAYS;

        // Restore the last selection.
        MemoryBank.update("Restoring the previous selection");
        restoringPreviousSelection = true;
        // If there is a 'good' value stored as the previous selection -
        if (appOpts.theSelectionRow >= 0) theTree.setSelectionRow(appOpts.theSelectionRow);
        else appMenuBar.manageMenus("No Selection");
        // There are a few cases where there would be no previous selection.
        // Ex: Showing the About graphic, First-time-ever for this user, lost or corrupted AppOptions.
        // In these cases leave as-is and the view will go to the About graphic.

        restoringPreviousSelection = false;

    } // end constructor for AppTreePanel


    @SuppressWarnings("rawtypes")  // For the Enumeration, below.
    private void addNewGroup() {
        String newName;

        // Initialize the following local variables, otherwise IJ complains.
        String prompt = "no prompt";
        String title = "no title";
        TreePath groupParentPath = null;
        FileGroupKeeper theFileGroupKeeper = null;

        String theContext = appMenuBar.getCurrentContext();
        String areaName = null;
        MemoryBank.debug("Adding new group in this context: " + theContext);
        switch (theContext) {
            case "Goal":
            case "Goals Branch Editor":
                prompt = "Enter a name for the Goal\n";
                prompt += "Ex: Graduate, Learn a Language, etc";
                title = "Add a new Goal";
                groupParentPath = goalsPath;
                theFileGroupKeeper = theGoalListKeeper;
                areaName = GoalGroup.areaName;
                break;
            case "Upcoming Event":
            case "Upcoming Events Branch Editor":
                prompt = "Enter a name for the new Event category\n";
                prompt += "Ex: meetings, appointments, birthdays, etc";
                title = "Add a new Events category";
                groupParentPath = eventsPath;
                theFileGroupKeeper = theEventListKeeper;
                areaName = EventNoteGroup.areaName;
                break;
            case "To Do List":
            case "To Do Lists Branch Editor":
                prompt = "Enter a name for the new To Do List";
                title = "Add a new To Do List";
                groupParentPath = todolistsPath;
                theFileGroupKeeper = theTodoListKeeper;
                areaName = TodoNoteGroup.areaName;
                break;
        }

        // Get user entry of a name for the new group.
        newName = JOptionPane.showInputDialog(theTree, prompt, title, JOptionPane.QUESTION_MESSAGE);
        MemoryBank.debug("Name chosen for new group: " + newName);

        if (newName == null) return;      // No user entry; dialog was Cancelled.

        // A user would not be entering a full path, or even a name with the filename prefix; they shouldn't know those.
        // TODO - find or write a better way to groom the user input.
//        newName = TreeLeaf.prettyName(newName); // Normalize the input

        if (null == groupParentPath) return;
        String groupParentName = groupParentPath.getLastPathComponent().toString();
        DefaultMutableTreeNode groupParentNode = BranchHelperInterface.getNodeByName(theRootNode, groupParentName);

        // Declare a tree node for the new group.
        DefaultMutableTreeNode theNewGroupNode = null;

        // Allowing 'add' to act as a back-door selection of a group that actually already
        // exists is ok, but do not add this choice to the branch if it is already there.
        // So - examine the tree to see if there is already a node for the new group -
        Enumeration children = groupParentNode.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode achild = (DefaultMutableTreeNode) children.nextElement();
            if (achild.toString().equals(newName)) {
                theNewGroupNode = achild;
            }
        }

        // And now we know.  If the node did not already exist, we make it now.
        if (theNewGroupNode == null) {  // Not already a node on the tree
            theNewGroupNode = new DefaultMutableTreeNode(newName, false);
            // Ensure that the new name meets our file-naming requirements.
            File aFile = new File(FileGroup.getFullFilename(areaName, newName));
            String theComplaint = BranchHelperInterface.checkFilename(newName, aFile.getParent());
            if (!theComplaint.isEmpty()) {
                optionPane.showMessageDialog(theTree, theComplaint,
                        "Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            // Add the new node to the tree
            groupParentNode.add(theNewGroupNode);
            DefaultTreeModel theTreeModel = (DefaultTreeModel) theTree.getModel();
            theTreeModel.nodeStructureChanged(groupParentNode);
        }

        // Try to get this Group from the NoteGroupKeeper (the 'Add New' request might just be a back-door
        // selection rather than actually intending to make a new one).  If not found there then go ahead
        // and make a new one, and put it there.
        FileGroup theGroup = theFileGroupKeeper.get(newName);
        if (theGroup == null) { // Not already loaded; construct one, whether there is a file for it or not.
            MemoryBank.debug("Getting a new group from the factory.");
            theGroup = NoteGroupFactory.getOrMakeGroup(theContext, newName);
            assert theGroup != null; // It won't be, but IJ needs to be sure.
            theFileGroupKeeper.add(theGroup);
            theGroup.setGroupChanged(true); // Save this (may be empty) group.
            theGroup.preClose();
        }

        // Expand the parent node (if needed) and select the group.
        theTree.expandPath(groupParentPath);
        theTree.setSelectionPath(groupParentPath.pathByAddingChild(theNewGroupNode));
    } // end addNewGroup


    // Adds a search result branch to the tree.
    private void addSearchResult(String searchResultName) {
        // Remove the tree selection listener while we
        //   rebuild this portion of the tree.
        theTree.removeTreeSelectionListener(this);

        // Make a new tree node for the result file whose path
        //   is given in the input parameter
        DefaultMutableTreeNode tmpNode;
        tmpNode = new DefaultMutableTreeNode(searchResultName, false);

        // Add to the tree under the Search Results branch
        DefaultMutableTreeNode nodeSearchResults = BranchHelperInterface.getNodeByName(theRootNode, "Search Results");
        nodeSearchResults.add(tmpNode);
        treeModel.nodeStructureChanged(nodeSearchResults);

        // Select the new list.
        TreeNode[] pathToRoot = tmpNode.getPath();
        theTree.addTreeSelectionListener(this);
        theTree.setSelectionPath(new TreePath(pathToRoot));
    } // end addSearchResult


    //-------------------------------------------------------
    // Method Name: closeGroup
    //
    // Removes the tree leaf corresponding to the currently
    //    selected Group.
    //-------------------------------------------------------
    void closeGroup() {
        // Obtain a reference to the current tree selection.
        // Since the App menu provides the only 'legal' entry to this
        // method by only showing the 'Close' option when a closeable
        // group is showing, it follows that the current tree selection
        // is the group that we want to close.
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) theTree.getLastSelectedPathComponent();
        if (node == null) return; // But this is here just in case someone got here another way.
        assert node.getChildCount() == 0; // another fail-safe; the assertion should never fail.

        // Remove this leaf from the tree but do not let the removal result in the need to
        // process another tree selection event.
        theTree.removeTreeSelectionListener(this);
        DefaultMutableTreeNode theParent = (DefaultMutableTreeNode) node.getParent();
        node.removeFromParent();
        treeModel.nodeStructureChanged(theParent);
        theTree.addTreeSelectionListener(this);

        updateTreeState(true); // Needed prior to new link target selection.

        // Select the parent branch
        TreeNode[] pathToRoot = theParent.getPath();
        theTree.setSelectionPath(new TreePath(pathToRoot));
    } // end closeGroup


    //-----------------------------------------------------------------
    // Method Name: createTree
    //
    // Creates the 'MemoryBank' tree.
    //-----------------------------------------------------------------
    private void createTree() {
        MemoryBank.debug("Creating the tree");
        DefaultMutableTreeNode trunk = new DefaultMutableTreeNode("App");
        theRootNode = trunk;

        // Temporary highly-reused variables
        DefaultMutableTreeNode branch; // An expandable node
        DefaultMutableTreeNode leaf;   // An end-node
        TreeNode[] pathToRoot;  // An array of node names leading back to the root (after the node has been added)

        // It begins -
        //---------------------------------------------------
        // Goals
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode("Goals");
        trunk.add(branch);
        pathToRoot = branch.getPath();
        goalsPath = new TreePath(pathToRoot);
        theGoalListKeeper = new FileGroupKeeper();

        for (String s : appOpts.goalsList) {
            // Add to the tree
            leaf = new DefaultMutableTreeNode(s, false);
            MemoryBank.debug("  Adding List: " + s);
            branch.add(leaf);
        } // end for


        //---------------------------------------------------
        // Events
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode("Upcoming Events");
        trunk.add(branch);
        pathToRoot = branch.getPath();
        eventsPath = new TreePath(pathToRoot);
        theEventListKeeper = new FileGroupKeeper();

        for (String s : appOpts.eventsList) {
            // Add to the tree
            leaf = new DefaultMutableTreeNode(s, false);
            MemoryBank.debug("  Adding List: " + s);
            branch.add(leaf);
        } // end for
        //---------------------------------------------------


        //---------------------------------------------------
        // Views - Calendar-style displays of Year, Month, Week
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode("Views");
        trunk.add(branch);
        pathToRoot = branch.getPath();
        viewsPath = new TreePath(pathToRoot);

        leaf = new DefaultMutableTreeNode("Year View");
        branch.add(leaf);
        pathToRoot = leaf.getPath();
        yearViewPath = new TreePath(pathToRoot);

        leaf = new DefaultMutableTreeNode("Month View");
        branch.add(leaf);
        pathToRoot = leaf.getPath();
        monthViewPath = new TreePath(pathToRoot);

        leaf = new DefaultMutableTreeNode("Week View");
        branch.add(leaf);
        pathToRoot = leaf.getPath();
        weekViewPath = new TreePath(pathToRoot);
        //---------------------------------------------------

        //---------------------------------------------------
        // Notes - Group types are Day, Month, Year
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode("Notes");
        trunk.add(branch);
        pathToRoot = branch.getPath();
        notesPath = new TreePath(pathToRoot);

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
        branch = new DefaultMutableTreeNode("To Do Lists", true);
        trunk.add(branch);
        pathToRoot = branch.getPath();
        todolistsPath = new TreePath(pathToRoot);
        theTodoListKeeper = new FileGroupKeeper();

        for (String s : appOpts.tasksList) {
            // Add to the tree
            branch.add(new DefaultMutableTreeNode(s, false));
        } // end for
        //---------------------------------------------------

        //---------------------------------------------------
        // Search Results
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode("Search Results", true);
        trunk.add(branch);
        pathToRoot = branch.getPath();
        searchresultsPath = new TreePath(pathToRoot);
        theSearchResultsKeeper = new FileGroupKeeper();

        // Restore previous search results, if any.
        if (!appOpts.searchResultList.isEmpty()) {
            for (int i = 0; i < appOpts.searchResultList.size(); i++) {
                String searchResultFilename = appOpts.searchResultList.get(i);
                branch.add(new DefaultMutableTreeNode(searchResultFilename, false));
            }
        } // end if

        // Create a default model based on the 'App' node that
        //   we've been growing, and create the tree from that model.
        treeModel = new DefaultTreeModel(trunk);
        theTree = new JTree(treeModel);

        // At creation, all paths are collapsed.
        // Expand branches based on last saved configuration.
        resetTreeState();

        // Set to single selection mode.
        theTree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Do not show the 'App' root of the tree.
        theTree.setRootVisible(false);

        // But do show the link that all children have to it.
        theTree.setShowsRootHandles(true);
    } // end createTree

    //----------------------------------------------------------------
    // Method Name:  deepClone
    //
    // Used to fully clone a tree node, since the system-provided
    // method only does the first level.
    //----------------------------------------------------------------
    @SuppressWarnings("rawtypes")
    static DefaultMutableTreeNode deepClone(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode newRoot = (DefaultMutableTreeNode) root.clone();
        for (Enumeration childEnum = root.children(); childEnum.hasMoreElements(); ) {
            newRoot.add(deepClone((DefaultMutableTreeNode) childEnum.nextElement()));
        }
        return newRoot;
    }

    // Delete the data file for this group.
    // This method is provided as a more direct route to file deletion than going thru
    // the BranchHelper.  This section of the code is similar to what is done there,
    // except that we don't remove it from the DataGroupKeeper.  This will allow for
    // an 'undo', if desired.
    private void deleteGroup() {
        // They get one warning..
        String deleteWarning;
        boolean doDelete;
        deleteWarning = "Are you sure?" + System.lineSeparator();
        deleteWarning += "This deletion may be undone (via the menu option) but only while still on this Editor.";
        doDelete = optionPane.showConfirmDialog(theTree, deleteWarning,
                "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        if(!doDelete) return;

        // Preserve the reference to the group to be deleted.
        deletedNoteGroup = theNoteGroup;

        // Now we need to close the group.
        closeGroup(); // This will change the selection and null out 'theNoteGroup'.

        // Now delete the file
        String deleteFile = deletedNoteGroup.getGroupFilename();
        MemoryBank.debug("Deleting " + deleteFile);
        try {
            if (!(new File(deleteFile)).delete()) { // Delete the file.
                throw new Exception("Unable to delete " + deleteFile);
            } // end if
        } catch (Exception se) {
            MemoryBank.debug(se.getMessage());
        } // end try/catch

        // Now make sure that the group will not be saved upon app exit.
        // That could happen because it is still being held in its DataGroupKeeper.
        deletedNoteGroup.setGroupChanged(false);

        // And finally, give the user an 'undo' capability.  This will be a one-time,
        //   one file only option.  Upon deletion they will have gone back to the
        //   Branch Editor.  If they then apply other actions on the Editor, or go on
        //   to some other list, the 'undo' menu option goes away.
        appMenuBar.showRestoreOption(true);
        appMenuBar.manageMenus(appMenuBar.getCurrentContext());
    }// end deleteGroup

    private void doSearch() {
        searching = true;
        Frame theFrame = JOptionPane.getFrameForComponent(this);

        // Now display the search dialog.
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

        if (choice != JOptionPane.OK_OPTION) {
            if(!spTheSearchPanel.doSearch) {
                searching = false;
                return;
            }
            spTheSearchPanel.doSearch = false; // Put this flag back to non-testing mode.
        }

        if (!spTheSearchPanel.hasWhere()) {
            JOptionPane.showMessageDialog(this,
                    " No location to search was chosen!",
                    "Search conditions specification error",
                    JOptionPane.ERROR_MESSAGE);
            searching = false;
            return;
        } // end if no search location was specified.

        theWorkingDialog.setLocationRelativeTo(rightPane); // This can be needed if windowed app has moved from center screen.
        showWorkingDialog(true); // Show the 'Working...' dialog; it's in a separate thread so we can keep going here...

        // Make sure that the most recent changes, if any,
        //   will be included in the search.
        if (theNoteGroup != null) {
            theNoteGroup.preClose();
        } // end if

        // Now make a Vector that can collect the search results.
        foundDataVector = new Vector<>();

        // We will display the results of the search, even if it finds nothing.
        SearchResultGroupProperties searchResultGroupProperties = new SearchResultGroupProperties();
        searchResultGroupProperties.setSearchSettings(spTheSearchPanel.getSettings());
        MemoryBank.debug("Running a Search with these settings: " + AppUtil.toJsonString(spTheSearchPanel.getSettings()));

        // Now scan the user's data area for data files - we do a recursive
        //   directory search and each file is examined as soon as it is
        //   found, provided that it passes the file-level filters.
        MemoryBank.debug("Data location is: " + MemoryBank.userDataHome);
        File f = new File(MemoryBank.userDataHome);
        scanDataDir(f, 0); // Indirectly fills the foundDataVector

        // Make a unique name for the results
        String resultsName = AppUtil.getTimestamp();
        String resultsPath = MemoryBank.userDataHome + File.separatorChar + "SearchResults" + File.separatorChar;
        String resultsFileName = resultsPath + "search_" + resultsName + ".json";
        System.out.println("Search performed at " + resultsName + " results: " + foundDataVector.size());

        // Make a new data file to hold the searchResultData list
        Object[] theGroup = new Object[2]; // A 'wrapper' for the Properties + List
        theGroup[0] = searchResultGroupProperties;
        theGroup[1] = foundDataVector;
        int notesWritten = FileGroup.saveFileData(resultsFileName, theGroup);
        if (foundDataVector.size() != notesWritten) {
            System.out.println("Possible problem - wrote " + notesWritten + " results");
        } else {
            System.out.println("Wrote " + notesWritten + " results to " + resultsFileName);
        }

        // Make a new tree node for these results and select it
        addSearchResult(resultsName);

        searching = false;
        showWorkingDialog(false);
    } // end doSearch


    // Make a Consolidated View group from all the currently selected Event Groups.
    @SuppressWarnings({"rawtypes"})
    private EventNoteGroup getConsolidatedView() {
        // First, get all the nodes that are currently under Upcoming Events.
        DefaultMutableTreeNode eventsNode = BranchHelperInterface.getNodeByName(theRootNode, "Upcoming Events");
        Enumeration e = eventsNode.breadthFirstEnumeration();
        String theNodeName;
        EventNoteGroup theBigGroup = null;
        Vector<NoteData> groupDataVector;
        LinkedHashSet<NoteData> theUniqueSet = null;
        while (e.hasMoreElements()) { // A bit of unintentional mis-direction, here.
            // The first node that we get this way - is the expandable node itself - Upcoming Events.
            DefaultMutableTreeNode eventNode = (DefaultMutableTreeNode) e.nextElement();
            // So we don't actually use it.
            if (theBigGroup == null) {
                // Instead, we instantiate a new (empty) EventNoteGroup, named for the Consolidated View (CV).
                NoteComponent.isEditable = false; // This is a non-editable group.
                theBigGroup = new EventNoteGroup((MemoryBank.appOpts.consolidatedEventsViewName));
                NoteComponent.isEditable = true; // Put it back to the default value.
                continue;
            }
            // Then we can look at merging in any possible child nodes, but
            // we have to skip the one node that actually does denote the CV.
            // This one could be anywhere in the list; just skip it when we come to it.
            theNodeName = eventNode.toString();
            if (theNodeName.equals(MemoryBank.appOpts.consolidatedEventsViewName)) continue;

            // And for the others (if any) - merge them into the CV group.
            String theFilename = FileGroup.getFullFilename(EventNoteGroup.areaName, theNodeName);
            MemoryBank.debug("Node: " + theNodeName + "  File: " + theFilename);
            Object[] theData = FileGroup.loadFileData(theFilename);
            BaseData.loading = true; // We don't want to affect the lastModDates!
            groupDataVector = AppUtil.mapper.convertValue(theData[theData.length - 1], new TypeReference<Vector<EventNoteData>>() {  });
            BaseData.loading = false; // Restore normal lastModDate updating.

            if (theUniqueSet == null) {
                theUniqueSet = new LinkedHashSet<>(groupDataVector);
            } else {
                theUniqueSet.addAll(groupDataVector);
            }
        }
        if (theUniqueSet == null) return null;
        groupDataVector = new Vector<>(theUniqueSet);
        theBigGroup.addNoteAllowed = false;
        theBigGroup.showGroupData(groupDataVector);
        theBigGroup.doSort();
        return theBigGroup;
    } // end getConsolidatedView

    // Used by Test
    NoteGroup getTheNoteGroup() {
        return theNoteGroup;
    }

    public JTree getTree() {
        return theTree;
    }

    private void handleMenuBar(String what) {
        if (what.equals("Exit")) System.exit(0);
        else if (what.equals("About")) showAbout();
        else if (what.equals("Add New...")) addNewGroup();
        else if (what.equals("Close")) closeGroup();
        else if (what.startsWith("Clear ")) theNoteGroup.clearGroup();
        else if (what.equals("Contents")) showHelp();
        else if (what.equals("Delete")) deleteGroup();
        else if (what.equals("Search...")) doSearch();
        else if (what.equals("Set Options...")) ((TodoNoteGroup) theNoteGroup).setOptions();
        else if (what.startsWith("Merge")) mergeGroup();
            //else if (what.startsWith("Print")) ((TodoNoteGroup) theNoteGroup).printList();
        else if (what.equals("Review...")) System.out.println("Review was selected.  It aint reddy yet."); // SCR00084
        else if (what.equals("Save")) theNoteGroup.refresh();
        else if (what.startsWith("Save As")) saveGroupAs();
        else if (what.equals("Undo Delete")) {
            appMenuBar.manageMenus(appMenuBar.getCurrentContext());
            deletedNoteGroup.setGroupChanged(true);
            deletedNoteGroup.preClose();
            System.out.println("Did it.");
            deletedNoteGroup = null;
            appMenuBar.showRestoreOption(false);
            treeSelectionChanged(theTree.getSelectionPath()); // Reload the branch editor, to show the 'new' file.
        }
        else if (what.equals("Icon Manager...")) {
            theTree.clearSelection();
            JPanel jp = new JPanel(new GridBagLayout());
            jp.add(new JLabel(what));
            appMenuBar.manageMenus("Icons"); // This will get the default / unhandled case.
            rightPane.setViewportView(jp);
        } else if (what.equals("Today")) showToday();
        else if (what.equals("Undo All")) {
            String s = appOpts.theSelection;
            switch (s) {
                case "Day Notes":
                    theAppDays.recalc();
                    break;
                case "Month Notes":
                    theAppMonths.recalc();
                    break;
                case "Year Notes":
                    theAppYears.recalc();
                    break;
                default:
                    theNoteGroup.updateGroup(); // reload without save
                    break;
            }
        } else {
            AppUtil.localDebug(true);
            MemoryBank.debug("  " + what);
            AppUtil.localDebug(false);
        } // end if/else
    } // end handleMenuBar


    private void mergeGroup() {
        preClose();
        String theContext = appMenuBar.getCurrentContext();
        switch (theContext) {
            case "Upcoming Event":
                ((EventNoteGroup) theNoteGroup).merge();
                break;
            case "To Do List":
                ((TodoNoteGroup) theNoteGroup).merge();
                break;
        }
    } // end mergeGroup


    //------------------------------------------------------------
    // Method Name:  preClose
    // Purpose:  Preserves any unsaved changes to all NoteGroups
    //   and updates the current state of the tree into the app options,
    //   in advance of saving the options during app shutdown.
    //------------------------------------------------------------
    public void preClose() {
        if (theAppDays != null) theAppDays.preClose();
        if (theAppMonths != null) theAppMonths.preClose();
        if (theAppYears != null) theAppYears.preClose();
        theGoalListKeeper.saveAll();
        theEventListKeeper.saveAll();
        theTodoListKeeper.saveAll();
        theSearchResultsKeeper.saveAll();

        updateTreeState(true); // Capture expansion states into appOpts
    } // end preClose


    // Call this method to do a 'programmatic' rename of a node
    // on the Tree, as opposed to doing it manually via a
    // TreeBranchEditor.  It operates only on the tree and not with
    // any corresponding files; you must do that separately.
    // See the 'Save As...' methodology for a good example.
    private void renameTreeBranchLeaf(DefaultMutableTreeNode theBranch, String oldname, String newname) {
        boolean changeWasMade = false;
        DefaultTreeModel tm = (DefaultTreeModel) theTree.getModel();

        // The tree is set for single-selection, so the selection will not be a collection but a
        // single value.  Nonetheless, Swing only provides a 'get' for min and max, and either
        // one will work for us.  Note that the TreePath returned by getSelectionPath()
        // will probably NOT work for reselection after we do the rename, so we use the row.
        int returnToRow = theTree.getMaxSelectionRow();

        int numLeaves = theBranch.getChildCount();
        DefaultMutableTreeNode leafLink;

        leafLink = theBranch.getFirstLeaf();

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
        tm.nodeStructureChanged(theBranch);

        // Reselect this tree node.
        theTree.setSelectionRow(returnToRow);

    } // end renameTreeBranchLeaf


    //------------------------------------------------------------------
    // Method Name: resetTreeState
    //
    // Call this method after changes have been made to the tree, to
    //   cause a repaint.  The first line below does that all by itself,
    //   but it also results in a tree with all nodes collapsed.  So the
    //   rest of this medhod is needed to re-expand any nodes that
    //   should have stayed that way.
    //------------------------------------------------------------------
    private void resetTreeState() {
        treeModel.nodeStructureChanged(theRootNode); // collapses all branches

        // Expand branches based on last configuration.
        if (appOpts.goalsExpanded) theTree.expandPath(goalsPath);
        if (appOpts.eventsExpanded) theTree.expandPath(eventsPath);
        if (appOpts.viewsExpanded) theTree.expandPath(viewsPath);
        if (appOpts.notesExpanded) theTree.expandPath(notesPath);
        if (appOpts.todoListsExpanded) theTree.expandPath(todolistsPath);
        if (appOpts.searchesExpanded) theTree.expandPath(searchresultsPath);

    } // end resetTreeState


    private void saveGroupAs() {
        String oldName = theNoteGroup.getName();
        boolean success = false;
        FileGroupKeeper theFileGroupKeeper = null;
        TreePath groupParentPath = null;

        String theContext = appMenuBar.getCurrentContext();
        switch (theContext) {
            case "Upcoming Event":
                success = ((EventNoteGroup) theNoteGroup).saveAs();
                groupParentPath = eventsPath;
                theFileGroupKeeper = theEventListKeeper;
                break;
            case "To Do List":
                success = ((TodoNoteGroup) theNoteGroup).saveAs();
                groupParentPath = todolistsPath;
                theFileGroupKeeper = theTodoListKeeper;
                break;
        }
        if (null == groupParentPath) return; // Should not happen in normal operation.

        if (success) {
            String newName = theNoteGroup.getName();

            // When the tree selection changes, any open NoteGroup is automatically saved,
            // and the tree selection will change automatically when we do the rename of
            // the leaf on the tree below.  But in this case we do not want that behavior,
            // because we have already saved the file, milliseconds ago.  It wouldn't hurt
            // to let it save again, but why allow it, when all it takes to stop it is:
            theNoteGroup = null;

            // Removal from the NoteGroupKeeper is needed, to force a file reload
            // during the rename of the leaf (below), because even though the saveAs
            // operation changed the name of the list held by the ListKeeper, it
            // still shows a title that was developed from the old file name.
            // Reloading from the file with the new name will fix that.
            theFileGroupKeeper.remove(newName);

            // Rename the leaf.
            // This will refresh the branch and reselect the same tree row to
            // cause a reload and redisplay of the group.  Note that not only does the
            // leaf name change, but the reload also changes the displayed group title.
            String groupParentName = groupParentPath.getLastPathComponent().toString();
            DefaultMutableTreeNode groupParentNode = BranchHelperInterface.getNodeByName(theRootNode, groupParentName);
            renameTreeBranchLeaf(groupParentNode, oldName, newName);
        }
    } // end saveGroupAs

    //------------------------------------------------------------------------------------------
    // Method Name:  scanDataDir
    //
    // This method scans a directory for data files.  If it finds a directory rather than a file,
    //   it will recursively call itself for that directory.
    //
    // The SearchPanel interface follows a 'filter out' plan.  To support that, this method starts
    //   with the idea that ALL files will be searched and then considers the filters, to eliminate
    //   candidate files.  If a file is not eliminated after the filters have been considered, the
    //   search method is called for that file.
    //------------------------------------------------------------------------------------------
    private void scanDataDir(File theDir, int level) {
        MemoryBank.dbg("Scanning " + theDir.getName());

        File[] theFiles = theDir.listFiles();
        assert theFiles != null;
        int howmany = theFiles.length;
        MemoryBank.debug("\t\tFound " + howmany + " data files");
        boolean goLook;
        LocalDate dateNoteDate;
        MemoryBank.debug("Level " + level);

        for (File theFile : theFiles) {
            String theFile1Name = theFile.getName();
            if (theFile.isDirectory()) {
                if (theFile1Name.equals("Archives")) continue;
                if (theFile1Name.equals("icons")) continue;
                if (theFile1Name.equals("SearchResults")) continue;
                scanDataDir(theFile, level + 1);
            } else {
                goLook = true;
                if (theFile1Name.equals("Goals")) {
                    if (!spTheSearchPanel.searchGoals()) {
                        goLook = false;
                    }
                } else if (theFile1Name.startsWith("event_")) {
                    if (!spTheSearchPanel.searchEvents()) {
                        goLook = false;
                    }
                } else if (theFile1Name.startsWith("todo_")) {
                    if (!spTheSearchPanel.searchLists()) {
                        goLook = false;
                    }
                } else if ((theFile1Name.startsWith("D")) && (level > 0)) {
                    if (!spTheSearchPanel.searchDays()) goLook = false;
                } else if ((theFile1Name.startsWith("M")) && (level > 0)) {
                    if (!spTheSearchPanel.searchMonths()) goLook = false;
                } else if ((theFile1Name.startsWith("Y")) && (level > 0)) {
                    if (!spTheSearchPanel.searchYears()) goLook = false;
                } else { // Any other file type not covered above.
                    // This includes search results (for now - SCR0073)
                    goLook = false;
                } // end if / else if

                // Check the Note date, possibly filter out based on 'when'.
                if (goLook) {
                    dateNoteDate = AppUtil.getDateFromFilename(theFile);
                    if (dateNoteDate != null) {
                        if (spTheSearchPanel.filterWhen(dateNoteDate)) goLook = false;
                    } // end if
                } // end if


                // The Last Modified date of the FILE is not necessarily the same as the Note, but
                //   it CAN be considered when looking for a last mod AFTER a certain date, because
                //   the last mod to ANY note in the file CANNOT be later than the last mod to the
                //   file itself.  Of course this depends on having no outside mods to the filesystem
                //   but we assume that because this is either a dev system (and we trust all devs :)
                //   or the app is being served from a server where only admins have access (and we
                //   trust all admins, of course).
                if (goLook) {
                    LocalDate dateLastMod = Instant.ofEpochMilli(theFile.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
                    if (spTheSearchPanel.getLastModSetting() == SearchPanel.AFTER) {
                        if (spTheSearchPanel.filterLastMod(dateLastMod)) goLook = false;
                    } // end if
                } // end if

                if (goLook) {
                    searchDataFile(theFile);
                } // end if
            } // end if
        }//end for i
    }//end scanDataDir


    //---------------------------------------------------------
    // Method Name: searchDataFile
    //
    // File-level (but not item-level) date filtering will
    //   have been done prior to this method being called.
    // For item-level filtering is not done; date-filtering
    //   is done against Calendar notes, by using their
    //   filename, only.  Todo items will all just pass thru
    //   the filter so if not desired, don't search there.
    //---------------------------------------------------------
    private void searchDataFile(File dataFile) {
        String theFilename = dataFile.getName();
        MemoryBank.debug("Searching: " + theFilename);
        Vector<AllNoteData> searchDataVector = null;

        Object[] theGroupData = FileGroup.loadFileData(dataFile);
        if (theGroupData != null && theGroupData[theGroupData.length - 1] != null) {
            BaseData.loading = true; // We don't want to affect the lastModDates!
            // During a search these notes would not be re-preserved anyway, but the reason we care is that
            // the search parameters may have specified a date-specific search; we don't want all Last Mod
            // dates to get updated to this moment and thereby muck up the search results.
            searchDataVector = AppUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<AllNoteData>>() { });
            BaseData.loading = false; // Restore normal lastModDate updating.
        }
        if(searchDataVector == null) return;

        // Now get on with the search -
        for (AllNoteData vectorItem : searchDataVector) {

            // If we find what we're looking for in/about this note -
            if (spTheSearchPanel.foundIt(vectorItem)) {

                // Make new search result data for this find.
                SearchResultData srd = new SearchResultData(vectorItem);

                // The copy constructor used above will preserve the
                //   dateLastMod of the original note.  Members specific
                //   to a SearchResultData must be set explicitly.
                srd.setFileFoundIn(dataFile);

                // Add this search result data to our findings.
                foundDataVector.add(srd);

            } // end if
        } // end for
    }// end searchDataFile

    void setSelectedDate(LocalDate theSelection) {
        selectedDate = theSelection;
        viewedDate = theSelection;
        viewedDateGranularity = ChronoUnit.DAYS;
    }

    void setViewedDate(int theYear) {
        viewedDate = LocalDate.of(theYear, viewedDate.getMonth(), viewedDate.getDayOfMonth());
        viewedDateGranularity = ChronoUnit.YEARS;
    }

    void setViewedDate(LocalDate theViewedDate, ChronoUnit theGranularity) {
        viewedDate = theViewedDate;
        viewedDateGranularity = theGranularity;
    }

    //--------------------------------------------------------------
    // Method Name:  showAbout
    //
    // This method will put the 'About' graphic into the right
    //   side of the display.  However, if invoked a second time
    //   without any other tree selection in between, it will
    //   restore the original selection.  To implement this
    //   'toggle' functionality, it is also necessary to capture
    //   the expanded/collapsed state of the tree on the first
    //   'About' and then restore it on the second call, possibly
    //   overriding the user's changes if they had made tree
    //   changes after the first 'About' without also making a new
    //   tree selection.  This happens because the tree state is
    //   reset whenever the About graphic is shown.  Acceptable.
    //--------------------------------------------------------------
    void showAbout() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                theTree.getLastSelectedPathComponent();

        // Examine the rightPane to see if the About graphic is already shown, or not.
        // There are simpler ways (such as aboutPanel.isShowing(), but that does not
        // work with the toggle test.
        JViewport viewport = rightPane.getViewport();
        JComponent theContent = (JComponent) viewport.getView();

        if (node == null && aboutPanel == theContent) { // This means we can toggle back to a previous tree selection.
            // Reset tree to the state it was in before.
            restoringPreviousSelection = true;
            if (appOpts.eventsExpanded) theTree.expandPath(eventsPath);
            else theTree.collapsePath(viewsPath);
            if (appOpts.viewsExpanded) theTree.expandPath(viewsPath);
            else theTree.collapsePath(viewsPath);
            if (appOpts.notesExpanded) theTree.expandPath(notesPath);
            else theTree.collapsePath(notesPath);
            if (appOpts.todoListsExpanded) theTree.expandPath(todolistsPath);
            else theTree.collapsePath(todolistsPath);
            theTree.setSelectionRow(appOpts.theSelectionRow);
        } else {
            // Capture the current state; we may have to 'toggle' back to it.
            updateTreeState(true); // Now updating lists every time, due to link target selection.
            theTree.clearSelection();

            // Show it.
            rightPane.setViewportView(aboutPanel);

            appMenuBar.manageMenus("About"); // This will get the default / unhandled case.
        } // end if
    } // end showAbout


    // Called from YearView or MonthView, mouse dbl-click on date.
    void showDay() {
        MemoryBank.debug("showDay called.");
        theTree.setSelectionPath(dayNotesPath);
    } // end showDay


    //------------------------------------------------------------
    // Method Name:  showFoundIn
    //
    //------------------------------------------------------------
    void showFoundIn(SearchResultData srd) {
        // Determine the treepath to be shown, based on
        //   the result's file name/path.
        String fname = srd.getFileFoundIn().getName();
        String fpath = srd.getFileFoundIn().getParent();

        if (fname.startsWith("todo_")) {
            // Given that a 'FoundIn' button has presumably been clicked, the implication
            // is that 'theNoteGroup' is populated with a SearchResultGroup, so we can use
            // that reference to access it's prettyName method (that it has due to NoteGroup implemening TreeLeaf).
            String prettyName = theNoteGroup.prettyName(fname);

            if (!(srd.getFileFoundIn()).exists()) {
                String s;
                s = "Error in loading " + prettyName + " !\n";
                s += "The original list no longer exists.";
                optionPane.showMessageDialog(this, s, "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                // We want to show the TodoNoteGroup where this data (srd) was found, but what if it is not currently
                // showing as a selectable leaf on the tree?  We cannot just add it anyway; it may be very old and
                // had been deliberately deselected.  This review of 'found-in-a-search' results should not change
                // the user's tree configuration.
                // The beauty of this approach is that the path we set the selection to does not actually have to be
                // resent in the tree for this to work and display the list.  But a problem (bug?) is that
                // we get the correct full path but the child that we get from that path shows no 'parent'.
                // That complicates matters when handling the selection change, but it still gets handled correctly.
                TreePath phantomPath = todolistsPath.pathByAddingChild(new DefaultMutableTreeNode(prettyName));
                theTree.setSelectionPath(phantomPath);
            } // end if
        } else if (fname.equals("UpcomingEvents")) { // This is (may be?) older, but there may still be some.  It can come out, eventually.
            // TODO - test the Found In for an event - working?  seems not.  Fix, or add better comments.
            theTree.setSelectionPath(eventsPath);
        } else if (!fpath.endsWith(MemoryBank.userDataHome)) {
            // If the path does not end at the top level data
            //   directory, then (at least at this writing) it
            //   means that we are down one of the calendar-
            //   based 'Year' paths.

            // Note that we should be able to utilize srd.getNoteDate()
            //   here, rather than having to develop it from the filename.
            //   need to look into why that is not working...
            if (fname.startsWith("Y")) {
                selectedDate = AppUtil.getDateFromFilename(srd.getFileFoundIn());
                theTree.setSelectionPath(yearNotesPath);
            } else if (fname.startsWith("M")) {
                selectedDate = AppUtil.getDateFromFilename(srd.getFileFoundIn());
                theTree.setSelectionPath(monthNotesPath);
            } else if (fname.startsWith("D")) {
                selectedDate = AppUtil.getDateFromFilename(srd.getFileFoundIn());
                theTree.setSelectionPath(dayNotesPath);
            } // end if
        } // end if
    } // end showFoundIn


    private void showHelp() {
        try {
            String s = MemoryBank.logHome + File.separatorChar;
            Runtime.getRuntime().exec("hh " + s + "MemoryBank.chm");
        } catch (IOException ioe) {
            // hh "badFile" - does NOT throw an exception, but puts up a 'cant find file' window/message.
            MemoryBank.debug(ioe.getMessage());
        } // end try/catch
    } // end showHelp


    // Called from YearView - a click on a Month name
    void showMonthView() {
        MemoryBank.debug("showMonthView called.");
        theTree.setSelectionPath(monthViewPath);
    } // end showMonthView


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
    // already today, the user is shown the textual panel
    // that is described above.
    //--------------------------------------------------------
    void showToday() {
        // Make sure that the most recent changes, if any, are preserved.
        preClose();

        // Get the current tree selection
        TreePath tp = theTree.getSelectionPath();

        if (tp == null) return; // Tree selection was cleared?
        // If the 'big' Today is already showing then there is no current tree selection.

        String theCurrentView = tp.getLastPathComponent().toString();
        MemoryBank.debug("AppTreePanel.showToday() - current path: " + theCurrentView);

        if (viewedDate.equals(LocalDate.now())) {
            theCurrentView = "Today";
        } else {
            setSelectedDate(LocalDate.now());
        }

        switch (theCurrentView) {
            case "Year View":
                theYearView.setChoice(selectedDate);
                return;
            case "Month View":
                theMonthView.setChoice(selectedDate);
                return;
            case "Day Notes": // For Day notes, the choice is not separate of the view.
                theAppDays.setDate(selectedDate);
                return;
            case "Month Notes":
                theAppMonths.setDate(selectedDate);
                return;
            case "Year Notes":
                theAppYears.setDate(selectedDate);
                return;
        }

        // If we got thru to here, it means that we were already
        //   displaying today's date when the user requested us
        //   to do so.    So - we do it bigger -
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        JLabel dayTitle = new JLabel();
        dayTitle.setHorizontalAlignment(JLabel.CENTER);
        dayTitle.setForeground(Color.blue);
        dayTitle.setFont(Font.decode("Serif-bold-24"));
        dayTitle.setText(dtf.format(selectedDate));
        rightPane.setViewportView(dayTitle);
        appMenuBar.manageMenus("Today"); // This will get the default / unhandled case.

        // Clear the current tree selection, so they can select it again
        // and get back to a 'normal' view.
        theTree.clearSelection();

    } // end showToday

    void showWeek(LocalDate theMonthToShow) {
        MemoryBank.debug("showWeek called.");
        // This method is called from external contexts such as MonthViewCanvas and YearViewCanvas.
        // There IS not actually a view to show, here.  The rightPane is
        // just loaded with the text, 'Week View'.  Therefore when this node is selected directly
        // on the tree, it does not come here but just shows the text of the request that it does
        // not know how to handle.

        //viewedDate = theMonthToShow; // NOT NEEDED until we have a week view to show.
        //viewedDateGranularity = ChronoUnit.WEEKS;
        // At that time you will also need to add handling to the selection changed area.

        theTree.setSelectionPath(weekViewPath);
    } // end showWeek


    //-----------------------------------------------------------
    // Method Name: showWorkingDialog
    //
    // This method will either show or hide a small modal
    //   dialog with an animated gif and a 'Working...' message.
    //   Call this method with 'true' before you start
    //   a potentially long task that the user must wait for,
    //   then call it with 'false' to go on.  It is static
    //   in order to give access to external classes such
    //   as group headers, that need to wait for sorting.
    //-----------------------------------------------------------
    static void showWorkingDialog(boolean showIt) {
        if (showIt) {
            // Create a new thread and setVisible within the thread.
            new Thread(new Runnable() {
                public void run() {
                    theWorkingDialog.setVisible(true);
                }
            }).start(); // Start the thread so that the dialog will show.
        } else {
            new Thread(new Runnable() {
                public void run() {
                    // Give the 'visible' time to complete, before going invisible.
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    theWorkingDialog.setVisible(false);
                }
            }).start();
        } // end if show - else hide
    } // end showWorkingDialog

    void treeSelectionChanged(TreePath newPath) {
        if (newPath == null) return;
        // You know how some animals will still move or twitch a bit after death?
        // The explanation is that their central nervous system is still sending
        // (random) signals as it shuts down, and there is enough of their body
        // still in working order well enough to react somewhat to those signals.
        // This app is similar in that some of the tests for it will run and then
        // end so quickly that some of the threads here have not finished their
        // processing.  The above line helps with that; otherwise we see null
        // exceptions on the 'node = ' line below, AFTER the test has passed.

        // Obtain a reference to the new selection.
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) (newPath.getLastPathComponent());
        // This is better than 'theTree.getLastSelectedPathComponent()' because it works for
        //   normal tree selection events but also allows for 'phantom' selections; tree
        //   paths that were created and selected by code vs those that came from a user's
        //   mouse click event on an existing (visible and active) tree node.
        if (node == null) return;

        // We have started to handle the change; now disallow
        //   further input until we are finished.
        theWorkingDialog.setLocationRelativeTo(rightPane); // Re-center before showing.
        if (!restoringPreviousSelection) showWorkingDialog(true);

        // Update the current selection row
        // Single-selection mode; Max == Min; take either one.
        appOpts.theSelectionRow = theTree.getMaxSelectionRow();

        // If there was a NoteGroup open prior to this change then update its data now.
        if (theNoteGroup != null) {
            theNoteGroup.unloadInterface(theNoteGroup.theNotePager.getCurrentPage());
        } // end if

        // Get the string for the selected node.
        String theNodeString = node.toString();
        MemoryBank.debug("New tree selection: " + theNodeString);
        appOpts.theSelection = theNodeString; // Preserved exactly, for app restart.
        String selectionContext = theNodeString;  // used in menu management; this default value may change, below.

        // Get the name of the node's parent.  Thanks to the way we have created the tree and
        // the unselectability of the tree root, we never expect the parent path to be null.
        String parentNodeName = newPath.getParentPath().getLastPathComponent().toString();

        //-----------------------------------------------------
        // These booleans will help us to avoid incorrect assumptions based on the text of the
        //   new selection, in cases where some bozo named their group the same as a parent
        //   branch.  For example, a To Do list named 'To Do Lists'.  We first look to see if
        //   the selection is a branch before we would start handling it as a leaf, and by
        //   then we will know which branch the leaf falls under.
        //-----------------------------------------------------
        boolean isGoalsBranch = theNodeString.equals("Goals");
        boolean isEventsBranch = theNodeString.equals("Upcoming Events");
        boolean isTodoBranch = theNodeString.equals("To Do Lists");
        boolean isSearchBranch = theNodeString.equals("Search Results");
        boolean isTopLevel = parentNodeName.equals("App");
        boolean isConsolidatedView = theNodeString.equals(MemoryBank.appOpts.consolidatedEventsViewName);

        theNoteGroup = null; // initialize

        //<editor-fold desc="Actions Depending on the selection">
        if (isGoalsBranch && isTopLevel) {  // Edit the Upcoming Events parent branch
            BranchHelper tbh = new BranchHelper(theTree, theGoalListKeeper, GoalGroup.areaName);
            TreeBranchEditor tbe = new TreeBranchEditor("Goals", node, tbh);
            selectionContext = "Goals Branch Editor";
            rightPane.setViewportView(tbe);
        } else if (isEventsBranch && isTopLevel) {  // Edit the Upcoming Events parent branch
            BranchHelper tbh = new BranchHelper(theTree, theEventListKeeper, EventNoteGroup.areaName);
            TreeBranchEditor tbe = new TreeBranchEditor("Upcoming Events", node, tbh);
            selectionContext = "Upcoming Events Branch Editor";
            rightPane.setViewportView(tbe);
        } else if (isTodoBranch && isTopLevel) {  // Edit the Todo parent branch
            // To Do List management - select, deselect, rename, reorder, remove
            // The 'tree' may change often.  We instantiate a new helper
            // and editor each time, to be sure all are in sync.
            BranchHelper tbh = new BranchHelper(theTree, theTodoListKeeper, TodoNoteGroup.areaName);
            TreeBranchEditor tbe = new TreeBranchEditor("To Do Lists", node, tbh);
            selectionContext = "To Do Lists Branch Editor";
            rightPane.setViewportView(tbe);
        } else if (isSearchBranch && isTopLevel) {  // Edit the Search parent branch
            BranchHelper sbh = new BranchHelper(theTree, theSearchResultsKeeper, SearchResultGroup.areaName);
            TreeBranchEditor tbe = new TreeBranchEditor("Search Results", node, sbh);
            selectionContext = "Search Results Branch Editor";
            rightPane.setViewportView(tbe);
        } else if (!node.isLeaf()) {  // Looking at other expandable nodes
            JTree jt = new JTree(node); // Show as a tree but no editing.
            jt.setShowsRootHandles(true);
            rightPane.setViewportView(jt);
        } else if (parentNodeName.equals("Goals")) { // Selection of a Goal
            selectionContext = "Goal";  // For manageMenus
            GoalGroup goalGroup;

            // If this group has been previously loaded during this session,
            // we can retrieve it from the keeper.
            goalGroup = (GoalGroup) theGoalListKeeper.get(theNodeString);

            // Otherwise load it, but only if a file for it already exists.
            if (goalGroup == null) {
                goalGroup = (GoalGroup) NoteGroupFactory.getGroup(parentNodeName, theNodeString);
                if (goalGroup != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    theTodoListKeeper.add(goalGroup);
                }
            } else {
                log.debug("Retrieved '" + theNodeString + "' from the keeper");
            }

            if (goalGroup == null) {
                // We just tried to retrieve it or to load it, so if it is STILL null
                //   then we take it to mean that the file is effectively not there.

                // We can show a notice about what went wrong and what we're
                // going to do about it, but that will only be helpful if
                // the user had just asked to see the selection, and NOT
                // in the case where this situation arose during a program
                // restart where the missing file just happens to be
                // the last selection that had been made during a previous run,
                // and now it is being restored, possibly several days later.
                if (!restoringPreviousSelection) { // We are here due to a recent user action.
                    showWorkingDialog(false);
                    JOptionPane.showMessageDialog(this,
                            "Cannot read in the Goal.\n" +
                                    "This list selection will be removed.",
                            "List not accessible", JOptionPane.WARNING_MESSAGE);
                } // end if

                closeGroup(); // File is already gone; this just removes the tree node.
            } else {
                // There is only one menu, for ALL Goals.  It needs to be reset with every list change.
                goalGroup.setListMenu(appMenuBar.getNodeMenu(selectionContext));

                theNoteGroup = goalGroup;
                rightPane.setViewportView(goalGroup.theBasePanel);
            } // end if

        } else if (parentNodeName.equals("Upcoming Events") && isConsolidatedView) { // Selection of the Consolidated Events List
            selectionContext = "Consolidated View";  // For manageMenus
            EventNoteGroup theBigPicture = getConsolidatedView();
            if (theBigPicture != null) {
                rightPane.setViewportView(theBigPicture.theBasePanel);
            }
        } else if (parentNodeName.equals("Upcoming Events")) { // Selection of an Event group
            selectionContext = "Upcoming Event";  // For manageMenus
            EventNoteGroup eventNoteGroup;

            // If this group has been previously loaded during this session,
            // we can retrieve it from the keeper.
            eventNoteGroup = (EventNoteGroup) theEventListKeeper.get(theNodeString);

            // Otherwise load it, but only if a file for it already exists.
            if (eventNoteGroup == null) {
                eventNoteGroup = (EventNoteGroup) NoteGroupFactory.getGroup(parentNodeName, theNodeString);
                if (eventNoteGroup != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    theEventListKeeper.add(eventNoteGroup);
                }
            } else {
                log.debug("Retrieved '" + theNodeString + "' from the keeper");
            }

            if (eventNoteGroup == null) {
                // We just tried to retrieve it or to load it, so if it is STILL null
                //   then we take it to mean that the file is effectively not there.

                // We can show a notice about what went wrong and what we're
                // going to do about it, but that will only be helpful if
                // the user had just asked to see the selection, and NOT
                // in the case where this situation arose during a program
                // restart where the missing file just happens to be for
                // the last selection that had been made during a previous run,
                // and now it is being restored, possibly several days later.
                if (!restoringPreviousSelection) { // We are here due to a recent user action.
                    showWorkingDialog(false);
                    JOptionPane.showMessageDialog(this,
                            "Cannot read in the Event group.\n" +
                                    "This group selection will be removed.",
                            "File not accessible", JOptionPane.WARNING_MESSAGE);
                } // end if

                closeGroup(); // File is already gone; this just removes the tree node.
            } else {
                // There is only one menu, for ALL event lists.  It needs to be reset with every list change.
                eventNoteGroup.setListMenu(appMenuBar.getNodeMenu(selectionContext));

                theNoteGroup = eventNoteGroup;
                rightPane.setViewportView(theNoteGroup.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("To Do Lists")) {
            // Selection of a To Do List
            selectionContext = "To Do List";  // For manageMenus
            TodoNoteGroup todoNoteGroup;

            // If the list has been previously loaded during this session,
            // we can retrieve the group from the keeper.
            todoNoteGroup = (TodoNoteGroup) theTodoListKeeper.get(theNodeString);

            // Otherwise load it, but only if a file for it already exists.
            if (todoNoteGroup == null) {
                todoNoteGroup = (TodoNoteGroup) NoteGroupFactory.getGroup(parentNodeName, theNodeString);
                if (todoNoteGroup != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    theTodoListKeeper.add(todoNoteGroup);
                }
            } else {
                log.debug("Retrieved '" + theNodeString + "' from the keeper");
            }

            if (todoNoteGroup == null) {
                // We just tried to retrieve it or to load it, so if it is STILL null
                //   then we take it to mean that the file is effectively not there.

                // We can show a notice about what went wrong and what we're
                // going to do about it, but that will only be helpful if
                // the user had just asked to see the selection, and NOT
                // in the case where this situation arose during a program
                // restart where the missing file just happens to be
                // the last selection that had been made during a previous run,
                // and now it is being restored, possibly several days later.
                if (!restoringPreviousSelection) { // We are here due to a recent user action.
                    showWorkingDialog(false);
                    JOptionPane.showMessageDialog(this,
                            "Cannot read in the To Do List.\n" +
                                    "This list selection will be removed.",
                            "List not accessible", JOptionPane.WARNING_MESSAGE);
                } // end if

                closeGroup(); // File is already gone; this just removes the tree node.
            } else {
                // There is only one menu, for ALL todo lists.  It needs to be reset with every list change.
                todoNoteGroup.setListMenu(appMenuBar.getNodeMenu(selectionContext));

                theNoteGroup = todoNoteGroup;
                rightPane.setViewportView(theNoteGroup.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("Search Results")) {
            // Selection of a Search Result List
            selectionContext = "Search Result";  // For manageMenus
            SearchResultGroup searchResultGroup;

            // If the search has been previously loaded during this session,
            // we can retrieve the group for it from the keeper.
            searchResultGroup = (SearchResultGroup) theSearchResultsKeeper.get(theNodeString);

            // Otherwise construct it, but only if a file for it already exists.
            if (searchResultGroup == null) {
                searchResultGroup = (SearchResultGroup) NoteGroupFactory.getGroup(parentNodeName, theNodeString);
                if (searchResultGroup != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    theSearchResultsKeeper.add(searchResultGroup);
                }
            } else {
                log.debug("Retrieved '" + theNodeString + "' from the keeper");
            }

            if (searchResultGroup == null) {
                // We just tried to retrieve it or to load it, so if it is STILL null
                //   then we take it to mean that the file is effectively not there.

                // We can show a notice about what went wrong and what we're
                // going to do about it, but that will only be helpful if
                // the user had just asked to see the search results, and NOT
                // in the case where this situation arose during a program
                // restart where the missing search results just happen to be
                // the last selection that had been made during a previous run,
                // and now it is being restored, possibly several days later.
                if (!restoringPreviousSelection) { // We are here due to a recent user action.
                    showWorkingDialog(false);
                    JOptionPane.showMessageDialog(this,
                            "Cannot read in the search results.\n" +
                                    "This search results selection will be removed.",
                            "Results not accessible", JOptionPane.WARNING_MESSAGE);
                } // end if

                closeGroup(); // File is already gone; this just removes the tree node.
            } else {
                theNoteGroup = searchResultGroup;
                rightPane.setViewportView(theNoteGroup.theBasePanel);
            } // end if
        } else if (theNodeString.equals("Year View")) {
            if (theYearView == null) {
                theYearView = new YearView(viewedDate);
                theYearView.setParent(this);
            } else {
                theYearView.setChoice(selectedDate); // To get the right choiceLabel
                theYearView.setView(viewedDate); // To show the right Year
            } // end if

            // The choice and view settings, whether they happened during construction or
            // via the 'else' branch, do not get reflected back to here, so if there was
            // no change to the view while the panel was active then the viewed date
            // granularity that we had going in, can remain unchanged after we come back
            // out.  If there WAS a view change then the granularity would have gone to
            // YEARS at that time.

            rightPane.setViewportView(theYearView);
        } else if (theNodeString.equals("Month View")) {

            if (theMonthView == null) {
                // The MonthView must be constructed with the current choice.
                // At least until after the choice label and selected day highlight are moved out of the construction path,
                // which I do intend to do at some point.
                theMonthView = new MonthView(selectedDate);
                theMonthView.setParent(this);
            } else {  // The MonthView was previously constructed.  Now we need to put it to the right choice.
                theMonthView.setChoice(selectedDate);
            }

            // If the Selected date is not the same as the Viewed date, decide which one should be shown.
            // When they differ it will be due to the user having adjusted the view
            // while on a different date-related panel, without making a selection there
            // before coming here.  So if the user was 'looking' at a different time
            // period on that other panel and then came here, we should continue to
            // show them that same time period here, as long as it was the same (or better) granularity.
            // But with only three granularity settings (currently) and DAYS keeps its Selected
            // in sync with its Viewed, currently the only possibility where this happens is
            // when coming here from MonthNotes (but that could change with future enhancements).
            if (!selectedDate.isEqual(viewedDate)) {
                if (viewedDateGranularity != ChronoUnit.YEARS) {
                    theMonthView.setView(viewedDate);
                }
            }
            rightPane.setViewportView(theMonthView);
        } else if (theNodeString.equals("Day Notes")) {
            if (theAppDays == null) {
                theAppDays = new DayNoteGroup();
                theAppDays.setParent(this);
                theAppDays.setListMenu(appMenuBar.getNodeMenu(selectionContext));
            }
            theNoteGroup = theAppDays;
            theAppDays.setDate(selectedDate);
            setViewedDate(selectedDate, ChronoUnit.DAYS);
            rightPane.setViewportView(theAppDays.theBasePanel);
        } else if (theNodeString.equals("Month Notes")) {
            if (viewedDateGranularity == ChronoUnit.YEARS) {
                viewedDate = selectedDate;
            }
            viewedDateGranularity = ChronoUnit.MONTHS;

            if (theAppMonths == null) {
                theAppMonths = new MonthNoteGroup(); // Takes current date as default initial 'choice'.
                theAppMonths.setParent(this);
                theAppMonths.setListMenu(appMenuBar.getNodeMenu(selectionContext));
            }
            theNoteGroup = theAppMonths;
            theAppMonths.setDate(viewedDate);
            rightPane.setViewportView(theAppMonths.theBasePanel);
        } else if (theNodeString.equals("Year Notes")) {
            if (theAppYears == null) {
                theAppYears = new YearNoteGroup();
                theAppYears.setParent(this);
                theAppYears.setListMenu(appMenuBar.getNodeMenu(selectionContext));
            }
            theNoteGroup = theAppYears;
            theAppYears.setDate(viewedDate); // possibly a wrong-named method.  setView ?
            viewedDateGranularity = ChronoUnit.YEARS;
            rightPane.setViewportView(theAppYears.theBasePanel);
        } else {
            // Any other as-yet unhandled node on the tree.
            // Currently - just Week View
            JPanel jp = new JPanel(new GridBagLayout());
            jp.add(new JLabel(theNodeString));
            rightPane.setViewportView(jp);
        } // end if/else if
        //</editor-fold>

        appMenuBar.manageMenus(selectionContext);
        showWorkingDialog(false); // This may have already been done, but no harm in doing it again.
    } // end treeSelectionChanged


    //-------------------------------------------------
    // Method Name:  updateTreeState
    //
    // Capture the current tree configuration in terms of node expansion/contraction
    //   and variable group contents, and put it into appOpts (AppOptions class).
    //-------------------------------------------------
    void updateTreeState(boolean updateLists) {
        appOpts.goalsExpanded = theTree.isExpanded(goalsPath);
        appOpts.eventsExpanded = theTree.isExpanded(eventsPath);
        appOpts.viewsExpanded = theTree.isExpanded(viewsPath);
        appOpts.notesExpanded = theTree.isExpanded(notesPath);
        appOpts.todoListsExpanded = theTree.isExpanded(todolistsPath);
        appOpts.searchesExpanded = theTree.isExpanded(searchresultsPath);

        //System.out.println("Divider Location: " + splitPane.getDividerLocation());
        appOpts.paneSeparator = splitPane.getDividerLocation();

        appOpts.theSelectionRow = theTree.getMaxSelectionRow();
        // Current selection text was captured when the last selection
        //    was made, but the row may have changed due to expansion
        //    or collapsing of nodes above the selection.

        if (!updateLists) return;

        // Variables reused in multiple places, below.
        DefaultMutableTreeNode leafLink;
        int numLists;

        // Preserve the names of the active Goals in the AppOptions.
        DefaultMutableTreeNode theGoalsNode = BranchHelperInterface.getNodeByName(theRootNode, "Goals");
        appOpts.goalsList.clear();

        numLists = theGoalsNode.getChildCount();
        if (numLists > 0) {
            leafLink = theGoalsNode.getFirstLeaf();
            while (numLists-- > 0) {
                String s = leafLink.toString();
                //MemoryBank.debug("  Preserving Goal: " + s);
                appOpts.goalsList.addElement(s);
                leafLink = leafLink.getNextLeaf();
            } // end while
        } // end if

        // Preserve the names of the Event Lists in the AppOptions.
        DefaultMutableTreeNode theEventsNode = BranchHelperInterface.getNodeByName(theRootNode, "Upcoming Events");
        appOpts.eventsList.clear();

        numLists = theEventsNode.getChildCount();
        if (numLists > 0) {
            leafLink = theEventsNode.getFirstLeaf();
            while (numLists-- > 0) {
                String leafName = leafLink.toString();
                //MemoryBank.debug("  Preserving Event List: " + leafName);
                appOpts.eventsList.addElement(leafName);
                leafLink = leafLink.getNextLeaf();
            } // end while
        } // end if

        // Preserve the names of the active To Do Lists in the AppOptions.
        DefaultMutableTreeNode theTodoNode = BranchHelperInterface.getNodeByName(theRootNode, "To Do Lists");
        appOpts.tasksList.clear();

        numLists = theTodoNode.getChildCount();
        if (numLists > 0) {
            leafLink = theTodoNode.getFirstLeaf();
            while (numLists-- > 0) {
                String s = leafLink.toString();
                //MemoryBank.debug("  Preserving Todo List: " + s);
                appOpts.tasksList.addElement(s);
                leafLink = leafLink.getNextLeaf();
            } // end while
        } // end if

        // Preserve the names of the active Search Results in the AppOpts.
        DefaultMutableTreeNode theSearchNode = BranchHelperInterface.getNodeByName(theRootNode, "Search Results");
        int numResults;
        appOpts.searchResultList.clear();

        numResults = theSearchNode.getChildCount();
        if (numResults > 0) {
            leafLink = theSearchNode.getFirstLeaf();
            while (numResults-- > 0) {
                String s = leafLink.toString();
                //MemoryBank.debug("  Preserving search result: " + s);
                appOpts.searchResultList.addElement(s);
                leafLink = leafLink.getNextLeaf();
            } // end while
        } // end if

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
        // This event-handling method is called due to user action to change the selection
        // and would not run if the current selection was simply being rehandled.  So - this
        // is the best place to accept that an offer to undo a deletion has been abandoned.
        appMenuBar.showRestoreOption(false);

        final TreePath newPath = e.getNewLeadSelectionPath();
        if (restoringPreviousSelection) {
            // We don't need to handle this event from a separate
            //   thread because we don't need the 'working' dialog
            //   when restoring a previous selection because the
            //   corresponding file, if any, would have already
            //   been accessed and loaded.  Although there is one
            //   exception to that, at program restart but in that
            //   case we have the splash screen and main progress bar.
            treeSelectionChanged(newPath);
        } else {
            // This is a user-directed selection;
            //   handle from a separate thread.
            new Thread(new Runnable() {
                public void run() {
                    // AppUtil.localDebug(true);
                    treeSelectionChanged(newPath);
                    // AppUtil.localDebug(false);
                }
            }).start(); // Start the thread
        }
    } // end valueChanged

} // end AppTreePanel class

