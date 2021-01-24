/*
  The primary control for the Memory Bank application; provides a menubar at
  the top, a 'tree' control on the left, and a viewing pane on the right
 */

// Quick-reference notes:
// 
// MenuBar events        - actionPerformed() --> handleMenuBar().
// Tree Selection events - valueChanged() --> treeSelectionChanged() in a new thread.

import com.fasterxml.jackson.core.type.TypeReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;


public class AppTreePanel extends JPanel implements TreePanel, TreeSelectionListener, AlteredDateListener {
    static final long serialVersionUID = 1L; // JPanel wants this but we will not serialize.
    static AppTreePanel theInstance;  // A tricky way for a static context to call an instance method.
    AppMenuBar appMenuBar;
    Map<String, JFrame> archiveWindows;

    private static final Logger log = LoggerFactory.getLogger(AppTreePanel.class);

    Notifier optionPane;
    //-------------------------------------------------------------------

    private JTree theTree;
    private DefaultTreeModel treeModel;
    private final JScrollPane rightPane;

    // Paths to expandable 'parent' nodes
    private TreePath viewsPath;
    private TreePath notesPath;
    private TreePath todolistsPath;
    private TreePath searchresultsPath;

    static JDialog theWorkingDialog;
    private NoteGroupPanel theNoteGroupPanel; // A reference to the current selection
    private NoteGroupPanel deletedNoteGroupPanel;
    DayNoteGroupPanel theAppDays;
    MonthNoteGroupPanel theAppMonths;
    YearNoteGroupPanel theAppYears;
    MonthView theMonthView;
    YearView theYearView;
    private Vector<NoteData> foundDataVector;  // Search results
    NoteGroupPanelKeeper theGoalsKeeper;          // keeper of all loaded Goals.
    NoteGroupPanelKeeper theEventListKeeper;      // keeper of all loaded Event lists.
    NoteGroupPanelKeeper theTodoListKeeper;       // keeper of all loaded To Do lists.
    NoteGroupPanelKeeper theSearchResultsKeeper;  // keeper of all loaded SearchResults.
    SearchPanel searchPanel;
    private final JPanel aboutPanel;
    private final JSplitPane splitPane;
    private TreePath theWayBack;

    private LocalDate selectedDate;  // The selected date
    private LocalDate viewedDate;    // A date to be shown but not as a 'choice'.
    private ChronoUnit viewedDateGranularity;

    private DefaultMutableTreeNode theRootNode;
    private DefaultMutableTreeNode selectedArchiveNode;

    // Predefined Tree Paths to 'leaf' nodes.
    TreePath dayNotesPath;
    TreePath monthNotesPath;
    TreePath yearNotesPath;
    TreePath yearViewPath;
    TreePath monthViewPath;
    private TreePath weekViewPath;
    private TreePath eventsPath;
    private TreePath goalsPath;

    private final AppOptions appOpts;

    boolean restoringPreviousSelection;
    boolean searching;

    public AppTreePanel(JFrame aFrame, AppOptions appOpts) {
        super(new GridLayout(1, 0));
        appMenuBar = new AppMenuBar();
        aFrame.setJMenuBar(appMenuBar);
        theInstance = this; // This works because we will always only have one AppTreePanel in this app.
        this.appOpts = appOpts;
        archiveWindows = new HashMap<>();

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
        appMenuBar.addHandler(al); // Add the above handler to all menu items.
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

        // Restore the last selection that was made in the previous session.
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

    // Decide which type of Group we are adding, get a name for it, and put it into the app tree.
    // The new group is not actually instantiated here; that happens when it is selected.
    @SuppressWarnings("rawtypes")  // For the xlint warning about Enumeration, (much farther) below.
    private void addNewGroup() {
        String newName = null;

        // Initialize the following local variables, otherwise IJ complains.
        String prompt;
        String title;
        TreePath groupParentPath;
        NoteGroupPanelKeeper theNoteGroupPanelKeeper;

        String theContext = appMenuBar.getCurrentContext();
        String areaName;
        MemoryBank.debug("Adding new group in this context: " + theContext);
        switch (theContext) {
            case "Goal":
            case "Goals Branch Editor":
                prompt = "Enter a short name for the Goal\n";
                prompt += "Ex: Graduate, Learn a Language, etc";
                title = "Add a new Goal";
                groupParentPath = goalsPath;
                theNoteGroupPanelKeeper = theGoalsKeeper;
                areaName = DataArea.GOALS.getAreaName();
                break;
            case "Upcoming Event":
            case "Upcoming Events Branch Editor":
                prompt = "Enter a name for the new Event category\n";
                prompt += "Ex: meetings, appointments, birthdays, etc";
                title = "Add a new Events category";
                groupParentPath = eventsPath;
                theNoteGroupPanelKeeper = theEventListKeeper;
                areaName = DataArea.UPCOMING_EVENTS.getAreaName();
                break;
            case "To Do List":
            case "To Do Lists Branch Editor":
                prompt = "Enter a name for the new To Do List";
                title = "Add a new To Do List";
                groupParentPath = todolistsPath;
                theNoteGroupPanelKeeper = theTodoListKeeper;
                areaName = DataArea.TODO_LISTS.getAreaName();
                break;
            default:
                return;
        }

        if(restoringPreviousSelection) {
            // In this case we are coming from a restart where a non-existent group was previously selected but
            // neither it nor any others are in this Category, now, so we landed on the Branch but at app startup
            // we didn't want to come here - so leave, quietly.
            // The node will have already been deselected, but now we need to wipe any extra menu -
            appMenuBar.manageMenus("No Selection");
        } else {
            // Get user entry of a name for the new group.
            newName = optionPane.showInputDialog(theTree, prompt, title, JOptionPane.QUESTION_MESSAGE);
            MemoryBank.debug("Name chosen for new group: " + newName);
        }
        if (newName == null) return;      // No user entry; dialog was Cancelled.

        // Here is where we might have done some grooming of the input, to possibly deconflict user input from the
        // prefixes and extensions used internally by the app.  But then, decided that (under 'normal' circumstances)
        // they wouldn't know those anyway and so we should take them at their stated intent, that they really do
        // want a group with a crappy name.  So - no call here to prettify it.

        String groupParentName = groupParentPath.getLastPathComponent().toString();
        DefaultMutableTreeNode groupParentNode = BranchHelperInterface.getNodeByName(theRootNode, groupParentName);

        // Declare a tree node for the new group.
        DefaultMutableTreeNode theNewGroupNode = null;

        // Allowing 'addNewGroup' to act as a back-door selection of a group that actually already
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

// Get this to be disassociated from NoteGroupFile; use the dataAccessorInterface and then move it -
            // Ensure that the new name meets our file-naming requirements.
            File aFile = new File(NoteGroupFile.makeFullFilename(areaName, newName));
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

            // Update tree state.  This is needed now and not only at app shutdown, because there are some functions
            // (such as linking) that need for the appOpts to be up-to-date after a new group has been added.
            updateAppOptions(true);
        }

        // Try to get this Group from the NoteGroupKeeper (the 'Add New' request might just be a back-door
        // selection rather than actually intending to make a new one).  If not found there then go ahead
        // and make a new one, and put it there.
        NoteGroupPanel theGroup = theNoteGroupPanelKeeper.get(newName);
        if (theGroup == null) { // Not already loaded; construct one, whether there is a file for it or not.
            MemoryBank.debug("Getting a new group from the factory.");
            theGroup = GroupPanelFactory.loadOrMakePanel(theContext, newName);
            assert theGroup != null; // It won't be, but IJ needs to be sure.
            if(theGroup.myNoteGroup.getGroupProperties() == null) {
                theGroup.myNoteGroup.makeGroupProperties();
            }
            theNoteGroupPanelKeeper.add(theGroup);
            // The new group will be saved by preClose().
        }

        // Expand the parent node (if needed) and select the group.
        theTree.expandPath(groupParentPath);
        updateAppOptions(true);
        theTree.setSelectionPath(groupParentPath.pathByAddingChild(theNewGroupNode));
    } // end addNewGroup


