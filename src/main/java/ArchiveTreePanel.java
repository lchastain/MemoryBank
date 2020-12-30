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
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.Vector;

import static javax.swing.JOptionPane.PLAIN_MESSAGE;


public class ArchiveTreePanel extends JPanel implements TreeSelectionListener, AlteredDateListener {
    static final long serialVersionUID = 1L; // JPanel wants this but we will not serialize.
    private static final Logger log = LoggerFactory.getLogger(ArchiveTreePanel.class);

    JFrame archiveWindow;
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

    private NoteGroupPanel theNoteGroupPanel; // A reference to the current selection
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

    JDialog theWorkingDialog;

    public ArchiveTreePanel(String archiveName) {
        super(new GridLayout(1, 0));
        archiveWindow = new JFrame("Archive: " + archiveName);
        appOpts = MemoryBank.appDataAccessor.getArchiveOptions(archiveName);

        //<editor-fold desc="Make the 'Working...' dialog">
        theWorkingDialog = new JDialog(archiveWindow, "Working", true);
        JLabel lbl = new JLabel("Please Wait...");
        lbl.setFont(Font.decode("Dialog-bold-16"));
        String strWorkingIcon = MemoryBank.logHome + File.separatorChar;
        strWorkingIcon += "icons" + File.separatorChar + "animated" + File.separatorChar + "manrun.gif";
        lbl.setIcon(new AppIcon(strWorkingIcon));
        lbl.setVerticalTextPosition(JLabel.TOP);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        theWorkingDialog.add(lbl);
        theWorkingDialog.pack();
        theWorkingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        //</editor-fold>

        optionPane = new Notifier() { }; // Uses all default methods.

//        setOpaque(true);

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
        // If there is a previous selection -
        if (appOpts.theSelectionRow >= 0) theTree.setSelectionRow(appOpts.theSelectionRow);
        // There are a few cases where there would be no previous selection.
        // Ex: Showing the About graphic, First-time-ever for this user, lost or corrupted AppOptions.
        // In these cases leave as-is and the view will go to the About graphic.

        // I'm a self-starter!
        archiveWindow.getContentPane().add(this);
        archiveWindow.pack();
        archiveWindow.setSize(new Dimension(880, 600));
        archiveWindow.setLocationRelativeTo(null); // Center screen
        archiveWindow.setVisible(true);
    } // end constructor for ArchiveTreePanel


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

        // Get the parent of the node to be removed -
        DefaultMutableTreeNode theParent = (DefaultMutableTreeNode) node.getParent();

        // Remove this node from the tree but do not let the removal result in the need to
        // process another tree selection event (the 'event' is selection being set to null).
        theTree.removeTreeSelectionListener(this);
        theParent.remove(node); // Previously:  node.removeFromParent(); - worked only sometimes.

        // Redisplay the branch that had the removal (but not if it was the 'trunk')
        if(!theParent.toString().equals("App")) {
            treeModel.nodeStructureChanged(theParent);

            // Select the parent branch.
            TreeNode[] pathToRoot = theParent.getPath();
            theTree.setSelectionPath(new TreePath(pathToRoot));

            updateTreeState(true); // Needed now, in case there is a new link target selection.
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
        branch = new DefaultMutableTreeNode("Search Results", true);
        trunk.add(branch);
        pathToRoot = branch.getPath();
        searchresultsPath = new TreePath(pathToRoot);
        theSearchResultsKeeper = new NoteGroupPanelKeeper();

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

    // Handling for the 'Go Back' menu item, to go back to Search Results after
    //   viewing one of its 'FoundIn' items.
    private void doGoBack() {
        // If menu management is being done correctly then 'theWayBack' will never be null
        //   when execution comes here to this method.  So, not going to condition this call.
        // For testers - be aware of this; changing this code for a test-only situation - not going to happen.
        theTree.setSelectionPath(theWayBack);
    }


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


    public JTree getTree() {
        return theTree;
    }


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

        if (node == null && aboutPanel == theContent) { // This means we can toggle back to a previous tree selection.
            // Reset tree to the state it was in before.
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

//            appMenuBar.manageMenus("About"); // This will get the default / unhandled case.
        } // end if
    } // end showAbout


