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
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Vector;


public class AppTreePanel extends JPanel implements TreeSelectionListener {
    static final long serialVersionUID = 1L; // JPanel wants this but we will not serialize.
    static AppTreePanel theInstance;  // A tricky way for a static context to call an instance method.
    static AppMenuBar appMenuBar;

    private static Logger log = LoggerFactory.getLogger(AppTreePanel.class);
    private static final int LIST_GONE = -3; // used in constr, createTree

    private Notifier optionPane;
    //-------------------------------------------------------------------

    private JTree theTree;
    private DefaultTreeModel treeModel;
    private JScrollPane rightPane;

    // Paths to expandable 'parent' nodes
    private TreePath viewsPath;
    private TreePath notesPath;
    private TreePath todolistsPath;
    private TreePath searchresultsPath;

    private static JDialog dlgWorkingDialog;
    private NoteGroup theNoteGroup; // A reference to the current selection
    private GoalPanel theGoalPanel;
    private EventNoteGroup theEvents;
    private DayNoteGroup theAppDays;
    private MonthNoteGroup theAppMonths;
    private YearNoteGroup theAppYears;
    private MonthView theMonthView;
    private YearView theYearView;
    private Vector<NoteData> noteDataVector;   // For searching
    private Vector<NoteData> foundDataVector;  // Search results
    private NoteGroupKeeper theEventListKeeper;      // keeper of all loaded Event lists.
    private NoteGroupKeeper theTodoListKeeper;       // keeper of all loaded To Do lists.
    private NoteGroupKeeper theSearchResultsKeeper;  // keeper of all loaded SearchResults.
    private SearchPanel spTheSearchPanel;
    private JPanel aboutPanel;
    private JSplitPane splitPane;

    private LocalDate currentDateChoice;
    private LocalDate showThisMonth;  // A month to be shown but not as a 'choice'.
    private String theLastTreeSelection;
    private DefaultMutableTreeNode theRootNode;

    // Predefined Tree Paths to 'leaf' nodes.
    private TreePath dayNotesPath;
    private TreePath monthNotesPath;
    private TreePath yearNotesPath;
    private TreePath monthViewPath;
    private TreePath weekViewPath;
    private TreePath eventsPath;

    private AppOptions appOpts;

    private boolean restoringPreviousSelection;

    public AppTreePanel(JFrame aFrame, AppOptions appOpts) {
        super(new GridLayout(1, 0));
        appMenuBar = new AppMenuBar();
        aFrame.setJMenuBar(appMenuBar);
        theInstance = this; // This works because we will always only have one.
        this.appOpts = appOpts;

        //<editor-fold desc="Make the 'Working...' dialog">
        dlgWorkingDialog = new JDialog(aFrame, "Working", true);
        JLabel lbl = new JLabel("Please Wait...");
        lbl.setFont(Font.decode("Dialog-bold-16"));
        String strWorkingIcon = MemoryBank.logHome + File.separatorChar;
        strWorkingIcon += "icons" + File.separatorChar + "animated" + File.separatorChar + "const_anim.gif";
        lbl.setIcon(new AppIcon(strWorkingIcon));
        lbl.setVerticalTextPosition(JLabel.TOP);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        dlgWorkingDialog.add(lbl);
        dlgWorkingDialog.pack();
        dlgWorkingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
//        dlgWorkingDialog.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
        dlgWorkingDialog.setLocationRelativeTo(this);
        //</editor-fold>

        //<editor-fold desc="Initialize the Search Panel from a new thread">
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

        currentDateChoice = LocalDate.now();

        // Restore the last selection.
        MemoryBank.update("Restoring the previous selection");
        restoringPreviousSelection = true;
        if (appOpts.theSelectionRow >= 0) theTree.setSelectionRow(appOpts.theSelectionRow);
        // If there is a 'good' value stored as the previous selection.

        // But the value COULD be higher than any 'legal' value - still need to handle that (or explain why it's ok)

        // There are a few cases where there would be no previous selection.
        // Ex: Showing the About graphic, First-time-ever for this user, lost or corrupted AppOptions.
        // In these cases leave as-is and the view will go to the About graphic.

        restoringPreviousSelection = false;

        TreePath tp = theTree.getSelectionPath();
        if (tp != null) {
            theLastTreeSelection = tp.getLastPathComponent().toString();
        }
    } // end constructor for AppTreePanel