    // Adds a search result branch to the tree.
    private void addSearchResultToBranch(String searchResultName) {
        // Remove the tree selection listener while we
        //   rebuild this portion of the tree.
        theTree.removeTreeSelectionListener(this);

        // Make a new tree node for the result file whose path
        //   is given in the input parameter
        DefaultMutableTreeNode tmpNode;
        tmpNode = new DefaultMutableTreeNode(searchResultName, false);

        // Get the Search Results branch
        DefaultMutableTreeNode nodeSearchResults = BranchHelperInterface.getNodeByName(theRootNode, DataArea.SEARCH_RESULTS.toString());

        // Search Results branch may not be there, if this is the first search result.  Add it, if needed.
        if(nodeSearchResults == null) {  // No branch editor until after there is at least one search result.
//            int currentSelection = theTree.getMaxSelectionRow(); // Remember current selection.
            nodeSearchResults = new DefaultMutableTreeNode(DataArea.SEARCH_RESULTS.toString(), true);
            theRootNode.add(nodeSearchResults);
            TreeNode[] pathToRoot = nodeSearchResults.getPath();
            searchresultsPath = new TreePath(pathToRoot);

            treeModel.nodeStructureChanged(theRootNode);
            resetTreeState();
//            theTree.clearSelection();
//            theTree.setSelectionRow(currentSelection);
        }

        nodeSearchResults.add(tmpNode);
        treeModel.nodeStructureChanged(nodeSearchResults);

        // Select the new list.
        TreeNode[] pathToRoot = tmpNode.getPath();
        theTree.addTreeSelectionListener(this);
        theTree.setSelectionPath(new TreePath(pathToRoot));
        updateAppOptions(true);
    } // end addSearchResultToBranch


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
        // is the group (and not its branch) that we want to close.
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) theTree.getLastSelectedPathComponent();
        if (node == null) return; // But this is here just in case someone got here another way.
        assert node.getChildCount() == 0; // another fail-safe; the assertion should never fail.

        // Get the parent of the node to be removed -
        DefaultMutableTreeNode theParent = (DefaultMutableTreeNode) node.getParent();

        // Do not let the impending removal result in the need to process another tree selection event.
        //    (The 'event' is that the current selection is being set to null).
        theTree.removeTreeSelectionListener(this);

        // Remove this node from the tree.
        if(theParent == null) node.removeFromParent(); // theParent can be null during testing...
        else theParent.remove(node); // but otherwise this is the better method to use.

        // Redisplay the branch that had the removal (but not if it was the 'trunk')
        if (theParent != null && !theParent.toString().equals("App")) {
            treeModel.nodeStructureChanged(theParent);

            // Older approach, when there was only one possible action when selecting the parent:
            // Select the parent branch.
//            TreeNode[] pathToRoot = theParent.getPath();
//            theTree.setSelectionPath(new TreePath(pathToRoot));

            // Now, when there are two different possibilities to handling the branch selection, and because
            // this group closure may be happening either automatically at app restart or due to user action,
            // we don't want to dictate a default action for what happens next; just let the user decide.
            // So for that, we need to clear the tree selection AND the display.
            showAbout(); // This also clears the selection and updates the option lists.
            updateAppOptions(false); // Needed again after showAbout, to clear a possible 'about toggle'
        }
        // Restore the tree selection listening.
        theTree.addTreeSelectionListener(this);

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
        // Archives
        //---------------------------------------------------
        leaf = new DefaultMutableTreeNode(DataArea.ARCHIVES, false);
        trunk.add(leaf);

        //---------------------------------------------------
        // Goals
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode(DataArea.GOALS);
        trunk.add(branch);
        pathToRoot = branch.getPath();
        goalsPath = new TreePath(pathToRoot);
        theGoalsKeeper = new NoteGroupPanelKeeper();

        for (String s : appOpts.goalsList) {
            // Add to the tree
            leaf = new DefaultMutableTreeNode(s, false);
            MemoryBank.debug("  Adding List: " + s);
            branch.add(leaf);
        } // end for


        //---------------------------------------------------
        // Events
        //---------------------------------------------------
        branch = new DefaultMutableTreeNode(DataArea.UPCOMING_EVENTS);
        trunk.add(branch);
        pathToRoot = branch.getPath();
        eventsPath = new TreePath(pathToRoot);
        theEventListKeeper = new NoteGroupPanelKeeper();

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
        branch = new DefaultMutableTreeNode(DataArea.TODO_LISTS, true);
        trunk.add(branch);
        pathToRoot = branch.getPath();
        todolistsPath = new TreePath(pathToRoot);
        theTodoListKeeper = new NoteGroupPanelKeeper();

        for (String s : appOpts.tasksList) {
            // Add to the tree
            branch.add(new DefaultMutableTreeNode(s, false));
        } // end for
        //---------------------------------------------------

        //---------------------------------------------------
        // Search Results
        //---------------------------------------------------
        theSearchResultsKeeper = new NoteGroupPanelKeeper();
        BranchHelper sbh = new BranchHelper(new JTree(), theSearchResultsKeeper, DataArea.SEARCH_RESULTS);
        int resultCount = sbh.getChoices().size(); // Choices array list can be empty but not null.
        if(resultCount > 0) {  // No branch editor until after there is at least one search result.
            branch = new DefaultMutableTreeNode("Search Results", true);
            trunk.add(branch);
            pathToRoot = branch.getPath();
            searchresultsPath = new TreePath(pathToRoot);
        }

// Tree trunk change examples and getting them to show properly -
// Currently not needed; but this approach keeps coming up before we find better solutions.
//   This is the 2nd or third time now.  So while this is a crappy place to keep this example,
//   it is still better than not having one at all.
//        updateAppOptions();
//        // Preserve the current tree selection
//        int currentSelection = theTree.getMaxSelectionRow();
          // When the affected row is not zero, this could be more difficult.
          // Preserved the current selection above, because the 'insert' could increase it by one.
