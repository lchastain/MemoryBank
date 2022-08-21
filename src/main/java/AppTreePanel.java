//  The primary control for the Memory Bank application; provides a menubar at
//  the top, a 'tree' control on the left, and a viewing pane on the right
//
// Quick-reference notes:
// 
// MenuBar events        - actionPerformed() --> handleMenuBar().
// Tree Selection events - valueChanged() --> treeSelectionChanged() in a new thread.

import com.fasterxml.jackson.core.type.TypeReference;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;


@SuppressWarnings("rawtypes")
public class AppTreePanel extends JPanel implements TreePanel, TreeSelectionListener, AlteredDateListener {
    static final long serialVersionUID = 1L; // JPanel wants this but we will not serialize.
    static AppTreePanel theInstance;  // A tricky way for a static context to call an instance method of this class.
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
    JDialog theEventsDialog;
    private NoteGroupPanel theNoteGroupPanel; // A reference to the current selection
    private NoteGroupPanel deletedNoteGroupPanel;
    DayNoteGroupPanel theAppDays;
    MonthNoteGroupPanel theAppMonths;
    YearNoteGroupPanel theAppYears;
    TabbedCalendarNoteGroupPanel theTabbedCalendarNoteGroupPanel;
    MonthView theMonthView;
    YearView theYearView;
    NoteGroupPanelKeeper theGoalsKeeper;          // keeper of all loaded Goals.
    NoteGroupPanelKeeper theEventListKeeper;      // keeper of all loaded Event lists.
    NoteGroupPanelKeeper theTodoListKeeper;       // keeper of all loaded To Do lists.
    NoteGroupPanelKeeper theSearchResultsKeeper;  // keeper of all loaded SearchResults.
    SearchPanel searchPanel;
    private final JPanel aboutPanel;
    private final JSplitPane splitPane;
    private TreePath theWayBack;
    private LocalDate viewedDate;    // A date to be shown by DateRelatedDisplayTypes

    private DefaultMutableTreeNode theRootNode;
    private DefaultMutableTreeNode selectedArchiveNode;
    private int theCalendarIndex;

    // Predefined Tree Paths to 'leaf' nodes.
    TreePath calendarNotesPath;
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