    // Called from YearView mouse dbl-click on numeric date, or MonthView mouse dbl-click on the 'day' square.
    void showDay() {
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
    void showFoundIn(SearchResultData srd) {
        if(srd.foundIn == null) return;
        NoteGroupPanel thePanel = srd.foundIn.getNoteGroupPanel();
        thePanel.setEditable(false);
        theNoteGroupPanel = thePanel; // For 'showCurrentNoteGroup'

        // Preserve the selection path, for 'goBack'
        // set GoBack to visible
        // if goback, set goback to not visible

        theTree.clearSelection();
//        appMenuBar.manageMenus("Viewing FoundIn");
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
//        appMenuBar.manageMenus("Today"); // This will get the default / unhandled case.

        // Clear the current tree selection, so they can select it again
        // and get back to a 'normal' view.  There was a previous version here, where we had kept the earlier
        // Panel so we could toggle back to it, but dropped that feature - too cute, and not that helpful.
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


    // This method will either show or hide a small modal
    //   dialog with an animated gif and a 'Working...' message.
    //   Call this method with 'true' before you start
    //   a potentially long task that the user must wait for,
    //   then call it with 'false' to go on.  It is static
    //   in order to give access to external classes such
    //   as group headers, that need to wait for sorting.
    void showWorkingDialog(boolean showIt) {
        if (showIt) {
            theWorkingDialog.setLocationRelativeTo(archiveWindow); // In case the app has been moved around.
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

    void treeSelectionChanged(TreePath newPath) {
        if (newPath == null) return;

        // Obtain a reference to the new selection.
        DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) (newPath.getLastPathComponent());
        // This is better than 'theTree.getLastSelectedPathComponent()' because it works for
        //   normal tree selection events but also allows for 'phantom' selections; tree
        //   paths that were created and selected by code vs those that came from a user's
        //   mouse click event on an existing (visible and active) tree node.
        if (selectedNode == null) return;
        selectedArchiveNode = null; // Clear any previous archive selection.

        // We have started to handle the change; now disallow
        //   further input until we are finished.
        theWorkingDialog.setLocationRelativeTo(rightPane); // Re-center before showing.
        showWorkingDialog(true);

        // Update the current selection row
        // Single-selection mode; Max == Min; take either one.
        appOpts.theSelectionRow = theTree.getMaxSelectionRow();

        // Get the string for the selected node.
        String theNodeString = selectedNode.toString();
        MemoryBank.debug("New tree selection: " + theNodeString);
        appOpts.theSelection = theNodeString; // Not used, but helpful during a visual review of the persisted options.

        // Get the name of the node's parent.  Thanks to the way we have created the tree and
        // the unselectability of the tree root, we never expect the parent path to be null.
        String parentNodeName = newPath.getParentPath().getLastPathComponent().toString();

        theNoteGroupPanel = null; // initialize

        //<editor-fold desc="Actions Depending on the selection">
        if (!selectedNode.isLeaf()) {  // Looking at expandable nodes
            JTree jt = new JTree(selectedNode); // Show as a tree but no editing.
            jt.setShowsRootHandles(true);
            rightPane.setViewportView(jt);
        } else if (parentNodeName.equals("Goals")) { // Selection of a Goal
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

                // So we show a notice about what went wrong and what we're going to do about it.
                    showWorkingDialog(false);
                    JOptionPane.showMessageDialog(this,
                            "Cannot read in the Goal.\n" +
                                    "This Goal selection will be removed.",
                            "Data not accessible", JOptionPane.WARNING_MESSAGE);

                closeGroup(); // File is already gone; this just removes the tree node.
            } else {
                // There is only one menu, for ALL Goals.  It needs to be reset with every list change.
//                goalGroup.setListMenu(appMenuBar.getNodeMenu(selectionContext));

                theNoteGroupPanel = goalGroup;
                rightPane.setViewportView(goalGroup.theBasePanel);
            } // end if

        } else if (parentNodeName.equals("Upcoming Events")) { // Selection of an Event group
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

                // So we show a notice about what went wrong and what we're going to do about it.
                    showWorkingDialog(false);
                    JOptionPane.showMessageDialog(this,
                            "Cannot read in the Event group.\n" +
                                    "This group selection will be removed.",
                            "File not accessible", JOptionPane.WARNING_MESSAGE);

                closeGroup(); // File is already gone; this just removes the tree node.
            } else {
                // There is only one menu, for ALL event lists.  It needs to be reset with every list change.
//                eventNoteGroup.setListMenu(appMenuBar.getNodeMenu(selectionContext));

                theNoteGroupPanel = eventNoteGroup;
                rightPane.setViewportView(theNoteGroupPanel.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("To Do Lists")) {
            // Selection of a To Do List
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

                // So we show a notice about what went wrong and what we're going to do about it.
                    showWorkingDialog(false);
                    JOptionPane.showMessageDialog(this,
                            "Cannot read in the To Do List.\n" +
                                    "This list selection will be removed.",
                            "List not accessible", JOptionPane.WARNING_MESSAGE);

                closeGroup(); // File is already gone; this just removes the tree node.
            } else {
                // There is only one menu, for ALL todo lists.  It needs to be reset with every list change.
//                todoNoteGroup.setListMenu(appMenuBar.getNodeMenu(selectionContext));

                theNoteGroupPanel = todoNoteGroup;
                rightPane.setViewportView(theNoteGroupPanel.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("Search Results")) {
            // Selection of a Search Result List
            SearchResultGroupPanel searchResultGroupPanel;
            theWayBack = theTree.getSelectionPath();

            // If the search has been previously loaded during this session,
            // we can retrieve the group for it from the keeper.
            searchResultGroupPanel = (SearchResultGroupPanel) theSearchResultsKeeper.get(theNodeString);

            // Otherwise construct it, but only if a file for it already exists.
            if (searchResultGroupPanel == null) {
                searchResultGroupPanel = (SearchResultGroupPanel) GroupPanelFactory.loadNoteGroupPanel(parentNodeName, theNodeString);
                if (searchResultGroupPanel != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    theSearchResultsKeeper.add(searchResultGroupPanel);
                }
            } else {
                log.debug("Retrieved '" + theNodeString + "' from the keeper");
            }

            if (searchResultGroupPanel == null) {
                // We just tried to retrieve it or to load it, so if it is STILL null
                //   then we take it to mean that the file is effectively not there.

                // So we show a notice about what went wrong and what we're going to do about it.
                    showWorkingDialog(false);
                    JOptionPane.showMessageDialog(this,
                            "Cannot read in the search results.\n" +
                                    "This search results selection will be removed.",
                            "Results not accessible", JOptionPane.WARNING_MESSAGE);

                closeGroup(); // File is already gone; this just removes the tree node.
            } else {
                theNoteGroupPanel = searchResultGroupPanel;
                rightPane.setViewportView(theNoteGroupPanel.theBasePanel);
            } // end if
        } else if (theNodeString.equals("Year View")) {
            if (theYearView == null) {
                theYearView = new YearView(viewedDate);
//                theYearView.setParent(this);
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
                // from appearing in the final (empty) DayCanvases of the MonthCanvas grid (not sure why).
                theMonthView.setChoice(selectedDate); // This sets the 'choice' label and day highlight, if appropriate.
//                theMonthView.setParent(this);
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
//                theAppDays.setListMenu(appMenuBar.getNodeMenu(selectionContext));
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
//                theAppMonths.setListMenu(appMenuBar.getNodeMenu(selectionContext));
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
//                theAppYears.setListMenu(appMenuBar.getNodeMenu(selectionContext));
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

//        appMenuBar.manageMenus(selectionContext);
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
        final TreePath newPath = e.getNewLeadSelectionPath();
        // Handle from a separate thread.
        new Thread(new Runnable() {
            public void run() {
                // AppUtil.localDebug(true);
                treeSelectionChanged(newPath);
                // AppUtil.localDebug(false);
            }
        }).start(); // Start the thread

    } // end valueChanged

} // end AppTreePanel class