    @SuppressWarnings("rawtypes")  // For the Enumeration, below.
    private void addNewGroup() {
        String newName;

        // Initialize the following local variables, otherwise IJ complains.
        String prompt = "no prompt";
        String title = "no title";
        TreePath groupParentPath = null;
        NoteGroupKeeper theNoteGroupKeeper = null;

        String theContext = appMenuBar.getCurrentContext();
        switch (theContext) {
            case "Upcoming Event":
            case "Upcoming Events Branch Editor":
                prompt = "Enter a name for the new Event category\n";
                prompt += "Ex: meetings, appointments, birthdays, etc";
                title = "Add a new Events category";
                groupParentPath = eventsPath;
                theNoteGroupKeeper = theEventListKeeper;
                break;
            case "To Do List":
            case "To Do Lists Branch Editor":
                prompt = "Enter a name for the new To Do List";
                title = "Add a new To Do List";
                groupParentPath = todolistsPath;
                theNoteGroupKeeper = theTodoListKeeper;
                break;
        }

        // Get user entry of a name for the new group.
        newName = JOptionPane.showInputDialog(theTree, prompt, title, JOptionPane.QUESTION_MESSAGE);

        if (newName == null) return;      // No user entry; dialog was Cancelled.
        newName = NoteGroup.prettyName(newName); // Normalize the input

        if (null == groupParentPath) return;
        String groupParentName = groupParentPath.getLastPathComponent().toString();
        DefaultMutableTreeNode groupParentNode = BranchHelperInterface.getNodeByName(theRootNode, groupParentName);

        // Declare a tree node for the new group.
        DefaultMutableTreeNode theNewGroupNode = null;

        // Allowing 'add' to act as a back-door selection of a group that actually already
        // exists is ok, but do not add this choice to the branch if it is already there.
        boolean addNodeToBranch = true;

        // So - examine the tree to see if there is already a node for the new group -
        Enumeration children = groupParentNode.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode achild = (DefaultMutableTreeNode) children.nextElement();
            if (achild.toString().equals(newName)) {
                theNewGroupNode = achild;
            }
        }

        // And now we know -
        if (theNewGroupNode == null) { // Not already a node on the tree
            theNewGroupNode = new DefaultMutableTreeNode(newName, false);
        } else { // It's already on the tree
            addNodeToBranch = false;
            // This also means that we don't need the checkFilename.
        }