    public AppTreePanel(@NotNull JFrame aFrame, AppOptions appOpts) {
        super(new GridLayout(1, 0));
        appMenuBar = new AppMenuBar();
        aFrame.setJMenuBar(appMenuBar);

        // Give full package-level access to this instance, by providing a static handle to it.
        // This works because we will always only have one instance of the AppTreePanel class in this app.
        theInstance = this;

        this.appOpts = appOpts;
        archiveWindows = new HashMap<>();

        //<editor-fold desc="Make the 'Working...' dialog">
        theWorkingDialog = new JDialog(aFrame, "Working", true);
        JLabel lbl = new JLabel("Please Wait...");
        lbl.setFont(Font.decode("Dialog-bold-16"));
        IconInfo iconInfo = new IconInfo();
        iconInfo.iconName = "Animated/const_anim";
        iconInfo.iconFormat = "gif";
        iconInfo.dataArea = DataArea.APP_ICONS;

        lbl.setIcon(iconInfo.getImageIcon());
        lbl.setVerticalTextPosition(JLabel.TOP);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        theWorkingDialog.add(lbl);
        theWorkingDialog.pack();
        theWorkingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        //</editor-fold>

        optionPane = new Notifier() { }; // Uses all default methods.

        //---------------------------------------------------
        // Create the menubar handler, but fire it from a
        //   thread so we can quickly return and then if
        //   any of those items need to show the 'Working...'
        //   dialog, they will be able to.
        //---------------------------------------------------
        ActionListener al = ae -> {
            final String what = ae.getActionCommand();
            new Thread(() -> handleMenuBar(what)).start(); // Start the thread
        };
        appMenuBar.addHandler(al); // Add the above handler to all menu items.
        //---------------------------------------------------------

        setOpaque(true);

        MemoryBank.update("Recreating the previous Tree configuration");
        theCalendarIndex = -1;
        createTree();  // Create the tree.

        // Listen for when the selection changes.
        // We need to do this now so that the proper initialization
        //   occurs when we restore the previous selection, below.
        theTree.addTreeSelectionListener(this);

        // Create the scroll pane and add the tree to it.
        JScrollPane treeView = new JScrollPane(theTree);

        // Create the viewing pane and start with the 'about' graphic.
        AppImage abbowt = new AppImage("images/about.gif");
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

        // Initialize a viewing Date
        viewedDate = LocalDate.now(); // This should never be null.

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
    // The new Panel is not actually instantiated here; that happens when it is selected.
    @SuppressWarnings({"rawtypes", "fallthrough"})  // rawtypes the xlint warning about Enumeration, (much farther) below.
    private void addNewGroup() {
        String newName = null;

        // Initialize the following local variables, otherwise IJ complains.
        String prompt;
        String title;
        TreePath groupParentPath;
        NoteGroupPanelKeeper theNoteGroupPanelKeeper;

        String theContext = appMenuBar.getCurrentContext();
        GroupType groupType;
        MemoryBank.debug("Adding new group in this context: " + theContext);
        switch (theContext) {
            case "Goal":
            case "Goals Branch Editor":
                groupType = GroupType.GOALS;
                prompt = "Enter a short name for the Goal\n";
                prompt += "Ex: Graduate, Learn a Language, etc";
                title = "Add a new Goal";
                groupParentPath = goalsPath;
                theNoteGroupPanelKeeper = theGoalsKeeper;
                break;
            case "Upcoming Event":
            case "Upcoming Events Branch Editor":
                groupType = GroupType.EVENTS;
                prompt = "Enter a name for the new Event category\n";
                prompt += "Ex: meetings, appointments, birthdays, etc";
                title = "Add a new Events category";
                groupParentPath = eventsPath;
                theNoteGroupPanelKeeper = theEventListKeeper;
                break;
            case "To Do List":
            case "To Do Lists Branch Editor":
                groupType = GroupType.TODO_LIST;
                prompt = "Enter a name for the new To Do List";
                title = "Add a new To Do List";
                groupParentPath = todolistsPath;
                theNoteGroupPanelKeeper = theTodoListKeeper;
                break;
            case "Search Results Branch Editor":
                // This can happen after the last SearchResults was deleted, leaving only the
                //   leaf that would otherwise be a branch.
                prepareSearch();
                // no break here, deliberately.
            default:
                return;
        }

        if (restoringPreviousSelection) {
            // In this case we are coming from a restart where a non-existent group was previously selected but
            // neither it nor any others are in this Category, now, so we landed on the Branch but at app startup.
            // We didn't want to come here - so leave, quietly.
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
                break;
            }
        }
        if (theNewGroupNode == null) {  // Not already a node on the tree, so make one now.
            theNewGroupNode = new DefaultMutableTreeNode(newName, false);

            // We will make a new NoteGroupPanel for this new group, eventually, but we need to get
            //   the new group right now in order to check to see if the proposed name is acceptable.
            // So - we make a group without a panel, so that we can get its NoteGroupDataAccessor.
            GroupInfo groupInfo = new GroupInfo(newName, groupType);
            NoteGroup theNewGroup = groupInfo.getNoteGroup();

            // Ensure that there are no known problems with the new name.
            String theComplaint = theNewGroup.groupDataAccessor.getObjectionToName(newName);
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
        if (theGroup == null) { // Not already loaded; construct one, whether there is pre-existing data for it or not.
            MemoryBank.debug("Getting a new group from the factory.");
            theGroup = GroupPanelFactory.loadOrMakePanel(theContext, newName);
            assert theGroup != null; // It won't be, but IJ needs to be sure.
            if (theGroup.myNoteGroup.getGroupProperties() == null) {
                theGroup.myNoteGroup.makeGroupProperties();
            }
            theGroup.setGroupChanged(true); // Needed for new Goals, if none others.
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
        // Remove the tree selection listener while we rebuild this portion of the tree.
        theTree.removeTreeSelectionListener(this);

        // Make a new tree node for the result data whose name is provided in the input parameter
        DefaultMutableTreeNode tmpNode;
        tmpNode = new DefaultMutableTreeNode(searchResultName, false);

        // Get the Search Results branch
        DefaultMutableTreeNode nodeSearchResults = BranchHelperInterface.getNodeByName(theRootNode, DataArea.SEARCH_RESULTS.toString());

        // Search Results branch may not be there, if this is the first search result.  Add it, if needed.
        if (nodeSearchResults == null) {  // No branch editor until after there is at least one search result.
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
        if (theParent == null) node.removeFromParent(); // theParent can be null during testing...
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

    void closeArchive(String archiveName) {
        // Close any windows that are currently open for this Archive -
        Set<String> setCodes = archiveWindows.keySet();
        for (String code : setCodes) {
            if (code.startsWith(archiveName)) {
                JFrame theArchiveWindow = archiveWindows.get(code);
                theArchiveWindow.setVisible(false);
            }
        }
    }

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

        leaf = new DefaultMutableTreeNode("Week View");
        branch.add(leaf);
        pathToRoot = leaf.getPath();
        weekViewPath = new TreePath(pathToRoot);

        leaf = new DefaultMutableTreeNode("Month View");
        branch.add(leaf);
        pathToRoot = leaf.getPath();
        monthViewPath = new TreePath(pathToRoot);

        leaf = new DefaultMutableTreeNode("Year View");
        branch.add(leaf);
        pathToRoot = leaf.getPath();
        yearViewPath = new TreePath(pathToRoot);
        //---------------------------------------------------

        // Calendar Notes - Group types are Day, Month, Year
        theCalendarIndex = 4;  // Not magic; just going by where we know this to belong on the trunk.
        groupCalendarNotes(true); // group or not, but add the right leaves to the tree.

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
        ArrayList groupNames = MemoryBank.dataAccessor.getGroupNames(GroupType.SEARCH_RESULTS, false);
        int resultCount = groupNames.size(); // groupNames array list can be empty but not null.
        if (resultCount > 0) {  // No branch editor until after there is at least one search result.
            branch = new DefaultMutableTreeNode("Search Results", true);
            trunk.add(branch);
            pathToRoot = branch.getPath();
            searchresultsPath = new TreePath(pathToRoot);
        }

        // Restore previous search results, if any.
        if (!appOpts.searchResultList.isEmpty()) {
            for (int i = 0; i < appOpts.searchResultList.size(); i++) {
                String searchResultName = appOpts.searchResultList.get(i);
                branch.add(new DefaultMutableTreeNode(searchResultName, false));
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

    @Override // AlteredDateListener method
    // Needed to keep date-related panels synchronized with respect to viewedDate when the date of any one of them
    //   changes.  The change will only come from user interaction with the provided controls on the currently
    //   displayed panel, so the last such interaction 'wins' and the resulting date is therefore the one to be used by
    //   all other panels.  The change will be applied immediately to the viewedDate here and also to any other panels
    //   that have already been constructed, regardless of whether or not the change is significant enough to warrant
    //   a change of their view.  There are two separate justifications for applying the same date to all other panels:
    // Reason 1:  To keep the numeric date unchanged when the change is by Month or by Year.
    //   Consider when the day is the 27th of the month and the MonthNotes has already been constructed.  In the
    //   DayNotes panel, the date is reduced from the 27th to the 26th.  The change would normally be too small to
    //   be seen on the MonthView.  Then on the tree, select the MonthView and then increment by one from September to
    //   October.  Now go back to DayNotes, and while you expect that the month has changed to October, you also
    //   expect that the date is the 26th.  The only way that the MonthView could have added one month and yet land
    //   on the correct day is if that 1-day change you made earlier had been accepted as its new 'base' date.
    // Reason 2:  To keep numeric dates 'legal'.
    //   Consider that you are looking at DayNotes on Aug 31.  Then the month is incremented (either from here or from
    //   one of the 'month' panels.  Then the day you land on is Sep 30.  This 'correction' happens thanks to the
    //   LocalDate logic handling of how to add or subtract a 'month'.  Using that mechanism we can always get to a
    //   'legal' date, and don't have to do any day-math or date reconstructing from calculated integers.  The panel
    //   that makes the change is always able to do its own math and arrive at the correct day, regardless of the
    //   size of the change.  This is why that is the date that needs to be applied to all the other panels.
    public void dateChanged(DateRelatedDisplayType whoChangedIt, LocalDate theNewDate) {
        viewedDate = theNewDate;
        // To keep all the Date-related Panels 'in sync', we need to set new dates on
        //   the other Panels besides the one that already had the change.
        // But don't worry; if there was not a significant-enough date change, the setDate method
        //   will not do a Notes panel reload, so that menu items remain enabled and panels
        //   retain their current appearance.
        if(theAppDays != null && whoChangedIt != DateRelatedDisplayType.DAY_NOTES) {
            theAppDays.setDate(viewedDate);
        }
        if(theAppMonths != null && whoChangedIt != DateRelatedDisplayType.MONTH_NOTES) {
            theAppMonths.setDate(viewedDate);
        }
        if(theAppYears != null && whoChangedIt != DateRelatedDisplayType.YEAR_NOTES) {
            theAppYears.setDate(viewedDate);
        }
        if(theYearView != null && whoChangedIt != DateRelatedDisplayType.YEAR_VIEW) {
            theYearView.setView(viewedDate);
        }
        if(theMonthView != null && whoChangedIt != DateRelatedDisplayType.MONTH_VIEW) {
            theMonthView.setView(viewedDate);
        }
    } // end dateChanged


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

    // Delete the data for the currently active group.  (called from a menu bar action, or a test)
    // Here we keep a reference to the deleted group.  This will allow for an 'undo', if desired.
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

        int choice = optionPane.showConfirmDialog(
                this,
                theMessage,
                "Create a new Archive ?", // pane title bar
                JOptionPane.OK_CANCEL_OPTION, // Option type
                PLAIN_MESSAGE);    // Message type

        if (choice == JOptionPane.OK_OPTION) {
            System.out.println("\nCreating an archive: \n");
            preClose(); // Preserve all changes across all open Panels.
            MemoryBank.dataAccessor.saveAppOptions();

            // Preserve the current tree selection
            int currentSelection = theTree.getMaxSelectionRow();

            // Create the archive
            if (MemoryBank.dataAccessor.createArchive()) {
                // The deselect/reselect is only needed if we are already showing the archive listing.
                if (currentSelection == 0) {
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
        if (selectedArchiveNode == null) return;

        // Get the archive's name from the selected tree node, then close it.
        String archiveName = selectedArchiveNode.toString();
        MemoryBank.debug("Selected Archive: " + archiveName);
        closeArchive(archiveName);

        // Convert the archive name into an archive date
        LocalDateTime localDateTime = MemoryBank.dataAccessor.getDateTimeForArchiveName(archiveName);

        // Remove the indicated archive
        if (MemoryBank.dataAccessor.removeArchive(localDateTime)) {
            int currentSelection = theTree.getMaxSelectionRow();
            // The reselect is only needed if we are showing the archive listing.
            if (currentSelection == 0) {
                theTree.clearSelection();
                theTree.setSelectionRow(currentSelection);
            }
        } else {
            optionPane.showMessageDialog(theTree, "Archive removal failed!");
        }
    }


    void doSearch(SearchPanel searchPanel) {
        // We will display the results of the search, even if it finds nothing.
        SearchPanelSettings searchPanelSettings = searchPanel.getSettings();
        MemoryBank.debug("Running a Search with these settings: " + AppUtil.toJsonString(searchPanelSettings));
        Vector<NoteData> foundDataVector = MemoryBank.dataAccessor.scanData(searchPanel);

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

        // Validate that this is a 'good' archive, before attempting to make a tree for it.
        boolean goodArchive = ArchiveTreePanel.validateArchive(archiveName);

        if (goodArchive)
            new ArchiveTreePanel(archiveName); // It shows itself in a new window; a reference to it is not needed.
        else optionPane.showMessageDialog(theTree, "Archive not available!");
    } // end doViewArchive


    // Make a Consolidated View group from all the currently selected Event Groups.
    @SuppressWarnings({"rawtypes", "unchecked"})
    private EventNoteGroupPanel getConsolidatedView() {
        // First, get all the nodes that are currently under Upcoming Events.
        DefaultMutableTreeNode eventsNode = BranchHelperInterface.getNodeByName(theRootNode, "Upcoming Events");
        Enumeration e = eventsNode.breadthFirstEnumeration();
        String theNodeName;
        EventNoteGroupPanel theBigGroup = null;
        Vector groupDataVector; // This is the reason for the 'rawtypes' warning suppress.
        LinkedHashSet theUniqueSet = null;
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
            GroupInfo theGroupInfo = new GroupInfo(theNodeName, GroupType.EVENTS);
            Object[] theData = MemoryBank.dataAccessor.getNoteGroupDataAccessor(theGroupInfo).loadNoteGroupData();
            System.out.println("Data Length: " + theData.length);

            BaseData.loading = true; // We don't want to affect the lastModDates!
            Object vectorObject = theData[theData.length - 1]; // The Data has two Objects; properties and a Vector.
            groupDataVector = AppUtil.mapper.convertValue(vectorObject, new TypeReference<Vector<EventNoteData>>() {
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


    AppMenuBar getAppMenuBar() {
        return appMenuBar;
    }


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
                noteGroupPanel = switch (theName) {
                    case "Day Notes" -> theAppDays;
                    case "Month Notes" -> theAppMonths;
                    case "Year Notes" -> theAppYears;
                    default -> noteGroupPanel;
                };
                break;
            case DAY_NOTES:
                if (theAppDays != null) {
                    String theTitle = theAppDays.getTitle();
                    // if theAppDays is set to the named group then we use it.
                    if (theTitle.equals(theName)) noteGroupPanel = theAppDays;
                }
                break;
            case MONTH_NOTES:
                if (theAppMonths != null) {
                    String theTitle = theAppMonths.getTitle();
                    // if theAppMonths is set to the named group then we use it.
                    if (theTitle.equals(theName)) noteGroupPanel = theAppMonths;
                }
                break;
            case YEAR_NOTES:
                if (theAppYears != null) {
                    String theTitle = theAppYears.getTitle();
                    // if theAppYears is set to the named group then we use it.
                    if (theTitle.equals(theName)) noteGroupPanel = theAppYears;
                }
                break;
        }
        return noteGroupPanel;
    }

    // Used by Test
    NoteGroupPanel getTheNoteGroupPanel() {
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

    @Override
    public LocalDate getViewedDate() {
        return viewedDate;
    }

    private void groupCalendarNotes(boolean initial) {
        boolean doit;
        // The decision on whether to group or ungroup could have been made as a simple toggle of a setting, but
        //   that does not cover the case of the initial setting, and it would also allow for the possibility
        //   that the tracking variable might get out of sync with the menu item, whereas this way does not.

        if(initial) {
            doit = MemoryBank.appOpts.groupCalendarNotes;
        } else {
            doit = AppMenuBar.groupCalendarNotes.getState();
            theRootNode.remove(theCalendarIndex); // Either a leaf, or a branch with three leaves.
        }
        System.out.println("Group: yes/no: " + doit);

        DefaultMutableTreeNode leaf, branch;
        TreeNode[] pathToRoot;  // An array of node names leading back to the root (after the node has been added)
        if (doit) { // Group them, into a single leaf off of the tree trunk.
            leaf = new DefaultMutableTreeNode("Calendar Notes");
            theRootNode.insert(leaf, theCalendarIndex);
            pathToRoot = leaf.getPath();
            calendarNotesPath = new TreePath(pathToRoot);
        } else { // Ungroup them; make a branch, with three leaves
            branch = new DefaultMutableTreeNode("Calendar Notes");
            theRootNode.insert(branch, theCalendarIndex);
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
        }

        if(!initial) {
            DefaultTreeModel theTreeModel = (DefaultTreeModel) theTree.getModel();
            theTreeModel.nodeStructureChanged(theRootNode);
            resetTreeState();
            theTree.expandPath(notesPath); // Whether it already is, or not.
            theTree.setSelectionPath(notesPath);
            updateAppOptions(false);
        }
    }

    private void handleMenuBar(@NotNull String what) {
        if (what.equals("Exit")) System.exit(0);
        else if (what.equals("About")) showAbout();
        else if (what.equals("Archive...")) doCreateArchive();
        else if (what.equals("Add New...")) addNewGroup();
        else if (what.equals("Close")) closeGroup();
        else if (what.startsWith("Clear ")) theNoteGroupPanel.clearAllNotes();
        else if (what.equals("Contents")) showHelp();
        else if (what.equals("Go Back")) doGoBack();
        else if (what.equals("Review Mode")) toggleReview();
        else if (what.equals("Group Calendar Notes")) groupCalendarNotes(false);
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
        } else if (what.equals("View")) doViewArchive(selectedArchiveNode.toString());
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
        preClose(); // Preserve all changes across all open Panels.
        String theContext = appMenuBar.getCurrentContext();
        if ("To Do List".equals(theContext)) {
            ((TodoNoteGroupPanel) theNoteGroupPanel).merge();
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


    // Present the user with a dialog whereby they may specify the parameters of
    // their search, then send those parameters to the 'doSearch' method.
    private void prepareSearch() {
        JPanel nameAndSearchPanel = new JPanel(new BorderLayout());
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT,5, 0));
        JTextField titleField = new JTextField(26);
        titleField.setToolTipText("You can name your search (optional)");
        JLabel titleLabel = new JLabel("  Search Title: ");
        titleLabel.setFont(Font.decode("Dialog-bold-14"));
        titlePanel.add(titleLabel);
        titlePanel.add(titleField);   // may want to set text here...  but need to get a name for the new group....
        titleField.setFont(Font.decode("Dialog-bold-14"));
        nameAndSearchPanel.add(titlePanel, BorderLayout.NORTH);
        // TODO - allow that a name may be supplied, but do validity checking.

        searching = true;
        searchPanel = new SearchPanel();
        nameAndSearchPanel.add(searchPanel, BorderLayout.CENTER);
        Frame theFrame = JOptionPane.getFrameForComponent(this);

        // Now display the search dialog.
        String string1 = "Search Now";
        String string2 = "Cancel";
        Object[] options = {string1, string2};
        int choice = optionPane.showOptionDialog(theFrame,
                nameAndSearchPanel,
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
        preClose(); // Preserve all changes across all open Panels.

        theWorkingDialog.setLocationRelativeTo(rightPane); // This can be needed if windowed app has moved from center screen.
        showWorkingDialog(true); // Show the 'Working...' dialog; it's in a separate thread so we can keep going here...

        doSearch(searchPanel);
    } // end prepareSearch


    // Call this method to do a 'programmatic' rename of a node
    // on the Tree, as opposed to doing it manually via a
    // TreeBranchEditor.  It operates only on the tree and not with
    // any corresponding files; you must do that separately.
    // See the 'Save As...' methodology for a good example.
    private void renameTreeBranchLeaf(@NotNull DefaultMutableTreeNode theBranch, String oldname, String newname) {
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
            case "Goal" -> {
                success = ((GoalGroupPanel) theNoteGroupPanel).saveAs();
                groupParentPath = goalsPath;
                theNoteGroupPanelKeeper = theGoalsKeeper;
            }
            case "Upcoming Event" -> {
                success = ((EventNoteGroupPanel) theNoteGroupPanel).saveAs();
                groupParentPath = eventsPath;
                theNoteGroupPanelKeeper = theEventListKeeper;
            }
            case "To Do List" -> {
                success = ((TodoNoteGroupPanel) theNoteGroupPanel).saveAs();
                groupParentPath = todolistsPath;
                theNoteGroupPanelKeeper = theTodoListKeeper;
            }
        }
        if (null == groupParentPath) return; // Should not happen in normal operation.

        if (success) {
            String newName = theNoteGroupPanel.getGroupName();

            // When the tree selection changes, any open NoteGroup is automatically saved,
            // and the tree selection will change automatically when we do the rename of
            // the leaf on the tree below.  But in this case we do not want that behavior,
            // because we have already saved the file with its new name, milliseconds ago.
            // It wouldn't hurt to let it save again, but why allow it, when all it takes
            // to stop it is:
            theNoteGroupPanel = null;

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


    // Convenience for Panels that are groupings of NoteGroupPanels but do not extend that class.
    // (Currently only the TabbedCalendarNoteGroupPanel, but GoalGroupPanel may be interested...)
    void setTheNoteGroupPanel(NoteGroupPanel noteGroupPanel) { theNoteGroupPanel = noteGroupPanel; }

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

        if (archiveCount > 0) {  // There are already archives - show the selectable list.
            // Build a single-level tree of archive leaves
            DefaultMutableTreeNode archiveTreeRootNode = new DefaultMutableTreeNode("Archives");

            for (String aName : archiveNames) {
                //jScrollPane.add(new Label(aName));
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

            archiveTree.addTreeSelectionListener(e -> {
                appMenuBar.manageMenus("One Archive");
                TreePath newPath = e.getNewLeadSelectionPath();
                if (newPath == null) return;

                // Obtain a reference to the new selection.
                selectedArchiveNode = (DefaultMutableTreeNode) (newPath.getLastPathComponent());
            });

            MouseListener ml = new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    if (e.getClickCount() == 2) {
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
            JPanel promptRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            promptRow.add(thePrompt);
            myBox.add(promptRow);
        }

        rightPane.setViewportView(archivePanel);
    } // end showArchives


    // Called from treeSelectionChanged, to handle different node selections that require the same set of actions.
    private String showBranch(DefaultMutableTreeNode selectedNode) {
        String theNodeString = selectedNode.toString(); // Get the string for the selected node.
        String menuContext = "No Selection";
        BranchHelper branchHelper = null;

        switch (theNodeString) {
            case "Goals" -> {
                menuContext = "Goals Branch Editor";
                branchHelper = new BranchHelper(theTree, theGoalsKeeper, DataArea.GOALS);
            }
            case "Upcoming Events" -> {
                menuContext = "Upcoming Events Branch Editor";
                branchHelper = new BranchHelper(theTree, theEventListKeeper, DataArea.UPCOMING_EVENTS);
            }
            case "To Do Lists" -> {
                menuContext = "To Do Lists Branch Editor";
                branchHelper = new BranchHelper(theTree, theTodoListKeeper, DataArea.TODO_LISTS);
            }
            case "Search Results" -> {
                menuContext = "Search Results Branch Editor";
                branchHelper = new BranchHelper(theTree, theSearchResultsKeeper, DataArea.SEARCH_RESULTS);
            }
        }
        if (branchHelper != null) {
            int resultCount = branchHelper.getChoices().size(); // Choices array list can be empty but not null.
            if (resultCount > 0) {  // No branch editor until after there is at least one NoteGroup.
                TreeBranchEditor tbe = new TreeBranchEditor(theNodeString, selectedNode, branchHelper);
                rightPane.setViewportView(tbe);
            } else { // In this case the tree selection event implies that a new group should be added.
                showWorkingDialog(false);
                theTree.clearSelection(); // This one cannot be restored upon app restart.
                appMenuBar.manageMenus(menuContext);
                // (menus will get managed again before the return from treeSelectionChanged, but
                //   in this case it needs to happen right now, before our next line).
                addNewGroup();
                // Note about differences for SearchResults:  addNewGroup has no handling for it; it will just return.
                // An earlier version here instead went into a new search if this was a 'restoring selection' case,
                // which could only have happened if a new search had been started and then was abandoned upon app
                // termination.  The design decision now is that there is no need to handle such a corner case; allow
                // the group to be removed due to not being found when the app is restarted, which might be days or
                // weeks later.  It will be up to the user at that time to decide to perform another search, or not.
            }
        } else { // Any other top-level branch that does not (yet?) have its own editor.
            JTree jt = new JTree(selectedNode); // Show as a tree but no editing.
            jt.setShowsRootHandles(true);
            rightPane.setViewportView(jt);
        }
        return menuContext;
    } // end showBranch

    void showCurrentNoteGroup() {
        Object theMessage;
        if (theNoteGroupPanel == null) {
            theMessage = "No NoteGroup is selected!";
        } else {
            JScrollPane jScrollPane = new JScrollPane();
            JTextArea jTextArea = new JTextArea();
            String groupChangedString = "\"groupChanged\" : " + theNoteGroupPanel.myNoteGroup.groupChanged + ",\n";
            jTextArea.append(groupChangedString);
            Object[] theData = null; // Not ever expected to stay this value.
            if (theNoteGroupPanel instanceof GoalGroupPanel goalGroupPanel) { // Show the correct sub-panel (tab)
                int index = goalGroupPanel.theTabbedPane.getSelectedIndex();
                switch (index) {
                    case 0 ->  // To Do List
                            theData = goalGroupPanel.theTodoNoteGroupPanel.myNoteGroup.getTheData();
                    case 1 -> // Log Entries
                            theData = goalGroupPanel.theLogNoteGroupPanel.myNoteGroup.getTheData();
                    case 2 -> // Milestones
                            theData = goalGroupPanel.theMilestoneNoteGroupPanel.myNoteGroup.getTheData();
                    case 3 -> // Notes
                            theData = goalGroupPanel.thePlainNoteGroupPanel.myNoteGroup.getTheData();
                }
            } else if (theTabbedCalendarNoteGroupPanel != null) { // Show the correct sub-panel (tab)
                int index = theTabbedCalendarNoteGroupPanel.theTabbedPane.getSelectedIndex();
                theData = switch (index) {
                    case 0 ->  // Day Notes
                            theAppDays.myNoteGroup.getTheData();
                    case 1 -> // Month Notes
                            theAppMonths.myNoteGroup.getTheData();
                    case 2 -> // Year Notes
                            theAppYears.myNoteGroup.getTheData();
                    default -> theData;
                };
            } else {
                theData = theNoteGroupPanel.myNoteGroup.getTheData();
            }
            jTextArea.append(AppUtil.toJsonString(theData));
            jScrollPane.setViewportView(jTextArea);
            jScrollPane.setPreferredSize(new Dimension(600, 500));
            theMessage = jScrollPane;
        }
        optionPane.showMessageDialog(this, theMessage, "Viewing Current NoteGroup", PLAIN_MESSAGE);
    }

    // Called from YearView mouse dbl-click on numeric date, or MonthView mouse dbl-click on the 'day' square.
    @Override
    public void showDay() {
        MemoryBank.debug("showDay called.  viewedDate = " + viewedDate.toString());
        if (appOpts.groupCalendarNotes) {
            theTree.setSelectionPath(calendarNotesPath);
            theTabbedCalendarNoteGroupPanel.theTabbedPane.setSelectedIndex(0);
        } else {
            theTree.setSelectionPath(dayNotesPath);
        }
    } // end showDay

    private String showEvent(DefaultMutableTreeNode selectedNode) {
        String theNodeString = selectedNode.toString(); // Get the string for the selected node.
        String menuContext = "Upcoming Event";  // For manageMenus
        EventNoteGroupPanel eventNoteGroup;

        // If this group has been previously loaded during this session,
        // we can retrieve it from the keeper.
        eventNoteGroup = (EventNoteGroupPanel) theEventListKeeper.get(theNodeString);

        // Otherwise load it, but only if a file for it already exists.
        if (eventNoteGroup == null) {
            eventNoteGroup = (EventNoteGroupPanel) GroupPanelFactory.loadNoteGroupPanel(DataArea.UPCOMING_EVENTS.toString(), theNodeString);
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
            menuContext = "No Selection";  // For manageMenus
        } else {
            theNoteGroupPanel = eventNoteGroup;
            rightPane.setViewportView(theNoteGroupPanel.theBasePanel);
        } // end if
        return menuContext;
    } // end showEvent

    // This is for the consolidated view, not a single Upcoming Event list, which is 'showEvent()'.
    void showEvents() {
        EventNoteGroupPanel theBigPicture = getConsolidatedView();
        if (theBigPicture == null) return;

        // Make a dialog window.
        // A dialog is preferred to the JOptionPane.showMessageDialog, because it is easier to control
        // the size, and we need no additional buttons.
        theEventsDialog = new JDialog((Frame) null, true);
        theEventsDialog.getContentPane().add(theBigPicture.theBasePanel, BorderLayout.CENTER);
//        dialogWindow.setTitle("Scheduled Events");
        theEventsDialog.setSize(680, 580);
        theEventsDialog.setResizable(false);

        // Center the dialog.
        theEventsDialog.setLocationRelativeTo(MemoryBank.logFrame);

        // Go modal -
        theEventsDialog.setVisible(true);
    } // end showEvents


    // Show the Group where the search result was found.  This is going to be its current state and not a snapshot
    // of the group when the data was found, so the note(s) in this group that met the search criteria may not still
    // be here or may not still meet that criteria.  And it is possible that the
    // group itself has gone away.  If the group cannot be shown then nothing happens.
    @Override
    public void showFoundIn(SearchResultData srd) {
        if (srd.foundIn == null) return;
        NoteGroupPanel thePanel = srd.foundIn.getNoteGroupPanel();
        thePanel.setEditable(false);
        theNoteGroupPanel = thePanel; // For 'showCurrentNoteGroup'

        // Whatever view we are about to switch to - is 'disconnected' from the tree, so clear tree selection.
        theTree.clearSelection();

        // set the 'Go Back' menu item to visible
        if (selectedArchiveNode == null) appMenuBar.manageMenus("Viewing FoundIn");
        else appMenuBar.manageMenus("No Selection"); // Or not, if we came from an archive.

        // View the Panel where the search result was found.
        rightPane.setViewportView(thePanel.theBasePanel);
    }

    private String showGoal(DefaultMutableTreeNode selectedNode) {
        String theNodeString = selectedNode.toString(); // Get the string for the selected node.
        String menuContext = "Goal";
        GoalGroupPanel goalGroup;

        // If this group has been previously loaded during this session,
        // we can retrieve it from the keeper.
        goalGroup = (GoalGroupPanel) theGoalsKeeper.get(theNodeString);

        // Otherwise load it if it exists or make a new one if it does not exist.
        if (goalGroup == null) {
            goalGroup = (GoalGroupPanel) GroupPanelFactory.loadNoteGroupPanel(DataArea.GOALS.toString(), theNodeString);

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
            //   then we take it to mean that the data is effectively not there.

            // We can show a notice about what went wrong and what we're
            // going to do about it, but that will only be helpful if
            // the user had just asked to see the selection, and NOT
            // in the case where this situation arose during a program
            // restart where the missing data just happens to be
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
            menuContext = "No Selection";  // For manageMenus
        } else {
            theNoteGroupPanel = goalGroup;
            rightPane.setViewportView(goalGroup.theBasePanel);
        } // end if
        return menuContext;
    } // end showGoal

    private void showHelp() {
        //new Exception("Your help is showing").printStackTrace();
        try {
            String s = MemoryBank.mbHome + "/";
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

        if (theAppDays != null) {
            aLine = "theAppDays:  " + theAppDays.getGroupName();
        } else {
            aLine = "theAppDays: null";
        }
        jTextArea.append(aLine);

        if (theAppMonths != null) {
            aLine = "\ntheAppMonths:  " + theAppMonths.getGroupName();
        } else {
            aLine = "\ntheAppMonths: null";
        }
        jTextArea.append(aLine);

        if (theAppYears != null) {
            aLine = "\ntheAppYears:  " + theAppYears.getGroupName();
        } else {
            aLine = "\ntheAppYears: null";
        }
        jTextArea.append(aLine);
        jTextArea.append("\n"); // Spacer

        aLine = "\ntheGoalsKeeper: " + keptGoals + "\n";
        jTextArea.append(aLine);
        theNames = theGoalsKeeper.getNames();
        for (Object anObject : theNames) {
            aLine = "   " + anObject + "\n";
            jTextArea.append(aLine);
        }

        aLine = "\ntheEventListKeeper: " + keptEvents + "\n";
        jTextArea.append(aLine);
        theNames = theEventListKeeper.getNames();
        for (Object anObject : theNames) {
            aLine = "   " + anObject + "\n";
            jTextArea.append(aLine);
        }

        aLine = "\ntheTodoListKeeper: " + keptTodoLists + "\n";
        jTextArea.append(aLine);
        theNames = theTodoListKeeper.getNames();
        for (Object anObject : theNames) {
            aLine = "   " + anObject + "\n";
            jTextArea.append(aLine);
        }

        aLine = "\ntheSearchResultsKeeper: " + keptSearches + "\n";
        jTextArea.append(aLine);
        theNames = theSearchResultsKeeper.getNames();
        for (Object anObject : theNames) {
            aLine = "   " + anObject + "\n";
            jTextArea.append(aLine);
        }

        jScrollPane.setViewportView(jTextArea);
        jScrollPane.setPreferredSize(new Dimension(600, 500));
        theMessage = jScrollPane;
        optionPane.showMessageDialog(this, theMessage, "Viewing NoteGroup Keepers", PLAIN_MESSAGE);
    }

    // Called from YearView - a click on a Month name
    @Override
    public void showMonthView() {
        MemoryBank.debug("showMonthView called.");
        theTree.setSelectionPath(monthViewPath);
    } // end showMonthView

    // Note that this method is NOT called for the 'T' button; only from the Menu.
    // The view will change to a textual representation of today's date.
    // The value of this feature is questionable but it remains below for now.
    void showToday() {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        JLabel dayTitle = new JLabel();
        dayTitle.setHorizontalAlignment(JLabel.CENTER);
        dayTitle.setForeground(Color.blue);
        dayTitle.setFont(Font.decode("Serif-bold-24"));
        dayTitle.setText(dtf.format(LocalDate.now()));
        rightPane.setViewportView(dayTitle);
        appMenuBar.manageMenus("Today"); // This will get the default / unhandled case.

        // Clear the current tree selection.
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

        // But if we came here via a mouse click on some view then all we need to do is set
        //   the tree's path correctly.
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
            new Thread(() -> theWorkingDialog.setVisible(true)).start(); // Start the thread so that the dialog will show.
        } else {
            new Thread(() -> {
                // Give the 'set visible' time to complete, before going invisible.
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                theWorkingDialog.setVisible(false);
            }).start();
        } // end if show - else hide
    } // end showWorkingDialog


    // The static menu item whose handler calls this, will affect all three CalendarNoteGroup types.
    private void toggleReview() {
        boolean reviewMode = AppMenuBar.reviewMode.isSelected();
        if (theAppDays != null) theAppDays.reviewMode = reviewMode;
        if (theAppMonths != null) theAppMonths.reviewMode = reviewMode;
        if (theAppYears != null) theAppYears.reviewMode = reviewMode;
    }


    // Note that for changes that are code-generated vs a response to user action, we don't come here directly
    //   but can get here by constructing a valid TreePath and then calling setSelectionPath() on the JTree.
    private void treeSelectionChanged(TreePath newPath) {
        selectedArchiveNode = null; // Clear any previous archive selection.

        if (newPath == null) return;
        // You know how some animals will still move or twitch a bit after death?
        // The explanation is that their central nervous system is still sending
        // (random) signals as it shuts down, and there is enough of their body
        // still in working order well enough to react somewhat to those signals.
        // This app is similar in that tests for it can run and complete so
        // quickly that some execution paths here have not finished their
        // processing due to having been started in a separate thread.
        // When that happens we can see null exceptions on some subsequent lines,
        // AFTER tests have passed.  The above line eliminates those twitches.

        // We have started to handle the change; now disallow further input by showing
        // the modal 'working' dialog, until we are finished.
        theWorkingDialog.setLocationRelativeTo(rightPane); // Re-center before showing.
        if (!restoringPreviousSelection) showWorkingDialog(true);

        // Update the current selection row
        // Single-selection mode; Max == Min; take either one.
        appOpts.theSelectionRow = theTree.getMaxSelectionRow();

        // If there was a NoteGroup open prior to this tree selection change, AND if it has changes in its
        //   interface but not in its underlying data then update that underlying data (in memory) now.
        // But actual saving of the updated NoteGroup data will come later.
        if (theNoteGroupPanel != null && theNoteGroupPanel.myNoteGroup.groupChanged) {
            theNoteGroupPanel.unloadNotesPanel(theNoteGroupPanel.theNotePager.getCurrentPage());

            // If the extendedNoteComponent of the open group had been edited then there may have been a Subjects
            //   change.  If so then those changes may be saved right now.
            if (theNoteGroupPanel.subjectEditor != null) {
                theNoteGroupPanel.subjectEditor.saveSubjects(); // This is a no-op if there was no change.
            }
        } // end if

        // Obtain a reference to the new Tree Node selection.
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) (newPath.getLastPathComponent());
        // This is better than 'theTree.getLastSelectedPathComponent()' because it works for
        //   normal tree selection events but also allows for 'phantom' selections; tree
        //   paths that were created and selected by code vs those that came from a user's
        //   mouse click event on an existing (visible and active) tree node.  And it will
        //   never be null because newPath is a non-null TreePath.

        // Get the string for the selected node.
        String theNodeString = selectedNode.toString();
        MemoryBank.debug("New tree selection: " + theNodeString);
        appOpts.theSelection = theNodeString; // Not used, but helpful during a visual review of the persisted options.
        String menuContext = theNodeString;  // for menu management; this default value changes in most cases, below.

        // Get the name of the node's parent.  Thanks to the way we have displayed the tree and
        // the unselect-ability of the tree root, we never expect the parent path to be null.
        String parentNodeName = newPath.getParentPath().getLastPathComponent().toString();

        //-----------------------------------------------------
        // These booleans will help us to avoid incorrect assumptions based on the text of the
        //   new selection, in cases where some bozo named their group the same as a parent
        //   branch.  For example, a To Do list named 'To Do Lists'.  We first look to see if
        //   the selection is a branch before we would start handling it as a leaf, and by
        //   then we will know which branch the leaf belongs on.
        //-----------------------------------------------------
        boolean isTopLevel = parentNodeName.equals("App");
        boolean hasChildren = selectedNode.getChildCount() > 0;
        boolean isArchives = isTopLevel && theNodeString.equals(DataArea.ARCHIVES.toString());

        theNoteGroupPanel = null; // initialize

        //<editor-fold desc="Actions Depending on the selection">
        //==========================================================================================================
        if (isArchives) showArchives();  // The order that we test the booleans is critical to the outcome.
        else if (theNodeString.equals("Calendar Notes") & !hasChildren) showCalendarNotes();
        else if (isTopLevel) menuContext = showBranch(selectedNode); // Edit the indicated branch, OR - add a new group.
        else if (parentNodeName.equals("Goals")) menuContext = showGoal(selectedNode);  // Selection of a Goal
        else if (parentNodeName.equals("Upcoming Events")) menuContext = showEvent(selectedNode);
        else if (parentNodeName.equals("To Do Lists")) menuContext = showTodoList(selectedNode);
        else if (parentNodeName.equals("Search Results")) menuContext = showSearchResult(selectedNode);
        else if (theNodeString.equals("Day Notes")) { // This node means that CalendarNotes are NOT grouped.
            if (theTabbedCalendarNoteGroupPanel != null) theTabbedCalendarNoteGroupPanel = null;
            if (theAppDays == null) {
                theAppDays = new DayNoteGroupPanel();
                theAppDays.setAlteredDateListener(this);
            } else { // Previously constructed, so ensure that it is editable.
                log.debug("Using the previously constructed 'theAppDays' for " + theNodeString);
                if (!theAppDays.getEditable()) theAppDays.setEditable(true);
            }
            // Setting the date here will cause a group save if there are unsaved changes, and we don't want
            //   that in the case where there has been no change to the viewed date and the user is not yet
            //   ready to save their work and might still undo.
            if (!theAppDays.getTitle().equals(theAppDays.getTitle(viewedDate))) {
                theAppDays.setDate(viewedDate);
            }
            theNoteGroupPanel = theAppDays;
            rightPane.setViewportView(theAppDays.theBasePanel);
        } else if (theNodeString.equals("Month Notes")) { // This node means that CalendarNotes are NOT grouped.
            if (theTabbedCalendarNoteGroupPanel != null) theTabbedCalendarNoteGroupPanel = null;
            if (theAppMonths == null) {
                theAppMonths = new MonthNoteGroupPanel(); // Takes current date as default initial 'choice'.
                theAppMonths.setAlteredDateListener(this);
            } else { // Previously constructed, so ensure that it is editable.
                log.debug("Using the previously constructed 'theAppMonths' for " + theNodeString);
                if (!theAppMonths.getEditable()) theAppMonths.setEditable(true);
            }
            // Setting the date here will cause a group save if there are unsaved changes, and we don't want
            //   that in the case where there has been no change to the viewed date and the user is not yet
            //   ready to save their work and might still undo.
            if (!theAppMonths.getTitle().equals(theAppMonths.getTitle(viewedDate))) {
                theAppMonths.setDate(viewedDate);
            }
            theNoteGroupPanel = theAppMonths;
            rightPane.setViewportView(theAppMonths.theBasePanel);
        } else if (theNodeString.equals("Year Notes")) { // This node means that CalendarNotes are NOT grouped.
            if (theTabbedCalendarNoteGroupPanel != null) theTabbedCalendarNoteGroupPanel = null;
            if (theAppYears == null) {
                theAppYears = new YearNoteGroupPanel();
                theAppYears.setAlteredDateListener(this);
            } else { // Previously constructed, so ensure that it is editable.
                log.debug("Using the previously constructed 'theAppYears' for " + theNodeString);
                if (!theAppYears.getEditable()) theAppYears.setEditable(true);
            }
            // Setting the date here will cause a group save if there are unsaved changes, and we don't want
            //   that in the case where there has been no change to the viewed date and the user is not yet
            //   ready to save their work and might still undo.
            if (!theAppYears.getTitle().equals(theAppYears.getTitle(viewedDate))) {
                theAppYears.setDate(viewedDate);
            }
            theNoteGroupPanel = theAppYears;
            rightPane.setViewportView(theAppYears.theBasePanel);
        } else if (theNodeString.equals("Year View")) {
            if (theYearView == null) {
                theYearView = new YearView(viewedDate);
                theYearView.setTreePanel(this);
            } else { // The YearView was previously constructed.
                theYearView.setView(viewedDate); // To show the right Year
            } // end if
            rightPane.setViewportView(theYearView);
        } else if (theNodeString.equals("Month View")) {
            // Capture new icons, if any.
            if (theAppDays != null) theAppDays.preClosePanel();

            if (theMonthView == null) {
                theMonthView = new MonthView(viewedDate);
                theMonthView.setTreePanel(this);
            } else {  // The MonthView was previously constructed.
                theMonthView.setView(viewedDate);
            }
            rightPane.setViewportView(theMonthView);
        } else {
            // Any other as-yet unhandled node on the tree (usually a placeholder such as 'Week View')
            JPanel jp = new JPanel(new GridBagLayout());
            jp.add(new JLabel(theNodeString));
            rightPane.setViewportView(jp);
        } // end if/else if
        //==========================================================================================================
        //</editor-fold>

        // This will change out the active menu for one that is appropriate for the selected tree node.
        appMenuBar.manageMenus(menuContext);

        if (theNoteGroupPanel != null) { // Not all tree node selections come with a NoteGroup.
            // With each Group type sharing the same static menu, we need to ensure that menu items are correctly
            //   enabled or disabled upon a change from one group to another of the same type.
            theNoteGroupPanel.adjustMenuItems(theNoteGroupPanel.myNoteGroup.groupChanged);
        }

        showWorkingDialog(false); // This may have already been done, but no harm in doing it again.
    } // end treeSelectionChanged

    private void showCalendarNotes() {
        if (theTabbedCalendarNoteGroupPanel == null) {
            // Construct the three date-based NoteGroupPanels, if needed, and add the AlteredDateListener.
            if (theAppDays == null) theAppDays = new DayNoteGroupPanel();
            theAppDays.setAlteredDateListener(this);
            if (theAppMonths == null) theAppMonths = new MonthNoteGroupPanel();
            theAppMonths.setAlteredDateListener(this);
            if (theAppYears == null) theAppYears = new YearNoteGroupPanel();
            theAppYears.setAlteredDateListener(this);
            theTabbedCalendarNoteGroupPanel = new TabbedCalendarNoteGroupPanel();
        }
        rightPane.setViewportView(theTabbedCalendarNoteGroupPanel.theBasePanel);

        int index = theTabbedCalendarNoteGroupPanel.theTabbedPane.getSelectedIndex();
        switch (index) {
            case 0 -> theNoteGroupPanel = theAppDays;
            case 1 -> theNoteGroupPanel = theAppMonths;
            case 2 -> theNoteGroupPanel = theAppYears;
        }
    } // end showCalendarNotes

    private String showSearchResult(DefaultMutableTreeNode selectedNode) {
        String theNodeString = selectedNode.toString(); // Get the string for the selected node.
        String menuContext = "Search Result";
        SearchResultGroupPanel searchResultGroupPanel;
        theWayBack = theTree.getSelectionPath();

        // If the search has been previously loaded during this session,
        // we can retrieve the group for it from the keeper.
        searchResultGroupPanel = (SearchResultGroupPanel) theSearchResultsKeeper.get(theNodeString);

        // Otherwise construct it, but only if a file for it already exists.
        if (searchResultGroupPanel == null) {
            searchResultGroupPanel = (SearchResultGroupPanel) GroupPanelFactory.loadNoteGroupPanel(DataArea.SEARCH_RESULTS.toString(), theNodeString);
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
            menuContext = "No Selection";  // For manageMenus
        } else {
            theNoteGroupPanel = searchResultGroupPanel;
            searchResultGroupPanel.treePanel = this;
            rightPane.setViewportView(theNoteGroupPanel.theBasePanel);
        } // end if
        return menuContext;
    }

    private String showTodoList(DefaultMutableTreeNode selectedNode) {
        String theNodeString = selectedNode.toString(); // Get the string for the selected node.
        String menuContext = "To Do List";  // For manageMenus
        TodoNoteGroupPanel todoNoteGroup;

        // If the list has been previously loaded during this session,
        // we can retrieve the group from the keeper.
        todoNoteGroup = (TodoNoteGroupPanel) theTodoListKeeper.get(theNodeString);

        // Otherwise load it, but only if a file for it already exists.
        if (todoNoteGroup == null) {
            todoNoteGroup = (TodoNoteGroupPanel) GroupPanelFactory.loadNoteGroupPanel(DataArea.TODO_LISTS.toString(), theNodeString);
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
            menuContext = "No Selection";  // For manageMenus
        } else {
            theNoteGroupPanel = todoNoteGroup;
            rightPane.setViewportView(theNoteGroupPanel.theBasePanel);
        } // end if
        return menuContext;
    }

    //-------------------------------------------------
    // Method Name:  updateAppOptions
    //
    // Capture the current tree configuration in terms of node expansion/contraction
    //   and variable group contents, and put it into appOpts (AppOptions class).
    //-------------------------------------------------
    void updateAppOptions(boolean updateLists) {
        appOpts.groupCalendarNotes = AppMenuBar.groupCalendarNotes.getState();

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

    @Override // implementation of the method in the TreeSelectionListener interface.
    public void valueChanged(TreeSelectionEvent e) {
        // This event-handling method is called due to user action to change the selection
        // and does not get called if there is no change.  So - this
        // is the best place to accept that an offer to undo a deletion has been abandoned.
        appMenuBar.showRestoreOption(false);
        appMenuBar.requestFocus(); // Do not start out with any note already selected.

        final TreePath newPath = e.getNewLeadSelectionPath();
        if (restoringPreviousSelection) {
            // We don't need to handle this event from a separate thread.
            // This is because when restoring a previous selection, the response
            //   should be much faster since any stored data has already
            //   been accessed and loaded.  Although there is one
            //   exception to that, at program restart but in that
            //   case we have the splash screen and main progress bar.
            treeSelectionChanged(newPath);
        } else {
            // This is a user-directed selection;
            // We handle this event by starting a separate new thread so we can
            //   quickly return from here to the 'main' JVM thread.  Otherwise,
            //   the 'working' dialog that is displayed during the treeSelectionChanged
            //   method would never update until after all actions had completed,
            //   and that defeats its entire purpose.  But be aware that this methodology
            //   can derail breakpoint debugging, especially during test runs.  This is
            //   why many tests will first set restoringPreviousSelection to true.
            new Thread(() -> {
                // AppUtil.localDebug(true);
                treeSelectionChanged(newPath);
                // AppUtil.localDebug(false);
            }).start(); // Start the thread
        }
    } // end valueChanged

} // end AppTreePanel class