//        // ---------------------------
//        theRootNode.insert(new DefaultMutableTreeNode("Archive"), 0); // ADD
//        currentSelection += 1;
//        // ---------------------------
//        theRootNode.remove(0);  // REPLACE
//        theRootNode.insert(new DefaultMutableTreeNode("Archives"), 0);
//        // ---------------------------
//        theTreeModel.nodeStructureChanged(theRootNode);
//        resetTreeState();
//        theTree.clearSelection();
//        theTree.setSelectionRow(currentSelection);

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

    public void dateDecremented(LocalDate theNewDate, ChronoUnit theGranularity) {
        if (theGranularity == ChronoUnit.DAYS) selectedDate = theNewDate;
        viewedDate = theNewDate;
        viewedDateGranularity = theGranularity;
    }

    public void dateIncremented(LocalDate theNewDate, ChronoUnit theGranularity) {
        if (theGranularity == ChronoUnit.DAYS) selectedDate = theNewDate;
        viewedDate = theNewDate;
        viewedDateGranularity = theGranularity;
    }


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

    // Delete the data for this group.  (called from a menu bar action, or a test)
    // This method is provided as a more direct route to deletion than going thru
    // the BranchHelper.  This section of the code is similar to what is done there,
    // except that we keep a reference to the deleted group.  This will allow for
    // an 'undo', if desired.  This is possible because here we only delete one,
    // whereas the     editor has the capability to delete several at the same time.
    void deleteGroup() {
        // They get one warning..
        String deleteWarning;
        boolean doDelete;
        deleteWarning = "   Are you sure?" + System.lineSeparator();
        deleteWarning += "This deletion may be undone (via a menu option) but only if it is done soon," + System.lineSeparator();
        deleteWarning += "before almost all other actions.  If the deletion is undone it will reappear" + System.lineSeparator();
        deleteWarning += "temporarily and can be restored to the tree via the Branch Editor.";
        doDelete = optionPane.showConfirmDialog(theTree, deleteWarning,
                "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
        if (!doDelete) return;

        // Preserve the reference to the group to be deleted.
        // This may be used if there is an 'undo' of the delete.
        deletedNoteGroupPanel = theNoteGroupPanel;

        // Now we need to close the group.
        closeGroup(); // This will change the selection and null out 'theNoteGroup'.

        // Now remove the underlying data from the repository
        deletedNoteGroupPanel.myNoteGroup.deleteNoteGroup();

        // Now make sure that the group will not be saved upon app exit.
        // That could happen if it is still being held in its DataGroupKeeper.
        // But that is unlikely, given that after this we will remove it from its keeper.
        deletedNoteGroupPanel.setGroupChanged(false);

        // Now - remove it from its keeper.  This will avoid any rename collision if
        // some other group takes this one's name after it was deleted but this group was
        // still being held in a keeper.
        theNoteGroupPanel.myKeeper.remove(theNoteGroupPanel.getGroupName());

        // And finally, give the user an 'undo' capability.  This will be a one-time, one group
        //   only option.  After the 'close' above, they will have gone back to the 'About' graphic.
        //   If they then go on to some other list, the 'undo' menu option goes away.
        appMenuBar.showRestoreOption(true);
        appMenuBar.manageMenus(appMenuBar.getCurrentContext());
    }// end deleteGroup


    private void doCreateArchive() {
        String theMessage = "This allows you to take a snapshot of the current content of this\n";
              theMessage += "application, preserving your active Goals, To Do Lists, etc, as  \n";
              theMessage += "they are at this precise moment, for later review.  The notes will\n";
              theMessage += "continue to evolve in the main application but an archive will hold\n";
              theMessage += "these earlier versions which can sometimes help when reviewed at a\n";
              theMessage += "later date when you are wondering - 'what was going on back then?'";

        int choice = JOptionPane.showConfirmDialog(
                this,
                theMessage,
                "Create a new Archive ?", // pane title bar
                JOptionPane.OK_CANCEL_OPTION, // Option type
                PLAIN_MESSAGE);    // Message type

        if (choice == JOptionPane.OK_OPTION) {
            System.out.println("\nCreating an archive: \n");
            // Save anything that is currently in progress.
            preClose();
            AppOptions.saveOpts();

            // Preserve the current tree selection
            int currentSelection = theTree.getMaxSelectionRow();

            // Create the archive
            if(MemoryBank.dataAccessor.createArchive()) {
                // The reselect is only needed if we are showing the archive listing.
                    if(currentSelection == 0) {
                        theTree.clearSelection();
                        theTree.setSelectionRow(currentSelection);
                    }
                MemoryBank.debug("Archive creation was successful.");
            } else {
                MemoryBank.debug("Unable to create the archive!");
            }
        } else {
            System.out.println("Archiving was cancelled.");
        }
    }

    // Handling for the 'Go Back' menu item, to go back to Search Results after
    //   viewing one of its 'FoundIn' items.
    private void doGoBack() {
        // If menu management is being done correctly then 'theWayBack' will never be null
        //   when execution comes here to this method.  So, not going to condition this call.
        // For testers - be aware of this; changing this code for a test-only situation - not going to happen.
        theTree.setSelectionPath(theWayBack);
    }


    private void doRemoveArchive() {
        // With correct menu management, we cannot arrive here without having a value in the selecteArchiveNode.
        if(selectedArchiveNode == null) return;

        // Get the archive's name from the selected tree node.
        String archiveName = selectedArchiveNode.toString();
        System.out.println("Selected Archive: " + archiveName);

        // Close any windows that are currently open for this Archive -
        Set<String> setCodes = archiveWindows.keySet();
        for (String code : setCodes) {
            if (code.startsWith(archiveName)) {
                JFrame theArchiveWindow = archiveWindows.get(code);
                theArchiveWindow.setVisible(false);
            }
        }

        // Convert the archive name into an archive date
        LocalDateTime localDateTime = MemoryBank.dataAccessor.getDateTimeForArchiveName(archiveName);

        // Remove the indicated archive
        if (MemoryBank.dataAccessor.removeArchive(localDateTime)) {
            int currentSelection = theTree.getMaxSelectionRow();
                // The reselect is only needed if we are showing the archive listing.
                if(currentSelection == 0) {
                    theTree.clearSelection();
                    theTree.setSelectionRow(currentSelection);
                }
        } else {
            optionPane.showMessageDialog(theTree, "Archive removal failed!");
        }
    }


    // Present the user with a dialog whereby they may specify the parameters of
    // their search, then send those parameters to the 'doSearch' method.
    private void prepareSearch() {
        searching = true;
        searchPanel = new SearchPanel();
        Frame theFrame = JOptionPane.getFrameForComponent(this);

        // Now display the search dialog.
        String string1 = "Search Now";
        String string2 = "Cancel";
        Object[] options = {string1, string2};
        int choice = optionPane.showOptionDialog(theFrame,
                searchPanel,
                "Search - Please specify the conditions for your quest",
                JOptionPane.OK_CANCEL_OPTION,
                PLAIN_MESSAGE,
                null,     //don't use a custom Icon
                options,  //the titles of buttons
                string1); //the title of the default button

        if (choice != JOptionPane.OK_OPTION) {
            searching = false;
            return;
        }

        if (!searchPanel.hasWhere()) {
            JOptionPane.showMessageDialog(this,
                    " No location to search was chosen!",
                    "Search conditions specification error",
                    JOptionPane.ERROR_MESSAGE);
            searching = false;
            return;
        } // end if no search location was specified.

        // Make sure that the most recent changes, if any, will be included in the search.
        preClose();

        theWorkingDialog.setLocationRelativeTo(rightPane); // This can be needed if windowed app has moved from center screen.
        showWorkingDialog(true); // Show the 'Working...' dialog; it's in a separate thread so we can keep going here...

        doSearch(searchPanel.getSettings());
    } // end prepareSearch


    void doSearch(SearchPanelSettings searchPanelSettings) {
        // We will display the results of the search, even if it finds nothing.
        MemoryBank.debug("Running a Search with these settings: " + AppUtil.toJsonString(searchPanelSettings));

        // Now make a Vector that can collect the search results.
        foundDataVector = new Vector<>();

        // Now scan the user's data area for data files - we do a recursive
        //   directory search and each file is examined as soon as it is
        //   found, provided that it passes the file-level filters.
        MemoryBank.debug("Data location is: " + MemoryBank.userDataHome);
        File f = new File(MemoryBank.userDataHome);
        scanDataDir(f, 0); // Indirectly fills the foundDataVector

        // Make a unique name for the results
        String resultsName;
        resultsName = NoteGroupFile.getTimestamp();
        SearchResultGroup theResultsGroup = new SearchResultGroup(new GroupInfo(resultsName, GroupType.SEARCH_RESULTS));
        SearchResultGroupProperties searchResultGroupProperties = (SearchResultGroupProperties) theResultsGroup.getGroupProperties();
        searchResultGroupProperties.setSearchSettings(searchPanelSettings);
        System.out.println("Search performed at " + resultsName + " results: " + foundDataVector.size());

        theResultsGroup.setNotes(foundDataVector);
        // We allow the search to be saved even without results because what was searched for, and when, is also important.
        theResultsGroup.saveNoteGroup();

        // After-actions needed by prepareSearch
        searching = false;
        showWorkingDialog(false);

        // Make (and select) a new tree node for these results.
        addSearchResultToBranch(resultsName);
    } // end doSearch


    void doViewArchive(String archiveName) {
        MemoryBank.debug("Selected Archive: " + archiveName);
        new ArchiveTreePanel(archiveName); // It shows itself in a new window; a reference to it is not needed.
    } // end doViewArchive


    // Make a Consolidated View group from all the currently selected Event Groups.
    @SuppressWarnings({"rawtypes"})
    private EventNoteGroupPanel getConsolidatedView() {
        // First, get all the nodes that are currently under Upcoming Events.
        DefaultMutableTreeNode eventsNode = BranchHelperInterface.getNodeByName(theRootNode, "Upcoming Events");
        Enumeration e = eventsNode.breadthFirstEnumeration();
        String theNodeName;
        EventNoteGroupPanel theBigGroup = null;
        Vector<NoteData> groupDataVector;
        LinkedHashSet<NoteData> theUniqueSet = null;
        while (e.hasMoreElements()) { // A bit of unintentional mis-direction, here.
            // The first node that we get this way - is the expandable node itself - Upcoming Events.
            DefaultMutableTreeNode eventNode = (DefaultMutableTreeNode) e.nextElement();
            // So we don't actually use it.
            if (theBigGroup == null) {
                // Instead, we instantiate a new (empty) EventNoteGroup, that will be used to show scheduled events.
                theBigGroup = new EventNoteGroupPanel("Scheduled Events");
                theBigGroup.setEditable(false);
                continue;
            }
            // Then we can look at merging any possible child nodes into the CV group.
            theNodeName = eventNode.toString();
            String theFilename = NoteGroupFile.makeFullFilename(DataArea.UPCOMING_EVENTS.getAreaName(), theNodeName);
            MemoryBank.debug("Node: " + theNodeName + "  File: " + theFilename);
            Object[] theData = NoteGroupFile.loadFileData(theFilename);
            BaseData.loading = true; // We don't want to affect the lastModDates!
            groupDataVector = AppUtil.mapper.convertValue(theData[theData.length - 1], new TypeReference<Vector<EventNoteData>>() {
            });
            BaseData.loading = false; // Restore normal lastModDate updating.

            if (theUniqueSet == null) {
                theUniqueSet = new LinkedHashSet<>(groupDataVector);
            } else {
                theUniqueSet.addAll(groupDataVector);
            }
        }
        if (theUniqueSet == null) return null;
        groupDataVector = new Vector<>(theUniqueSet);
        theBigGroup.setEditable(false);
        theBigGroup.showGroupData(groupDataVector);
        theBigGroup.doSort();
        return theBigGroup;
    } // end getConsolidatedView


    AppMenuBar getAppMenuBar() { return appMenuBar; }


    NoteGroupPanel getPanelFromKeeper(GroupInfo groupInfo) {
        return getPanelFromKeeper(groupInfo.groupType, groupInfo.getGroupName());
    }


    // Get the requested Panel from the appropriate keeper.
    // Usage of this method relates to linking.  Since SearchResults cannot be linked,
    //      they are not addressed.
    // If the requested group is not in its keeper, a null is returned.
    NoteGroupPanel getPanelFromKeeper(GroupType theType, String theName) {
        NoteGroupPanel noteGroupPanel = null;
        switch (theType) {
            case GOALS:
                noteGroupPanel = theGoalsKeeper.get(theName);
                break;
            case EVENTS:
                noteGroupPanel = theEventListKeeper.get(theName);
                break;
            case TODO_LIST:
                noteGroupPanel = theTodoListKeeper.get(theName);
                break;
            case NOTES:
                switch (theName) {
                    case "Day Notes":
                        noteGroupPanel = theAppDays;
                        break;
                    case "Month Notes":
                        noteGroupPanel = theAppMonths;
                        break;
                    case "Year Notes":
                        noteGroupPanel = theAppYears;
                        break;
                }
                break;
            case DAY_NOTES:
                if(theAppDays != null) {
                    String theTitle = theAppDays.getTitle();
                    // if theAppDays is set to the named group then we use it.
                    if (theTitle.equals(theName)) noteGroupPanel = theAppDays;
                }
                break;
            case MONTH_NOTES:
                if(theAppMonths != null) {
                    String theTitle = theAppMonths.getTitle();
                    // if theAppMonths is set to the named group then we use it.
                    if (theTitle.equals(theName)) noteGroupPanel = theAppMonths;
                }
                break;
            case YEAR_NOTES:
                if(theAppYears != null) {
                    String theTitle = theAppYears.getTitle();
                    // if theAppYears is set to the named group then we use it.
                    if (theTitle.equals(theName)) noteGroupPanel = theAppYears;
                }
                break;
        }
        return noteGroupPanel;
    }

    // Used by Test
    NoteGroupPanel getTheNoteGroup() {
        return theNoteGroupPanel;
    }

    public JTree getTree() {
        return theTree;
    }

    // This is useful for tests; no so much for normal app operation.
    JComponent getViewedComponent() {
        JViewport viewport = rightPane.getViewport();
        return (JComponent) viewport.getView();
    }


    private void handleMenuBar(String what) {
        if (what.equals("Exit")) System.exit(0);
        else if (what.equals("About")) showAbout();
        else if (what.equals("Archive...")) doCreateArchive();
        else if (what.equals("Add New...")) addNewGroup();
        else if (what.equals("Close")) closeGroup();
        else if (what.startsWith("Clear ")) theNoteGroupPanel.clearAllNotes();
        else if (what.equals("Contents")) showHelp();
        else if (what.equals("Go Back")) doGoBack();
        else if (what.equals("Linkages...")) theNoteGroupPanel.groupLinkages();
        else if (what.equals("Review Mode")) toggleReview();
        else if (what.equals("Show Scheduled Events")) showEvents();
        else if (what.equals("Show Current NoteGroup")) showCurrentNoteGroup();
        else if (what.equals("Show Keepers")) showKeepers();
        else if (what.equals("Delete")) deleteGroup();
        else if (what.equals("Search...")) prepareSearch();
        else if (what.equals("Set Options...")) ((TodoNoteGroupPanel) theNoteGroupPanel).setOptions();
        else if (what.startsWith("Merge")) mergeGroup();
            //else if (what.startsWith("Print")) ((TodoNoteGroup) theNoteGroup).printList();
        else if (what.equals("Remove")) doRemoveArchive();
        else if (what.equals("Review...")) ((SearchResultGroupPanel) theNoteGroupPanel).doReview();
        else if (what.equals("Save")) theNoteGroupPanel.refresh();
        else if (what.startsWith("Save As")) saveGroupAs();
        else if (what.equals("Undo Delete")) {
            appMenuBar.manageMenus(appMenuBar.getCurrentContext());
            deletedNoteGroupPanel.setGroupChanged(true);
            deletedNoteGroupPanel.preClosePanel(); // saves it, possibly for the first time.
            deletedNoteGroupPanel.myKeeper.add(deletedNoteGroupPanel); // a bit circular, but necessary.
            rightPane.setViewportView(deletedNoteGroupPanel.theBasePanel);
            System.out.println("Did it.");
            deletedNoteGroupPanel = null;
            appMenuBar.showRestoreOption(false);

            //treeSelectionChanged(theTree.getSelectionPath()); // Reload the branch editor, to show the 'new' file.
        }
        else if (what.equals("View")) doViewArchive(selectedArchiveNode.toString());
        else if (what.equals("Icon Manager...")) {
            theTree.clearSelection();
            JPanel jp = new JPanel(new GridBagLayout());
            jp.add(new JLabel(what));
            appMenuBar.manageMenus("Icons"); // This will get the default / unhandled case.
            rightPane.setViewportView(jp);
        } else if (what.equals("Today")) showToday();
        else if (what.equals("Undo All")) {
            theNoteGroupPanel.updateGroup(); // reload without save
        } else {
            AppUtil.localDebug(true);
            MemoryBank.debug("  " + what);
            AppUtil.localDebug(false);
        } // end if/else
    } // end handleMenuBar

    private void mergeGroup() {
        preClose(); // Close everything that might be open.
        String theContext = appMenuBar.getCurrentContext();
        switch (theContext) {
            case "Upcoming Event":
                ((EventNoteGroupPanel) theNoteGroupPanel).merge();
                break;
            case "To Do List":
                ((TodoNoteGroupPanel) theNoteGroupPanel).merge();
                break;
        }
    } // end mergeGroup


    // Purpose:  Preserves any unsaved changes to all NoteGroups
    //   and updates the current state of the tree into the app options,
    //   in advance of saving the options during app shutdown.
    public void preClose() {
        if (theAppDays != null) theAppDays.preClosePanel();
        if (theAppMonths != null) theAppMonths.preClosePanel();
        if (theAppYears != null) theAppYears.preClosePanel();
        theGoalsKeeper.saveAll();
        theEventListKeeper.saveAll();
        theTodoListKeeper.saveAll();
        theSearchResultsKeeper.saveAll(); // Needed when fixing data or after sorting.

        updateAppOptions(true); // Capture expansion states into appOpts
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
    //   rest of this method is needed to re-expand any nodes that
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
        String oldName = theNoteGroupPanel.getGroupName();
        boolean success = false;
        NoteGroupPanelKeeper theNoteGroupPanelKeeper = null;
        TreePath groupParentPath = null;

        String theContext = appMenuBar.getCurrentContext();
        switch (theContext) {
            case "Upcoming Event":
                success = ((EventNoteGroupPanel) theNoteGroupPanel).saveAs();
                groupParentPath = eventsPath;
                theNoteGroupPanelKeeper = theEventListKeeper;
                break;
            case "To Do List":
                success = ((TodoNoteGroupPanel) theNoteGroupPanel).saveAs();
                groupParentPath = todolistsPath;
                theNoteGroupPanelKeeper = theTodoListKeeper;
                break;
        }
        if (null == groupParentPath) return; // Should not happen in normal operation.

        if (success) {
            String newName = theNoteGroupPanel.getGroupName();

            // When the tree selection changes, any open NoteGroup is automatically saved,
            // and the tree selection will change automatically when we do the rename of
            // the leaf on the tree below.  But in this case we do not want that behavior,
            // because we have already saved the file, milliseconds ago.  It wouldn't hurt
            // to let it save again, but why allow it, when all it takes to stop it is:
//            theNoteGroupPanel = null;

            // Removal from the NoteGroupKeeper is needed, to force a file reload
            // during the rename of the leaf (below), because even though the saveAs
            // operation changed the name of the list held by the ListKeeper, it
            // still shows a title that was developed from the old file name.
            // Reloading from the file with the new name will fix that.
            theNoteGroupPanelKeeper.remove(newName);

            // Rename the leaf.
            // This will refresh the branch and reselect the same tree row to
            // cause a reload and redisplay of the group.  Note that not only does the
            // leaf name change, but the reload also changes the displayed group title.
            String groupParentName = groupParentPath.getLastPathComponent().toString();
            DefaultMutableTreeNode groupParentNode = BranchHelperInterface.getNodeByName(theRootNode, groupParentName);
            renameTreeBranchLeaf(groupParentNode, oldName, newName);
        }
    } // end saveGroupAs


    // This method scans a directory for data files.  If it finds a directory rather than a file,
    //   it will recursively call itself for that directory.
    //
    // The SearchPanel interface follows a 'filter out' plan.  To support that, this method starts
    //   with the idea that ALL files will be searched and then considers the filters, to eliminate
    //   candidate files.  If a file is not eliminated after the filters have been considered, the
    //   search method is called for that file.
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
                String theGroupName = NoteGroupFile.getGroupNameFromFilename(theFile1Name);
                if (theFile1Name.startsWith("goal_")) {
                    if (!searchPanel.searchGoals()) {
                        goLook = false;
                    } else {
                        if(!appOpts.active(GroupType.GOALS, theGroupName)) goLook = false;
                    }
                } else if (theFile1Name.startsWith("event_")) {
                    if (!searchPanel.searchEvents()) {
                        goLook = false;
                    } else {
                        if(!appOpts.active(GroupType.EVENTS, theGroupName)) goLook = false;
                    }
                } else if (theFile1Name.startsWith("todo_")) {
                    if (!searchPanel.searchLists()) {
                        goLook = false;
                    } else {
                        if(!appOpts.active(GroupType.TODO_LIST, theGroupName)) goLook = false;
                    }
                } else if ((theFile1Name.startsWith("D")) && (level > 0)) {
                    if (!searchPanel.searchDays()) goLook = false;
                } else if ((theFile1Name.startsWith("M")) && (level > 0)) {
                    if (!searchPanel.searchMonths()) goLook = false;
                } else if ((theFile1Name.startsWith("Y")) && (level > 0)) {
                    if (!searchPanel.searchYears()) goLook = false;
                } else { // Any other file type not covered above.
                    // This includes search results (for now - SCR0073)
                    goLook = false;
                } // end if / else if

                // Check the Note date, possibly filter out based on 'when'.

                if (goLook) {
                    if(searchPanel.getWhenSetting() != -1) {
                        // This section is only needed if the user has specified a date in the search.
                        dateNoteDate = NoteGroupFile.getDateFromFilename(theFile);
                        if (dateNoteDate != null) {
                            if (searchPanel.filterWhen(dateNoteDate)) goLook = false;
                        } // end if
                    }
                } // end if


                // The Last Modified date of the FILE is not necessarily the same as the Note, but
                //   it CAN be considered when looking for a last mod AFTER a certain date, because
                //   the last mod to ANY note in the file CANNOT be later than the last mod to the
                //   file itself.  Of course this depends on having no outside mods to the filesystem
                //   but we assume that because this is either a dev system (and we trust all devs :)
                //   or the app is being served from a server where only admins have access (and we
                //   trust all admins, of course).
                if (goLook) {
                    if(searchPanel.getLastModSetting() != -1) {
                        // This section is only needed if the user has specified a date in the search.
                        LocalDate dateLastMod = Instant.ofEpochMilli(theFile.lastModified()).atZone(ZoneId.systemDefault()).toLocalDate();
                        if (searchPanel.getLastModSetting() == SearchPanel.AFTER) {
                            if (searchPanel.filterLastMod(dateLastMod)) goLook = false;
                        } // end if
                    }
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
    // Item-level filtering is not done; date-filtering
    //   is done against Calendar notes, by using their
    //   filename, only.  Todo items will all just pass thru
    //   the filter so if not desired, don't search there.
    //---------------------------------------------------------
    private void searchDataFile(File dataFile) {
        //MemoryBank.debug("Searching: " + dataFile.getName());  // This one is a bit too verbose.
        Vector<AllNoteData> searchDataVector = null;

        // Load the file
        Object[] theGroupData = NoteGroupFile.loadFileData(dataFile);
        if (theGroupData != null && theGroupData[theGroupData.length - 1] != null) {
            // During a search these notes would not be re-preserved anyway, but the reason we care is that
            // the search parameters may have specified a date-specific search; we don't want all Last Mod
            // dates to get updated to this moment and thereby muck up the search results.
            BaseData.loading = true;
            searchDataVector = AppUtil.mapper.convertValue(theGroupData[theGroupData.length - 1], new TypeReference<Vector<AllNoteData>>() { });
            BaseData.loading = false;
        }
        if (searchDataVector == null) return;

        // Now get on with the search -
        for (AllNoteData vectorItem : searchDataVector) {

            // If we find what we're looking for in/about this note -
            if (searchPanel.foundIt(vectorItem)) {
                // Get the 'foundIn' info -
                // Although this is actually a GroupInfo, we do not need to populate the foundIn.groupId
                //   because search results are not intended to themselves be a part of the traceability chain,
                //   and they cannot be linked to or from.  Currently, the method below does not read in the
                //   data, so it cannot provide the groupId.
                GroupInfo foundIn = NoteGroupFile.getGroupInfoFromFile(dataFile);

                // Make new search result data for this find.
                SearchResultData srd = new SearchResultData(vectorItem);

                // The copy constructor used above will preserve the
                //   dateLastMod of the original note.  Members specific
                //   to a SearchResultData must be set explicitly.
                srd.foundIn = foundIn; // No need to 'copy' foundIn; in this case it can be reused.

                // Add this search result data to our findings.
                foundDataVector.add(srd);

            } // end if
        } // end for
    }// end searchDataFile

    @Override
    public void setSelectedDate(LocalDate theSelection) {
        selectedDate = theSelection;
        viewedDate = theSelection;
        viewedDateGranularity = ChronoUnit.DAYS;
    }

    @Override
    public void setViewedDate(int theYear) {
        viewedDate = LocalDate.of(theYear, viewedDate.getMonth(), viewedDate.getDayOfMonth());
        viewedDateGranularity = ChronoUnit.YEARS;
    }

    @Override
    public void setViewedDate(LocalDate theViewedDate, ChronoUnit theGranularity) {
        viewedDate = theViewedDate;
        viewedDateGranularity = theGranularity;
    }

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
    void showAbout() {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                theTree.getLastSelectedPathComponent();

        // Examine the rightPane to see if the About graphic is already shown, or not.
        // There are simpler ways (such as aboutPanel.isShowing(), but that does not
        // work with the toggle test.
        JViewport viewport = rightPane.getViewport();
        JComponent theContent = (JComponent) viewport.getView();

        boolean toggle = (!restoringPreviousSelection && node == null & aboutPanel == theContent);
        if (toggle) { // This means we can go back to a previous tree selection.
            // Reset tree to the state it was in before.
            restoringPreviousSelection = true;
            if (appOpts.goalsExpanded) theTree.expandPath(goalsPath);
            else theTree.collapsePath(goalsPath);
            if (appOpts.eventsExpanded) theTree.expandPath(eventsPath);
            else theTree.collapsePath(eventsPath);
            if (appOpts.viewsExpanded) theTree.expandPath(viewsPath);
            else theTree.collapsePath(viewsPath);
            if (appOpts.notesExpanded) theTree.expandPath(notesPath);
            else theTree.collapsePath(notesPath);
            if (appOpts.todoListsExpanded) theTree.expandPath(todolistsPath);
            else theTree.collapsePath(todolistsPath);
            theTree.setSelectionRow(appOpts.theSelectionRow);
            restoringPreviousSelection = false;
        } else {
            // Capture the current state; we may have to 'toggle' back to it.
            updateAppOptions(true); // Now updating lists every time, due to link target selection.
            theTree.clearSelection();

            // Show it.
            rightPane.setViewportView(aboutPanel);

            appMenuBar.manageMenus("About"); // This will get the default / unhandled case.
        } // end if
    } // end showAbout


    // Create and show an Archive panel
    // Content may start out with informative text, if there are few enough archives.
    void showArchives() {
        JPanel archivePanel = new JPanel(new BorderLayout());
        String[] archiveNames = MemoryBank.dataAccessor.getArchiveNames();
        int archiveCount = archiveNames == null ? 0 : archiveNames.length;

        // In the base container, the NORTH area will hold a Box.
        // The Box will have rows; first row is the title.
        Box myBox = new Box(BoxLayout.Y_AXIS);
        JLabel theHeading = new JLabel(("  Archives"));
        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        theHeading.setHorizontalAlignment(SwingConstants.LEFT);
        theHeading.setFont(Font.decode("Serif-bold-24"));
        titleRow.add(theHeading);
        myBox.add(titleRow);

        JPanel infoPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 10));
        infoPanel.add(new JLabel(" ")); // This (plus the hgap) is the left side margin/indent.
        JTextArea theTextArea = new JTextArea();
        theTextArea.setLineWrap(true);
        theTextArea.setWrapStyleWord(true);
        theTextArea.setOpaque(false);
        theTextArea.setFont(Font.decode("Serif-bold-14"));
        String theInfo = "The Memory Bank application is already archiving the notable aspects of your life by ";
        theInfo += "mapping them onto the calendar by way of your date and time specific entries, but to go ";
        theInfo += "just a bit beyond that, this feature also preserves your Goals, To Do Lists, ";
        theInfo += "and other notes in the same state that they were on the day that the archive was created.  ";
        theInfo += "These lists all evolve over time, but this feature allows you to look back and see them as ";
        theInfo += "they were at the moment that you made the archive.  Suggestion is to make a new archive at least ";
        theInfo += "once a year, possibly on a birthday or anniversary. ";
        theTextArea.setText(theInfo);

        theTextArea.setEditable(false);
        JPanel infoRow = new JPanel(new DndLayout()); // Dnd, to stretch the TextArea content to its horizontal limit.
        infoRow.setPreferredSize(new Dimension(500, 160));
        infoRow.add(theTextArea, "Stretch");

        // If our list of archives has gotten too long then the screen real estate taken up by the info message can
        // be reclaimed; after this many archives they really don't need to see the justification for having them.
        // But they may want to occasionally review that message, so now we move it from the default screen and up
        // into the tool tip of the heading.
        if (archiveCount > 15) {
            // Make a tool tip - add line breaks and wrap in HTML to PREserve multi-line.
            // Tool Tip is only to be added after we get so many archives that we start hiding the info row.
            String theToolTip = "<html><pre>" + AppUtil.getTooltipString(theInfo) + "</pre></html>";
            theHeading.setToolTipText(theToolTip);
            myBox.add(new JLabel(" ")); // Spacer before the archives start, below this.
        } else {
            infoPanel.add(infoRow);
            myBox.add(infoPanel);
        }
        archivePanel.add(myBox, BorderLayout.NORTH);

        if(archiveCount > 0) {  // There are already archives - show the selectable list.
            // Build a single-level tree of archive leaves
            DefaultMutableTreeNode archiveTreeRootNode = new DefaultMutableTreeNode("Archives");

            for (String aName : archiveNames) {
                //jScrollPane.add(new Label(aName));
                DefaultMutableTreeNode aNode = new DefaultMutableTreeNode(aName);
                archiveTreeRootNode.add(new DefaultMutableTreeNode(aName));
            }
            // Create a default model based on the archive node, and create the tree from that model.
            TreeModel archiveTreeModel = new DefaultTreeModel(archiveTreeRootNode);
            //UIManager.put("Tree.rendererFillBackground", false); // Works ok for unselected but not selected.
            JTree archiveTree = new JTree(archiveTreeModel);
            // Set a tree cell renderer that does not override the background of unselected nodes.
            archiveTree.setCellRenderer(new DefaultTreeCellRenderer() {
                static final long serialVersionUID = 1L; // JPanel wants this but we will not serialize.
                @Override
                public Color getBackgroundNonSelectionColor() {
                    return (null);
                }

                @Override
                public Color getBackground() {
                    return (null);
                }
            });

            // Set to single selection mode.
            archiveTree.getSelectionModel().setSelectionMode
                    (TreeSelectionModel.SINGLE_TREE_SELECTION);

            archiveTree.addTreeSelectionListener(new TreeSelectionListener() {
                @Override
                public void valueChanged(TreeSelectionEvent e) {
                    appMenuBar.manageMenus("One Archive");
                    TreePath newPath = e.getNewLeadSelectionPath();
                    if (newPath == null) return;

                    // Obtain a reference to the new selection.
                    selectedArchiveNode = (DefaultMutableTreeNode) (newPath.getLastPathComponent());
                }
            });

            MouseListener ml = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if(e.getClickCount() == 2) {
                        System.out.println("Going directly to view: " + selectedArchiveNode.toString());
                        doViewArchive(selectedArchiveNode.toString());
                    }
                }
            };
            archiveTree.addMouseListener(ml);

            // Do not show the root of the tree.
            archiveTree.setRootVisible(false);
            //rightPane.setViewportView(archiveTree);
            archiveTree.setOpaque(false);
            archiveTree.setFont(Font.decode("Serif-bold-14"));
            JPanel archiveRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            archiveRow.add(new JLabel(" ")); // a bit of left-indentation.
            archiveRow.add(archiveTree);
            archivePanel.add(archiveRow, BorderLayout.CENTER);
        } else {  // There are no archives; suggest that they make one, and say how to do it.
            // For whatever reason, giving the label a left justification and adding it directly to the Box -
            // resulted in a right-justified appearance.  So - wrapped it in the left-flavored FlowLayout.
            JLabel thePrompt = new JLabel("  You can make a new archive via the main menu.");
            thePrompt.setFont(Font.decode("Serif-bold-20"));
            JPanel promptRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0,0));
            promptRow.add(thePrompt);
            myBox.add(promptRow);
        }

        rightPane.setViewportView(archivePanel);
    } // end showArchives


    // Called from YearView mouse dbl-click on numeric date, or MonthView mouse dbl-click on the 'day' square.
    @Override
    public void showDay() {
        MemoryBank.debug("showDay called.");
        theTree.setSelectionPath(dayNotesPath);
    } // end showDay


    void showCurrentNoteGroup() {
        Object theMessage;
        if(theNoteGroupPanel == null) {
            theMessage = "No NoteGroup is selected!";
        } else {
            JScrollPane jScrollPane = new JScrollPane();
            JTextArea jTextArea = new JTextArea();
            String groupChangedString = "\"groupChanged\" : " + theNoteGroupPanel.myNoteGroup.groupChanged + ",\n";
            jTextArea.append(groupChangedString);
            Object[] theData = theNoteGroupPanel.myNoteGroup.getTheData();
            jTextArea.append(AppUtil.toJsonString(theData));
            jScrollPane.setViewportView(jTextArea);
            jScrollPane.setPreferredSize(new Dimension(600, 500));
            theMessage = jScrollPane;
        }
        optionPane.showMessageDialog(this, theMessage, "Viewing Current NoteGroup", PLAIN_MESSAGE);
    }

    void showEvents() {
        JDialog dialogWindow;

        EventNoteGroupPanel theBigPicture = getConsolidatedView();
        if (theBigPicture == null) return;

        // Make a dialog window.
        // A dialog is preferred to the JOptionPane.showMessageDialog, because it is easier to control
        // the size, and we need no additional buttons.
        dialogWindow = new JDialog((Frame) null, true);
        dialogWindow.getContentPane().add(theBigPicture.theBasePanel, BorderLayout.CENTER);
//        dialogWindow.setTitle("Scheduled Events");
        dialogWindow.setSize(680, 580);
        dialogWindow.setResizable(false);

        // Center the dialog.
        dialogWindow.setLocationRelativeTo(MemoryBank.logFrame);

        // Go modal -
        dialogWindow.setVisible(true);
    } // end showEvents


    // Show the Group where the search result was found.  This is going to be its current state and not a snapshot
    // of the group when the data was found, so the note(s) in this group that met the search criteria may not still
    // be here or may not still meet that criteria.  And it is possible that the
    // group itself has gone away.  If the group cannot be shown then nothing happens.
    @Override
    public void showFoundIn(SearchResultData srd) {
        if(srd.foundIn == null) return;
        NoteGroupPanel thePanel = srd.foundIn.getNoteGroupPanel();
        thePanel.setEditable(false);
        theNoteGroupPanel = thePanel; // For 'showCurrentNoteGroup'

        // Whatever view we are about to switch to - is 'disconnected' from the tree, so clear tree selection.
        theTree.clearSelection();

        // set the 'Go Back' menu item to visible
        if(selectedArchiveNode == null) appMenuBar.manageMenus("Viewing FoundIn");
        else appMenuBar.manageMenus("No Selection"); // Or not, if we came from an archive.

        // View the Panel where the search result was found.
        rightPane.setViewportView(thePanel.theBasePanel);
    }

    private void showHelp() {
        //new Exception("Your help is showing").printStackTrace();
        try {
            String s = MemoryBank.logHome + File.separatorChar;
            Runtime.getRuntime().exec("hh " + s + "MemoryBank.chm");
        } catch (IOException ioe) {
            // hh "badFile" - does NOT throw an exception, but puts up a 'cant find file' window/message.
            MemoryBank.debug(ioe.getMessage());
        } // end try/catch
    } // end showHelp


    void showKeepers() {
        Object theMessage;
        JScrollPane jScrollPane = new JScrollPane();
        JTextArea jTextArea = new JTextArea();

        String aLine;
        ArrayList<String> theNames;
        int keptGoals = theGoalsKeeper.size();
        int keptEvents = theEventListKeeper.size();
        int keptTodoLists = theTodoListKeeper.size();
        int keptSearches = theSearchResultsKeeper.size();

        if(theAppDays != null) {
            aLine = "theAppDays:  " + theAppDays.getGroupName();
        } else {
            aLine = "theAppDays: null";
        }
        jTextArea.append(aLine);

        if(theAppMonths != null) {
            aLine = "\ntheAppMonths:  " + theAppMonths.getGroupName();
        } else {
            aLine = "\ntheAppMonths: null";
        }
        jTextArea.append(aLine);

        if(theAppYears != null) {
            aLine = "\ntheAppYears:  " + theAppYears.getGroupName();
        } else {
            aLine = "\ntheAppYears: null";
        }
        jTextArea.append(aLine);
        jTextArea.append("\n"); // Spacer

        aLine = "\ntheGoalsKeeper: " + keptGoals + "\n";
        jTextArea.append(aLine);
        theNames = theGoalsKeeper.getNames();
        for(Object anObject: theNames) {
            aLine = "   " + anObject + "\n";
            jTextArea.append(aLine);
        }

        aLine = "\ntheEventListKeeper: " + keptEvents + "\n";
        jTextArea.append(aLine);
        theNames = theEventListKeeper.getNames();
        for(Object anObject: theNames) {
            aLine = "   " + anObject + "\n";
            jTextArea.append(aLine);
        }

        aLine = "\ntheTodoListKeeper: " + keptTodoLists + "\n";
        jTextArea.append(aLine);
        theNames = theTodoListKeeper.getNames();
        for(Object anObject: theNames) {
            aLine = "   " + anObject + "\n";
            jTextArea.append(aLine);
        }

        aLine = "\ntheSearchResultsKeeper: " + keptSearches + "\n";
        jTextArea.append(aLine);
        theNames = theSearchResultsKeeper.getNames();
        for(Object anObject: theNames) {
            aLine = "   " + anObject + "\n";
            jTextArea.append(aLine);
        }

        jScrollPane.setViewportView(jTextArea);
        jScrollPane.setPreferredSize(new Dimension(600, 500));
        theMessage = jScrollPane;
        optionPane.showMessageDialog(this, theMessage, "Viewing Current NoteGroup", PLAIN_MESSAGE);
    }

    // Called from YearView - a click on a Month name
    @Override
    public void showMonthView() {
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
        // and get back to a 'normal' view.  There was a previous version here, where we had kept the earlier
        // Panel so we could toggle back to it, but dropped that feature - too cute, and not that helpful.
        theTree.clearSelection();

    } // end showToday

    @Override
    public void showWeek(LocalDate theWeekToShow) {
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


    // This method will either show or hide a small modal
    //   dialog with an animated gif and a 'Working...' message.
    //   Call this method with 'true' before you start
    //   a potentially long task that the user must wait for,
    //   then call it with 'false' to go on.  It is static
    //   in order to give access to external classes such
    //   as group headers, that need to wait for sorting.
    static void showWorkingDialog(boolean showIt) {
        if (showIt) {
            theWorkingDialog.setLocationRelativeTo(theInstance); // In case the app has been moved around.
            //new Exception("Test tracing").printStackTrace(); // Helpful in finding which tests left this up.

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
                        Thread.sleep(200);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    theWorkingDialog.setVisible(false);
                }
            }).start();
        } // end if show - else hide
    } // end showWorkingDialog


    // The static menu item applies to all three CalendarNoteGroup types.
    private void toggleReview() {
        boolean reviewMode = false;
        if(AppMenuBar.reviewMode.isSelected()) reviewMode = true;
        if(theAppDays != null) theAppDays.reviewMode = reviewMode;
        if(theAppMonths != null) theAppMonths.reviewMode = reviewMode;
        if(theAppYears != null) theAppYears.reviewMode = reviewMode;
    }


    void treeSelectionChanged(TreePath newPath) {
        if (newPath == null) return;
        // You know how some animals will still move or twitch a bit after death?
        // The explanation is that their central nervous system is still sending
        // (random) signals as it shuts down, and there is enough of their body
        // still in working order well enough to react somewhat to those signals.
        // This app is similar in that some of the tests for it will run and then
        // end so quickly that some of the threads here have not finished their
        // processing.  The above line helps with that; otherwise we see null
        // exceptions on the 'node = ' line directly below, AFTER the test has passed.

        // Obtain a reference to the new selection.
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) (newPath.getLastPathComponent());
        // This is better than 'theTree.getLastSelectedPathComponent()' because it works for
        //   normal tree selection events but also allows for 'phantom' selections; tree
        //   paths that were created and selected by code vs those that came from a user's
        //   mouse click event on an existing (visible and active) tree node.
        if (selectedNode == null) {
            appMenuBar.manageMenus("No Selection");
            return;
        }
        selectedArchiveNode = null; // Clear any previous archive selection.

        // We have started to handle the change; now disallow
        //   further input by showing the modal 'working' dialog, until we are finished.
        theWorkingDialog.setLocationRelativeTo(rightPane); // Re-center before showing.
        if (!restoringPreviousSelection) showWorkingDialog(true);

        // Update the current selection row
        // Single-selection mode; Max == Min; take either one.
        appOpts.theSelectionRow = theTree.getMaxSelectionRow();

        // If there was a NoteGroup open prior to this tree selection change then update its data now.
        // But we don't just automatically save it; it has to have undergone one or more changes.
        if (theNoteGroupPanel != null && theNoteGroupPanel.myNoteGroup.groupChanged) {
            theNoteGroupPanel.unloadNotesPanel(theNoteGroupPanel.theNotePager.getCurrentPage());
        } // end if

        // Get the string for the selected node.
        String theNodeString = selectedNode.toString();
        MemoryBank.debug("New tree selection: " + theNodeString);
        appOpts.theSelection = theNodeString; // Not used, but helpful during a visual review of the persisted options.
        String selectionContext = theNodeString;  // used in menu management; this default value may change, below.

        // Get the name of the node's parent.  Thanks to the way we have created the tree and
        // the unselectability of the tree root, we never expect the parent path to be null.
        String parentNodeName = newPath.getParentPath().getLastPathComponent().toString();

        //-----------------------------------------------------
        // These booleans will help us to avoid incorrect assumptions based on the text of the
        //   new selection, in cases where some bozo named their group the same as a parent
        //   branch.  For example, a To Do list named 'To Do Lists'.  We first look to see if
        //   the selection is a branch before we would start handling it as a leaf, and by
        //   then we will know which branch the leaf belongs on.
        //-----------------------------------------------------
        boolean isTopLevel = parentNodeName.equals("App");
        boolean isArchives = isTopLevel && theNodeString.equals(DataArea.ARCHIVES.toString());
        boolean isGoalsBranch = isTopLevel && theNodeString.equals(DataArea.GOALS.toString());
        boolean isEventsBranch = isTopLevel && theNodeString.equals(DataArea.UPCOMING_EVENTS.toString());
        boolean isTodoBranch = isTopLevel && theNodeString.equals(DataArea.TODO_LISTS.toString());
        boolean isSearchBranch = isTopLevel && theNodeString.equals(DataArea.SEARCH_RESULTS.toString());

        theNoteGroupPanel = null; // initialize

        //<editor-fold desc="Actions Depending on the selection">
        if ( isArchives ) {
            showArchives();
        } else if (isGoalsBranch) {  // Edit the Goals parent branch
            selectionContext = "Goals Branch Editor";
            BranchHelper tbh = new BranchHelper(theTree, theGoalsKeeper, DataArea.GOALS);
            int resultCount = tbh.getChoices().size(); // Choices array list can be empty but not null.
            if(resultCount > 0) {  // No branch editor until after there is at least one Goal.
                TreeBranchEditor tbe = new TreeBranchEditor("Goals", selectedNode, tbh);
                rightPane.setViewportView(tbe);
            } else { // In this case we switch from handling tree selection events to (pseudo) menu bar item handling.
                showWorkingDialog(false);
                theTree.clearSelection(); // This one cannot be restored upon app restart.
                appMenuBar.manageMenus("Goals Branch Editor");
                // (menus will get managed again at the end of this long 'if/else' structure, but
                //   in this case it needed to happen right now, before our next line).
                addNewGroup();
            }
        } else if (isEventsBranch) {  // Edit the Upcoming Events parent branch
            selectionContext = "Upcoming Events Branch Editor";
            BranchHelper tbh = new BranchHelper(theTree, theEventListKeeper, DataArea.UPCOMING_EVENTS);
            int resultCount = tbh.getChoices().size(); // Choices array list can be empty but not null.
            if(resultCount > 0) {  // No branch editor until after there is at least one Goal.
                TreeBranchEditor tbe = new TreeBranchEditor("Upcoming Events", selectedNode, tbh);
                rightPane.setViewportView(tbe);
            } else { // In this case we switch from handling tree selection events to (pseudo) menu bar item handling.
                showWorkingDialog(false);
                theTree.clearSelection(); // This one cannot be restored upon app restart.
                appMenuBar.manageMenus(selectionContext);
                // (menus will get managed again at the end of this long 'if/else' structure, but
                //   in this case it needed to happen right now, before our next line).
                addNewGroup();
            }
        } else if (isTodoBranch) {  // Edit the Todo parent branch
            // To Do List management - select, deselect, rename, reorder, remove
            // The 'tree' may change often.  We instantiate a new helper
            // and editor each time, to be sure all are in sync.
            selectionContext = "To Do Lists Branch Editor";
            BranchHelper tbh = new BranchHelper(theTree, theTodoListKeeper, DataArea.TODO_LISTS);
            int resultCount = tbh.getChoices().size(); // Choices array list can be empty but not null.
            if(resultCount > 0) {  // No branch editor until after there is at least one Goal.
                TreeBranchEditor tbe = new TreeBranchEditor("To Do Lists", selectedNode, tbh);
                rightPane.setViewportView(tbe);
            } else { // In this case we switch from handling tree selection events to (pseudo) menu bar item handling.
                showWorkingDialog(false);
                theTree.clearSelection(); // This one cannot be restored upon app restart.
                appMenuBar.manageMenus(selectionContext);
                // (menus will get managed again at the end of this long 'if/else' structure, but
                //   in this case it needed to happen right now, before our next line).
                addNewGroup();
            }
        } else if (isSearchBranch) {  // Edit the Search parent branch
            selectionContext = "Search Results Branch Editor";
            BranchHelper sbh = new BranchHelper(theTree, theSearchResultsKeeper, DataArea.SEARCH_RESULTS);
            int resultCount = sbh.getChoices().size(); // Choices array list can be empty but not null.
            if(resultCount > 0) {  // No branch editor until after there is at least one Goal.
                TreeBranchEditor tbe = new TreeBranchEditor("Search Results", selectedNode, sbh);
                rightPane.setViewportView(tbe);
            } else { // In this case we switch from handling tree selection events to (pseudo) menu bar item handling.
                showWorkingDialog(false);
                selectionContext = "No Selection";
                theTree.clearSelection(); // This one cannot be restored upon app restart.
                if(!restoringPreviousSelection) {
                    // This can happen on an app restart where the previous session was left on an empty SearchResults
                    //   node because the new search was cancelled.  Definitely a corner case.
                    prepareSearch();
                }
            }
        } else if (!selectedNode.isLeaf()) {  // Looking at other expandable nodes
            JTree jt = new JTree(selectedNode); // Show as a tree but no editing.
            jt.setShowsRootHandles(true);
            rightPane.setViewportView(jt);
        } else if (parentNodeName.equals("Goals")) { // Selection of a Goal
            selectionContext = "Goal";  // For manageMenus
            GoalGroupPanel goalGroup;

            // If this group has been previously loaded during this session,
            // we can retrieve it from the keeper.
            goalGroup = (GoalGroupPanel) theGoalsKeeper.get(theNodeString);

            // Otherwise load it if it exists or make a new one if it does not exist.
            if (goalGroup == null) {
                goalGroup = (GoalGroupPanel) GroupPanelFactory.loadNoteGroupPanel(parentNodeName, theNodeString);

                if (goalGroup != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    theGoalsKeeper.add(goalGroup);
                }
            } else {
                if (!goalGroup.getEditable()) goalGroup.setEditable(true);
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
                                    "This Goal selection will be removed.",
                            "Data not accessible", JOptionPane.WARNING_MESSAGE);
                } // end if

                closeGroup(); // Group is already gone; this just removes the tree node.
                selectionContext = "No Selection";  // For manageMenus
            } else {
                // There is only one menu, for ALL Goals.  It needs to be reset with every list change.
                goalGroup.setListMenu(appMenuBar.getNodeMenu(selectionContext));

                theNoteGroupPanel = goalGroup;
                rightPane.setViewportView(goalGroup.theBasePanel);
            } // end if

        } else if (parentNodeName.equals("Upcoming Events")) { // Selection of an Event group
            selectionContext = "Upcoming Event";  // For manageMenus
            EventNoteGroupPanel eventNoteGroup;

            // If this group has been previously loaded during this session,
            // we can retrieve it from the keeper.
            eventNoteGroup = (EventNoteGroupPanel) theEventListKeeper.get(theNodeString);

            // Otherwise load it, but only if a file for it already exists.
            if (eventNoteGroup == null) {
                eventNoteGroup = (EventNoteGroupPanel) GroupPanelFactory.loadNoteGroupPanel(parentNodeName, theNodeString);
                if (eventNoteGroup != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    theEventListKeeper.add(eventNoteGroup);
                }
            } else {
                if (!eventNoteGroup.getEditable()) eventNoteGroup.setEditable(true);
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
                            "Group not accessible", JOptionPane.WARNING_MESSAGE);
                } // end if

                closeGroup(); // Group is already gone; this just removes the tree node.
                selectionContext = "No Selection";  // For manageMenus
            } else {
                // There is only one menu, for ALL event lists.  It needs to be reset with every list change.
                eventNoteGroup.setListMenu(appMenuBar.getNodeMenu(selectionContext));

                theNoteGroupPanel = eventNoteGroup;
                rightPane.setViewportView(theNoteGroupPanel.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("To Do Lists")) {
            // Selection of a To Do List
            selectionContext = "To Do List";  // For manageMenus
            TodoNoteGroupPanel todoNoteGroup;

            // If the list has been previously loaded during this session,
            // we can retrieve the group from the keeper.
            todoNoteGroup = (TodoNoteGroupPanel) theTodoListKeeper.get(theNodeString);

            // Otherwise load it, but only if a file for it already exists.
            if (todoNoteGroup == null) {
                todoNoteGroup = (TodoNoteGroupPanel) GroupPanelFactory.loadNoteGroupPanel(parentNodeName, theNodeString);
                if (todoNoteGroup != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    theTodoListKeeper.add(todoNoteGroup);
                }
            } else {
                if (!todoNoteGroup.getEditable()) todoNoteGroup.setEditable(true);
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

                closeGroup(); // Group is already gone; this just removes the tree node.
                selectionContext = "No Selection";  // For manageMenus
            } else {
                // There is only one menu, for ALL todo lists.  It needs to be reset with every list change.
                todoNoteGroup.setListMenu(appMenuBar.getNodeMenu(selectionContext));

                theNoteGroupPanel = todoNoteGroup;
                rightPane.setViewportView(theNoteGroupPanel.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("Search Results")) {
            // Selection of a Search Result List
            selectionContext = "Search Result";  // For manageMenus
            SearchResultGroupPanel searchResultGroupPanel;
            theWayBack = theTree.getSelectionPath();

            // If the search has been previously loaded during this session,
            // we can retrieve the group for it from the keeper.
            searchResultGroupPanel = (SearchResultGroupPanel) theSearchResultsKeeper.get(theNodeString);

            // Otherwise construct it, but only if a file for it already exists.
            if (searchResultGroupPanel == null) {
                searchResultGroupPanel = (SearchResultGroupPanel) GroupPanelFactory.loadNoteGroupPanel(parentNodeName, theNodeString);
                if (searchResultGroupPanel != null) {
                    log.debug("Loaded " + theNodeString + " from the data repository");
                    theSearchResultsKeeper.add(searchResultGroupPanel);
                }
            } else {
                log.debug("Retrieved '" + theNodeString + "' from the keeper");
            }

            if (searchResultGroupPanel == null) {
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

                closeGroup(); // Group is already gone; this just removes the tree node.
                selectionContext = "No Selection";  // For manageMenus
            } else {
                theNoteGroupPanel = searchResultGroupPanel;
                searchResultGroupPanel.treePanel = this;
                rightPane.setViewportView(theNoteGroupPanel.theBasePanel);
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
            // Capture new icons, if any.
            if(theAppDays != null) theAppDays.preClosePanel();

            if (theMonthView == null) {
                // Construct with a date where the Month (with 31 days) starts on a Sunday, part of the cure for:
                //   SCR00035 - MonthView does not show all icons for a day.  (see also the note in MonthView)
                theMonthView = new MonthView(LocalDate.of(2020, 3, 15));
                // This results in a one-time-only double 'set' of the day but is needed so that the various
                // DayCanvases are ready to properly show icons.  (This appears to also prevent the problem
                // from appearing in the final (initially empty) DayCanvases of the MonthCanvas grid (not sure why).
                theMonthView.setChoice(selectedDate); // This sets the 'choice' label and day highlight, if appropriate.
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
                theAppDays = new DayNoteGroupPanel();
                theAppDays.setListMenu(appMenuBar.getNodeMenu(selectionContext));
            } else { // Previously constructed.  Ensure that it is editable.
                log.debug("Using the previously constructed 'theAppDays' for " + theNodeString);
                if (!theAppDays.getEditable()) theAppDays.setEditable(true);
            }

            theAppDays.setAlteredDateListener(this); // needed for both new and pre-existing.
            theNoteGroupPanel = theAppDays;
            theAppDays.setDate(selectedDate);

            setViewedDate(selectedDate, ChronoUnit.DAYS);
            rightPane.setViewportView(theAppDays.theBasePanel);
        } else if (theNodeString.equals("Month Notes")) {
            if (viewedDateGranularity == ChronoUnit.YEARS) {
                viewedDate = selectedDate;
            }
            viewedDateGranularity = ChronoUnit.MONTHS;

            if (theAppMonths == null) {
                theAppMonths = new MonthNoteGroupPanel(); // Takes current date as default initial 'choice'.
                theAppMonths.setListMenu(appMenuBar.getNodeMenu(selectionContext));
            } else { // Previously constructed.  Ensure that it is editable.
                log.debug("Using the previously constructed 'theAppMonths' for " + theNodeString);
                if (!theAppMonths.getEditable()) theAppMonths.setEditable(true);
            }

            theAppMonths.setAlteredDateListener(this); // needed for both new and pre-existing.
            theNoteGroupPanel = theAppMonths;
            theAppMonths.setDate(viewedDate);
            rightPane.setViewportView(theAppMonths.theBasePanel);
        } else if (theNodeString.equals("Year Notes")) {
            if (theAppYears == null) {
                theAppYears = new YearNoteGroupPanel();
                theAppYears.setListMenu(appMenuBar.getNodeMenu(selectionContext));
            } else { // Previously constructed.  Ensure that it is editable.
                log.debug("Using the previously constructed 'theAppYears' for " + theNodeString);
                if (!theAppYears.getEditable()) theAppYears.setEditable(true);
            }

            theAppYears.setAlteredDateListener(this); // needed for both new and pre-existing.
            theNoteGroupPanel = theAppYears;
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
    // Method Name:  updateAppOptions
    //
    // Capture the current tree configuration in terms of node expansion/contraction
    //   and variable group contents, and put it into appOpts (AppOptions class).
    //-------------------------------------------------
    void updateAppOptions(boolean updateLists) {
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

        numResults = theSearchNode == null ? 0 : theSearchNode.getChildCount();
        if (numResults > 0) {
            leafLink = theSearchNode.getFirstLeaf();
            while (numResults-- > 0) {
                String s = leafLink.toString();
                //MemoryBank.debug("  Preserving search result: " + s);
                appOpts.searchResultList.addElement(s);
                leafLink = leafLink.getNextLeaf();
            } // end while
        } // end if

    } // end updateAppOptions

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
        appMenuBar.requestFocus(); // Do not start out with a note already selected.

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