        if (addNodeToBranch) {
            // Ensure that the new name meets our file-naming requirements.
            String theComplaint = BranchHelperInterface.checkFilename(newName, NoteGroup.basePath(TodoNoteGroup.areaName));
            if (!theComplaint.isEmpty()) {
                JOptionPane.showMessageDialog(theTree, theComplaint,
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
        NoteGroup theGroup = theNoteGroupKeeper.get(newName);
        if (theGroup == null) { // Not already loaded; construct one, whether there is a file for it or not.
            theGroup = NoteGroupFactory.getOrMakeGroup(theContext, newName);
            if(theGroup != null) { // It won't be, but IJ needs to be sure.
                theNoteGroupKeeper.add(theGroup);
                theGroup.setGroupChanged(true); // Save this empty group.
                theGroup.preClose();
            }
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
        // Events
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode("Upcoming Events");
        trunk.add(branch);
        pathToRoot = branch.getPath();
        eventsPath = new TreePath(pathToRoot);

        theEventListKeeper = new NoteGroupKeeper();

        for (String s : appOpts.eventLists) {
            // Add to the tree
            leaf = new DefaultMutableTreeNode(s, false);
            MemoryBank.debug("  Adding List: " + s);
            branch.add(leaf);
        } // end for
        //---------------------------------------------------


        //---------------------------------------------------
        // Goals - This will change, dramatically!!
        //     Will probably disappear as a tree leaf
        //     and instead become a branch, where selection
        //     of one of its leaves will change ALL other NoteGroups
        //      (except Notes?)
        //---------------------------------------------------
        leaf = new DefaultMutableTreeNode("Goals");
        trunk.add(leaf);

        //---------------------------------------------------
        // Views - Calendar-style displays of Year, Month, Week
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode("Views");
        trunk.add(branch);
        pathToRoot = branch.getPath();
        viewsPath = new TreePath(pathToRoot);

        leaf = new DefaultMutableTreeNode("Year View");
        branch.add(leaf);

        leaf = new DefaultMutableTreeNode("Month View");
        branch.add(leaf);

        // Convert this node to a TreePath to be used later,
        //   in selection events.
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

        theTodoListKeeper = new NoteGroupKeeper();
        theSearchResultsKeeper = new NoteGroupKeeper();

        for (String s : appOpts.todoLists) {
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

        // Do not show the 'Log' root of the tree.
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

    // Make a Consolidated View group from all the currently selected Event Groups.
    @SuppressWarnings("rawtypes")
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
                EventNoteComponent.isEditable = false; // This is a non-editable group.
                theBigGroup = new EventNoteGroup((MemoryBank.appOpts.consolidatedEventsViewName));
                EventNoteComponent.isEditable = true; // Put it back to the default value.
                continue;
            }
            // Then we can look at merging in any possible child nodes, but
            // we have to skip the one node that actually does denote the CV.
            // This one could be anywhere in the list; just skip it when we come to it.
            theNodeName = eventNode.toString();
            if (theNodeName.equals(MemoryBank.appOpts.consolidatedEventsViewName)) continue;

            // And for the others (if any) - merge them into the CV group.
            String theFilename = EventNoteGroup.getGroupFilename(theNodeName);
            System.out.println("Node: " + theNodeName + "  File: " + theFilename);

            Object[] theData = AppUtil.loadNoteGroupData(theFilename);

            NoteData.loading = true; // We don't want to affect the lastModDates!
            groupDataVector = AppUtil.mapper.convertValue(theData[0], new TypeReference<Vector<EventNoteData>>() { });
            NoteData.loading = false; // Restore normal lastModDate updating.

            if (theUniqueSet == null) {
                theUniqueSet = new LinkedHashSet<>(groupDataVector);
            } else {
                theUniqueSet.addAll(groupDataVector);
            }
        }
        if(theUniqueSet == null) return null;
        groupDataVector = new Vector<>(theUniqueSet);
        theBigGroup.addNoteAllowed = false;
        theBigGroup.setGroupData(groupDataVector);
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
        else if (what.equals("Search...")) doSearch();
        else if (what.equals("Set Options...")) ((TodoNoteGroup) theNoteGroup).setOptions();
        else if (what.startsWith("Merge")) mergeGroup();
            //else if (what.startsWith("Print")) ((TodoNoteGroup) theNoteGroup).printList();
        else if (what.equals("Review...")) System.out.println("Review was selected.");
        else if (what.equals("Save")) theNoteGroup.refresh();
        else if (what.startsWith("Save As")) saveGroupAs();
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
    // Purpose:  Preserves any unsaved changes to the current noteGroup
    //   and updates the current state of the panel into the app options,
    //   in advance of saving the options during app shutdown.
    //------------------------------------------------------------
    public void preClose() {
        // The currently active NoteGroup may need this; any others would
        //   have been saved when the view changed away from them.
        if (theNoteGroup != null) theNoteGroup.preClose();

        updateTreeState(true); // Update appOpts
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
        if (appOpts.eventsExpanded) theTree.expandPath(eventsPath);
        if (appOpts.viewsExpanded) theTree.expandPath(viewsPath);
        if (appOpts.notesExpanded) theTree.expandPath(notesPath);
        if (appOpts.todoListsExpanded) theTree.expandPath(todolistsPath);
        if (appOpts.searchesExpanded) theTree.expandPath(searchresultsPath);

    } // end resetTreeState


    private void saveGroupAs() {
        String oldName = theNoteGroup.getName();
        boolean success = false;
        NoteGroupKeeper theNoteGroupKeeper = null;
        TreePath groupParentPath = null;

        String theContext = appMenuBar.getCurrentContext();
        switch (theContext) {
            case "Upcoming Event":
                success =  ((EventNoteGroup) theNoteGroup).saveAs();
                groupParentPath = eventsPath;
                theNoteGroupKeeper = theEventListKeeper;
                break;
            case "To Do List":
                success =  ((TodoNoteGroup) theNoteGroup).saveAs();
                groupParentPath = todolistsPath;
                theNoteGroupKeeper = theTodoListKeeper;
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
            theNoteGroupKeeper.remove(newName);

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
                } else if (theFile1Name.equals("UpcomingEvents.json")) {
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
    //---------------------------------------------------------
    private void searchDataFile(File dataFile) {
        String theFilename = dataFile.getName();
        MemoryBank.debug("Searching: " + theFilename);
        noteDataVector = new Vector<>();

        Object[] theGroupData = AppUtil.loadNoteGroupData(dataFile);
        if (theGroupData != null && theGroupData[theGroupData.length - 1] != null) {
            NoteData.loading = true; // We don't want to affect the lastModDates!

            // We don't have an instantiation of a Group, so we cannot use
            // the overridden setGroupData() like we do when NoteGroup loads
            // a file.  So - we'll use the filename to determine which
            // generation of NoteData we need to recognize and convert.
            if (theFilename.equals("UpcomingEvents.json")) {
                noteDataVector = AppUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<EventNoteData>>() {
                });
            } else if ((theFilename.startsWith("M") && theFilename.charAt(3) == '_')) {
                noteDataVector = AppUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<NoteData>>() {
                });
            } else if ((theFilename.startsWith("Y") && theFilename.charAt(1) == '_')) {
                noteDataVector = AppUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<NoteData>>() {
                });
            } else if ((theFilename.startsWith("D") && theFilename.charAt(5) == '_')) {
                noteDataVector = AppUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<DayNoteData>>() {
                });
            } else if (theFilename.startsWith("todo_")) {
                noteDataVector = AppUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<TodoNoteData>>() {
                });
            }
            NoteData.loading = false; // Restore normal lastModDate updating.
        }

        // Now get on with the search -
        for (NoteData ndTemp : noteDataVector) {

            // If we find what we're looking for in/about this note -
            if (spTheSearchPanel.foundIt(ndTemp)) {

                // Make new search result data for this find.
                SearchResultData srd = new SearchResultData(ndTemp);

                // The copy constructor used above will preserve the
                //   dateLastMod of the original note.  Members specific
                //   to a SearchResultData must be set explicitly.
                srd.setFileFoundIn(dataFile);

                // Add this search result data to our findings.
                foundDataVector.add(srd);

            } // end if
        } // end for
    }//end searchDataFile

    // Used by test methods
    public void setNotifier(Notifier newNotifier) {
        optionPane = newNotifier;
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
        updateCurrentDateChoice(); // Used when restoring the previous view.

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
            updateTreeState(false);
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
            String prettyName = NoteGroup.prettyName(fname);

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
        } else if (fname.equals("UpcomingEvents")) {
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
                currentDateChoice = AppUtil.getDateFromFilename(srd.getFileFoundIn());
                theTree.setSelectionPath(yearNotesPath);
            } else if (fname.startsWith("M")) {
                currentDateChoice = AppUtil.getDateFromFilename(srd.getFileFoundIn());
                theTree.setSelectionPath(monthNotesPath);
            } else if (fname.startsWith("D")) {
                currentDateChoice = AppUtil.getDateFromFilename(srd.getFileFoundIn());
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


    void showMonth(LocalDate theMonthToShow) {
        MemoryBank.debug("showMonth called.");
        // This method is called from an external context.
        showThisMonth = theMonthToShow;
        theTree.setSelectionPath(monthViewPath);
    } // end showMonth


    private void doSearch() {
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

        if (choice != JOptionPane.OK_OPTION) return;

        if (!spTheSearchPanel.hasWhere()) {
            JOptionPane.showMessageDialog(this,
                    " No location to search was chosen!",
                    "Search conditions specification error",
                    JOptionPane.ERROR_MESSAGE);
            return;
        } // end if no search location was specified.

        dlgWorkingDialog.setLocationRelativeTo(rightPane); // This can be needed if windowed app has moved from center screen.
        showWorkingDialog(true); // Show the 'Working...' dialog; it's in a separate thread so we can keep going here...

        // Make sure that the most recent changes, if any,
        //   will be included in the search.
        if (theNoteGroup != null) {
            theNoteGroup.preClose();
        } // end if

        // Now make a Vector that can collect the search results.
        foundDataVector = new Vector<>(0, 1);

        // Now scan the user's data area for data files - we do a recursive
        //   directory search and each file is examined as soon as it is
        //   found, provided that it passes the file-level filters.
        MemoryBank.debug("Data location is: " + MemoryBank.userDataHome);
        File f = new File(MemoryBank.userDataHome);
        scanDataDir(f, 0); // Indirectly fills the foundDataVector
        noteDataVector = foundDataVector;

        // We will display the results of the search, even if it found nothing.
        SearchResultGroupProperties searchResultGroupProperties = new SearchResultGroupProperties();
        searchResultGroupProperties.setSearchSettings(spTheSearchPanel.getSettings());

        // Make a unique name for the results
        String resultsName = AppUtil.getTimestamp();
        String resultsPath = MemoryBank.userDataHome + File.separatorChar + "SearchResults" + File.separatorChar;
        String resultsFileName = resultsPath + "search_" + resultsName + ".json";
        System.out.println("Search performed at " + resultsName + " results: " + foundDataVector.size());

        // Make a new data file to hold the searchResultData list
        Object[] theGroup = new Object[2]; // A 'wrapper' for the Properties + List
        theGroup[0] = searchResultGroupProperties;
        theGroup[1] = noteDataVector;
        int notesWritten = AppUtil.saveNoteGroupData(resultsFileName, theGroup);
        if (foundDataVector.size() != notesWritten) {
            System.out.println("Possible problem - wrote " + notesWritten + " results");
        } else {
            System.out.println("Wrote " + notesWritten + " results to " + resultsFileName);
        }

        // Make a new tree node for these results and select it
        addSearchResult(resultsName);

        showWorkingDialog(false);
    } // end doSearch


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
    // already today, the user is shown the textual panel as
    // described above.
    //--------------------------------------------------------
    void showToday() {
        // Make sure that the most recent changes, if any, are preserved.
        preClose();

        // Get the current tree selection
        TreePath tp = theTree.getSelectionPath();

        if (tp == null) return; // Tree selection was cleared?
        // If the 'big' Today is already showing then there is no current tree selection.
        // Decided not to do a toggle here because the original date choice was
        // already lost as soon as they asked for 'today' the first time.  It may be
        // more helpful to just remove the menu choice in this case, but this also works.

        String theCurrentView = tp.getLastPathComponent().toString();
        MemoryBank.debug("AppTreePanel.showToday() - current path: " + theCurrentView);

        // Set the last tree selection to the current one so that the current choice update
        // will take its value from the correct tree node.
        theLastTreeSelection = theCurrentView;
        updateCurrentDateChoice();

        if (currentDateChoice.equals(LocalDate.now())) {
            theCurrentView = "Today";
        } else {
            currentDateChoice = LocalDate.now();
        }

        switch (theCurrentView) {
            case "Year View":
                theYearView.setChoice(currentDateChoice);
                return;
            case "Month View":
                theMonthView.setChoice(currentDateChoice);
                return;
            case "Day Notes":
                theAppDays.setChoice(currentDateChoice);
                return;
            case "Month Notes":
                theAppMonths.setChoice(currentDateChoice);
                return;
            case "Year Notes":
                theAppYears.setChoice(currentDateChoice);
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
        dayTitle.setText(dtf.format(currentDateChoice));
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

         // showThisMonth = theMonthToShow; // NOT NEEDED until we have a week view to show.
        // At that time you will also need to add handling to the selection changed area, and clear this var.

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
                    dlgWorkingDialog.setVisible(true);
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
                    dlgWorkingDialog.setVisible(false);
                }
            }).start();
        } // end if show - else hide
    } // end showWorkingDialog

    private void treeSelectionChanged(TreePath oldPath, TreePath newPath) {
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
        //   mouse click event on an existing tree node.
        if (node == null) return;

        // We have started to handle the change; now disallow
        //   further input until we are finished.
        dlgWorkingDialog.setLocationRelativeTo(rightPane); // Re-center before showing.
        if (!restoringPreviousSelection) showWorkingDialog(true);

        // Update the current selection row
        // Single-selection mode; Max == Min; take either one.
        appOpts.theSelectionRow = theTree.getMaxSelectionRow();

        // If there was a NoteGroup open prior to this change then save it now.
        if (theNoteGroup != null) {
            theNoteGroup.preClose();
        } // end if

        // Update the currentDateChoice so that it can be used to set the
        //   date to be shown before we display the newly selected group,
        //   if it cares about dates, that is.
        if (oldPath != null) {
            theLastTreeSelection = oldPath.getLastPathComponent().toString();
            MemoryBank.debug("Last Selection was: " + theLastTreeSelection);
            updateCurrentDateChoice();
        }

        // Get the string for the selected node.
        String theNodeString = node.toString();
        MemoryBank.debug("New tree selection: " + theNodeString);
        appOpts.theSelection = theNodeString; // Preserved exactly, for app restart.
        String selectionContext = theNodeString;  // used in menu management; this default value may change, below.

        // Get the name of the node's parent.  Thanks to the way we have created the tree and
        // the unselectability of the tree root, we never expect the parent path to be null.
        String theParent = newPath.getParentPath().getLastPathComponent().toString();

        //-----------------------------------------------------
        // These booleans will help us to avoid incorrect assumptions based on the text of the
        //   new selection, in cases where some bozo named their group the same as a parent
        //   branch.  For example, a To Do list named 'To Do Lists'.  We first look to see if
        //   the selection is a branch before we would start handling it as a leaf, and by
        //   then we will know which branch the leaf falls under.
        //-----------------------------------------------------
        boolean isEventsBranch = theNodeString.equals("Upcoming Events");
        boolean isTodoBranch = theNodeString.equals("To Do Lists");
        boolean isSearchBranch = theNodeString.equals("Search Results");
        boolean isTopLevel = theParent.equals("App");
        boolean isConsolidatedView = theNodeString.equals(MemoryBank.appOpts.consolidatedEventsViewName);

        theNoteGroup = null; // initialize

        //<editor-fold desc="Actions Depending on the selection">
        if (isEventsBranch && isTopLevel) {  // Edit the Upcoming Events parent branch
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
        } else if (theParent.equals("Upcoming Events") && isConsolidatedView) { // Selection of the Consolidated Events List
            selectionContext = "Consolidated View";  // For manageMenus
            EventNoteGroup theBigPicture = getConsolidatedView();
            rightPane.setViewportView(theBigPicture);
        } else if (theParent.equals("Upcoming Events")) { // Selection of an Event group
            selectionContext = "Upcoming Event";  // For manageMenus
            EventNoteGroup eventNoteGroup;

            // If this group has been previously loaded during this session,
            // we can retrieve it from the keeper.
            eventNoteGroup = (EventNoteGroup) theEventListKeeper.get(theNodeString);

            // Otherwise load it, but only if a file for it already exists.
            if (eventNoteGroup == null) {
                eventNoteGroup = (EventNoteGroup) NoteGroupFactory.getGroup(theParent, theNodeString);
                if (eventNoteGroup != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    eventNoteGroup.setListMenu(appMenuBar.getListMenu(selectionContext));
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
                theNoteGroup = eventNoteGroup;
                rightPane.setViewportView(theNoteGroup);
            } // end if
        } else if (theParent.equals("To Do Lists")) {
            // Selection of a To Do List
            selectionContext = "To Do List";  // For manageMenus
            TodoNoteGroup todoNoteGroup;

            // If the list has been previously loaded during this session,
            // we can retrieve the group from the keeper.
            todoNoteGroup = (TodoNoteGroup) theTodoListKeeper.get(theNodeString);

            // Otherwise load it, but only if a file for it already exists.
            if (todoNoteGroup == null) {
                todoNoteGroup = (TodoNoteGroup) NoteGroupFactory.getGroup(theParent, theNodeString);
                if (todoNoteGroup != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    todoNoteGroup.setListMenu(appMenuBar.getListMenu(selectionContext));
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
                theNoteGroup = todoNoteGroup;
                rightPane.setViewportView(theNoteGroup);
            } // end if
        } else if (theParent.equals("Search Results")) {
            // Selection of a Search Result List
            selectionContext = "Search Result";  // For manageMenus
            SearchResultGroup searchResultGroup;

            // If the search has been previously loaded during this session,
            // we can retrieve the group for it from the keeper.
            searchResultGroup = (SearchResultGroup) theSearchResultsKeeper.get(theNodeString);

            // Otherwise construct it, but only if a file for it already exists.
            if (searchResultGroup == null) {
                searchResultGroup = (SearchResultGroup) NoteGroupFactory.getGroup(theParent, theNodeString);
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
                rightPane.setViewportView(theNoteGroup);
            } // end if
        } else if (theNodeString.equals("Goals")) {
            if (theGoalPanel == null) {
                theGoalPanel = new GoalPanel();
            }
            rightPane.setViewportView(theGoalPanel);
        } else if (theNodeString.equals("Year View")) {
            if (theYearView == null) {
                theYearView = new YearView(currentDateChoice);
                theYearView.setParent(this);
            } // end if
            // Might need to use currentDateChoice to set the year, here.  and above.
            theYearView.setChoice(currentDateChoice);
            rightPane.setViewportView(theYearView);
        } else if (theNodeString.equals("Month View")) {
            if (theMonthView == null) {
                theMonthView = new MonthView(currentDateChoice);
                theMonthView.setParent(this);
            } else {
                theMonthView.setChoice(currentDateChoice);
            }
            if(showThisMonth != null) {
                theMonthView.setView(showThisMonth);
                showThisMonth = null;
            }
            rightPane.setViewportView(theMonthView);
        } else if (theNodeString.equals("Day Notes")) {
            if (theAppDays == null) {
                theAppDays = new DayNoteGroup();
                theAppDays.setListMenu(appMenuBar.getListMenu(selectionContext));
            }
            theNoteGroup = theAppDays;
            theAppDays.setChoice(currentDateChoice);
            rightPane.setViewportView(theAppDays);
        } else if (theNodeString.equals("Month Notes")) {
            if (theAppMonths == null) {
                theAppMonths = new MonthNoteGroup();
                theAppMonths.setListMenu(appMenuBar.getListMenu(selectionContext));
            }
            theNoteGroup = theAppMonths;
            theAppMonths.setChoice(currentDateChoice);
            rightPane.setViewportView(theAppMonths);
        } else if (theNodeString.equals("Year Notes")) {
            if (theAppYears == null) {
                theAppYears = new YearNoteGroup();
                theAppYears.setListMenu(appMenuBar.getListMenu(selectionContext));
            }
            theNoteGroup = theAppYears;
            theAppYears.setChoice(currentDateChoice);
            rightPane.setViewportView(theAppYears);
        } else {
            // Any other as-yet unhandled node on the tree.
            // Currently - Week View
            JPanel jp = new JPanel(new GridBagLayout());
            jp.add(new JLabel(theNodeString));
            rightPane.setViewportView(jp);
        } // end if/else if
        //</editor-fold>

        appMenuBar.manageMenus(selectionContext);
        showWorkingDialog(false); // This may have already been done, but if no then no harm in doing it again.
    } // end treeSelectionChanged


    // There are five situations where the previous selection
    //   may have allowed the user to change the displayed date.
    //   If 'theLastTreeSelection' is one of them then set the
    //   currentDateChoice to the date from that group.  This is
    //   needed when switching between these groups and also
    //   used by showToday and showAbout.
    private void updateCurrentDateChoice() {
        if (theLastTreeSelection == null) return;

        switch (theLastTreeSelection) {
            case "Year View":
                // Unlike the others, a YearView choice MAY be null.
                // This is because the YearView can be used as a Date selection interface,
                // where a 'no choice' option needs to be supported.  But it could
                // possibly be altered to require one for the Tree's YearView while not
                // requiring one when used as a selection dialog.  After all, it's not
                // static.
                LocalDate yearViewChoice = theYearView.getChoice();
                if (yearViewChoice != null) currentDateChoice = theYearView.getChoice();
                break;
            case "Month View":
                currentDateChoice = theMonthView.getChoice();
                break;
            case "Day Notes":
                currentDateChoice = theAppDays.getChoice();
                break;
            case "Month Notes":
                currentDateChoice = theAppMonths.getChoice();
                break;
            case "Year Notes":
                currentDateChoice = theAppYears.getChoice();
                break;
        }
    }


    //-------------------------------------------------
    // Method Name:  updateTreeState
    //
    // Capture the current tree configuration
    //   and put it into appOpts (AppOptions class).
    //-------------------------------------------------
    void updateTreeState(boolean updateLists) {
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

        // Preserve the names of the Event Lists in the AppOptions.
        DefaultMutableTreeNode theEventsNode = BranchHelperInterface.getNodeByName(theRootNode, "Upcoming Events");
        appOpts.eventLists.clear();

        numLists = theEventsNode.getChildCount();
        if (numLists > 0) {
            leafLink = theEventsNode.getFirstLeaf();
            while (numLists-- > 0) {
                String leafName = leafLink.toString();
                //MemoryBank.debug("  Preserving Event List: " + leafName);
                appOpts.eventLists.addElement(leafName);
                leafLink = leafLink.getNextLeaf();
            } // end while
        } // end if

        // Preserve the names of the active To Do Lists in the AppOptions.
        DefaultMutableTreeNode theTodoNode = BranchHelperInterface.getNodeByName(theRootNode, "To Do Lists");
        appOpts.todoLists.clear();

        numLists = theTodoNode.getChildCount();
        if (numLists > 0) {
            leafLink = theTodoNode.getFirstLeaf();
            while (numLists-- > 0) {
                String s = leafLink.toString();
                //MemoryBank.debug("  Preserving Todo List: " + s);
                appOpts.todoLists.addElement(s);
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

        final TreePath oldPath = e.getOldLeadSelectionPath();
        final TreePath newPath = e.getNewLeadSelectionPath();
        if (restoringPreviousSelection) {
            // We don't need to handle this event from a separate
            //   thread because we don't need the 'working' dialog
            //   when restoring a previous selection because the
            //   corresponding file, if any, would have already
            //   been accessed and loaded.  Although there is one
            //   exception to that, at program restart but in that
            //   case we have the splash screen and main progress bar.
            treeSelectionChanged(oldPath, newPath);
        } else {
            // This is a user-directed selection;
            //   handle from a separate thread.
            new Thread(new Runnable() {
                public void run() {
                    // AppUtil.localDebug(true);
                    treeSelectionChanged(oldPath, newPath);
                    // AppUtil.localDebug(false);
                }
            }).start(); // Start the thread
        }
    } // end valueChanged

} // end AppTreePanel class

