/*
  This is a ghost of the AppTreePanel; used only to show read-only archived NoteGroups.
*/

// Quick-reference notes:
// 
// Tree Selection events - valueChanged() --> treeSelectionChanged() in a new thread.

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class ArchiveTreePanel extends JPanel implements TreePanel, TreeSelectionListener, AlteredDateListener {
    static final long serialVersionUID = 1L; // JPanel wants this but we will not serialize.
    private static final Logger log = LoggerFactory.getLogger(ArchiveTreePanel.class);

    JFrame archiveWindow;
    Notifier optionPane;
    DataAccessor dataAccessor;
    //-------------------------------------------------------------------

    private JTree theTree;
    private DefaultTreeModel treeModel;
    private final JScrollPane rightPane;

    // Paths to expandable 'parent' nodes
    private TreePath viewsPath;
    private TreePath notesPath;
    private TreePath todolistsPath;
    private TreePath searchresultsPath;

    DayNoteGroupPanel theAppDays;
    MonthNoteGroupPanel theAppMonths;
    YearNoteGroupPanel theAppYears;
    MonthView theMonthView;
    YearView theYearView;
    private final JSplitPane splitPane;

    private final LocalDate selectedDate;  // The selected date
    private LocalDate viewedDate;    // A date to be shown but not as a 'choice'.
    private ChronoUnit viewedDateGranularity;

    private DefaultMutableTreeNode theRootNode;

    // Predefined Tree Paths to 'leaf' nodes.
    TreePath dayNotesPath;
    private TreePath weekViewPath;
    TreePath monthNotesPath;
    TreePath yearNotesPath;
    TreePath yearViewPath;
    TreePath monthViewPath;
    private TreePath eventsPath;
    private TreePath goalsPath;

    private final AppOptions appOpts;

    JDialog theWorkingDialog;
    String archiveName; // The one with colons in the time portion..

    public ArchiveTreePanel(String theName) {
        super(new GridLayout(1, 0));
        archiveName = theName;
        dataAccessor = MemoryBank.dataAccessor;
        archiveWindow = new JFrame("Archive: " + archiveName);

        // When this archive is being deleted from the AppTreePanel, this
        // provides the handle that it needs to be able to close any open windows to this archive.
        String archiveKey = archiveName + " " + UUID.randomUUID();
        AppTreePanel.theInstance.archiveWindows.put(archiveKey, archiveWindow);

        // Now - use the name to load in the archived application options.
        appOpts = dataAccessor.getArchiveOptions(archiveName);

        //<editor-fold desc="Make the 'Working...' dialog">
        theWorkingDialog = new JDialog(archiveWindow, "Working", true);
        JLabel lbl = new JLabel("Please Wait...");
        lbl.setFont(Font.decode("Dialog-bold-16"));
        IconInfo iconInfo = new IconInfo(DataArea.APP_ICONS, "animated:manrun", "gif");
        lbl.setIcon(iconInfo.getImageIcon());
        lbl.setVerticalTextPosition(JLabel.TOP);
        lbl.setHorizontalTextPosition(JLabel.CENTER);
        theWorkingDialog.add(lbl);
        theWorkingDialog.pack();
        theWorkingDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        //</editor-fold>

        optionPane = new Notifier() { }; // Uses all default methods.

        MemoryBank.update("Recreating the previous Tree configuration");
        createTree();  // Create the tree.

        // Listen for when the selection changes.
        // We need to do this now so that the proper initialization
        //   occurs when we restore the previous selection, below.
        theTree.addTreeSelectionListener(this);

        // Create the scroll pane and add the tree to it.
        JScrollPane treeView = new JScrollPane(theTree);

        // Create the viewing pane and start with the 'about' graphic.
        AppImage abbowt = new AppImage(MemoryBank.mbHome + "/images/ABOUT.gif");
        JPanel aboutPanel = new JPanel(new GridBagLayout());
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

        // Do not try to restore the last selection.   (appOpts.theSelectionRow)
        // Given that this is an archive that is being reviewed days, weeks or months later, knowing the last
        //   tree selection that the user made before making this archive is going to have questionable value.
        //   But disallowing the reselection avoids a potential problem due to archive trees not being an
        //   exact image of the 'live' tree; there could be differences in row counts due to newly-introduced
        //   grouping options.
        // The view will go to the About graphic, and this has the added benefit of making it that much clearer
        //  that the user is viewing archived (not current) data.

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

            boolean updateLists = true; // If inline, IJ complains about the method not ever having another value.
            updateTreeState(updateLists); // Needed now, in case there is a new link target selection.
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


    @Override
    public void dateChanged(DateRelatedDisplayType whoChangedIt, LocalDate theNewDate) {
        viewedDate = theNewDate;
    }

    public JTree getTree() {
        return theTree;
    }

    @Override
    public LocalDate getViewedDate() { return viewedDate; }

    // Call this method after changes have been made to the tree, to
    //   cause a repaint.  The first line below does that all by itself,
    //   but it also results in a tree with all nodes collapsed.  So the
    //   rest of this method is needed to re-expand any nodes that
    //   should have stayed that way.
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


//    @Override
//    public void setSelectedDate(LocalDate theSelection) {
//        selectedDate = theSelection;
//        viewedDate = theSelection;
//        viewedDateGranularity = ChronoUnit.DAYS;
//    }

    @Override
    public  void setViewedDate(int theYear) {
        viewedDate = LocalDate.of(theYear, viewedDate.getMonth(), viewedDate.getDayOfMonth());
//        viewedDateGranularity = ChronoUnit.YEARS;
    }

    @Override
    public void setViewedDate(LocalDate theViewedDate) {
        viewedDate = theViewedDate;
//        viewedDateGranularity = theGranularity;
    }


    // Called from YearView mouse dbl-click on numeric date, or MonthView mouse dbl-click on the 'day' square.
    @Override
    public void showDay() {
        MemoryBank.debug("showDay called.");
        theTree.setSelectionPath(dayNotesPath);
    } // end showDay


    // Show the Group where the search result was found.  This is going to be its current state and not a snapshot
    // of the group when the data was found, so the note(s) in this group that met the search criteria may not still
    // be here or may not still meet that criteria.  And it is possible that the
    // group itself has gone away.  If the group cannot be shown then nothing happens.
    @Override
    public void showFoundIn(SearchResultData srd) {
        if(srd.foundIn == null) return;
        NoteGroupPanel thePanel = srd.foundIn.getNoteGroupPanel();
        thePanel.setEditable(false);

        // Whatever view we are about to switch to - is 'disconnected' from the tree, so clear tree selection.
        theTree.clearSelection();

        rightPane.setViewportView(thePanel.theBasePanel);
    }


    // Called from YearView - a click on a Month name
    @Override
    public void showMonthView() {
        MemoryBank.debug("showMonthView called.");
        theTree.setSelectionPath(monthViewPath);
    } // end showMonthView


    @Override
    public void showWeek(LocalDate theWeekToShow) {
        MemoryBank.debug("showWeek called.");
        // This method is called from external contexts such as MonthViewCanvas and YearViewCanvas and Tests.
        // There IS not actually a view to show, here.  The rightPane is
        // just loaded with the text, 'Week View'.  Therefore when this node is selected directly
        // on the tree, it does not come here but just shows the text of a request that it does
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

        // Get the date of the archive -
        LocalDateTime archiveDateTime = MemoryBank.dataAccessor.getDateTimeForArchiveName(archiveName);
        LocalDate theArchiveDate = archiveDateTime.toLocalDate();

        // Get the name of the node's parent.  Thanks to the way we have created the tree and
        // the unselectability of the tree root, we never expect the parent path to be null.
        String parentNodeName = newPath.getParentPath().getLastPathComponent().toString();

        //<editor-fold desc="Actions Depending on the selection">
        if (!selectedNode.isLeaf()) {  // Looking at expandable nodes
            JTree jt = new JTree(selectedNode); // Show as a tree but no editing.
            jt.setShowsRootHandles(true);
            rightPane.setViewportView(jt);
        } else if (parentNodeName.equals("Goals")) { // Selection of a Goal
            // Make the corresponding GroupInfo -
            GroupInfo groupInfo = new GroupInfo(theNodeString, GroupType.GOALS);
            groupInfo.archiveName = archiveName;

            // Load the archived Goal group panel, if it exists (but there is no operationally valid reason why it wouldn't).
            GoalGroupPanel goalGroupPanel = null;
            if(groupInfo.exists()) {
                // Yes, existence is also checked when attempting to load the data, but if it fails in that
                // case then you get a new, empty NoteGroup.  For archives - if we can't have the original
                // then we don't want one at all.
                goalGroupPanel = new GoalGroupPanel(groupInfo);
            }

            showWorkingDialog(false);

            if (goalGroupPanel == null) {
                // We just tried to load it, so if it is still null then we take it to mean that the file
                // is not there.  So we show a notice about that, before we remove this leaf from any
                // further user attempts to retrieve it.
                JOptionPane.showMessageDialog(this,
                        "Cannot read in the Goal.\n" +
                                "This Goal selection will be removed.",
                        "Data not accessible", JOptionPane.WARNING_MESSAGE);

                // Nothing else can be done; the group was unavailable for some reason.
                closeGroup(); // This just removes the tree node, but it will reappear the next time
                // the archive is viewed, since the archive options are not updated or preserved.
            } else {
                rightPane.setViewportView(goalGroupPanel.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("Upcoming Events")) { // Selection of an Event group
            // Make the corresponding GroupInfo -
            GroupInfo groupInfo = new GroupInfo(theNodeString, GroupType.EVENTS);
            groupInfo.archiveName = archiveName;

            // Load the archived Event group panel, if it exists (but there is no operationally valid reason why it wouldn't).
            EventNoteGroupPanel eventNoteGroupPanel = null;
            if(groupInfo.exists()) {
                // Yes, existence is also checked when attempting to load the data, but if it fails in that
                // case then you get a new, empty NoteGroup.  For archives - if we can't have the original
                // then we don't want one at all.
                eventNoteGroupPanel = new EventNoteGroupPanel(groupInfo);
            }

            showWorkingDialog(false);

            if (eventNoteGroupPanel == null) {
                // We just tried to load it, so if it still null then we take it to mean that the file is
                // not there.  So we show a notice about that, before we remove this leaf from any
                // further user attempts to retrieve it.
                JOptionPane.showMessageDialog(this,
                        "Cannot read in the Event List.\n" +
                                "This Event selection will be removed.",
                        "Data not accessible", JOptionPane.WARNING_MESSAGE);

                // Nothing else can be done; the group was unavailable for some reason.
                closeGroup(); // This just removes the tree node, but it will reappear the next time
                // the archive is viewed, since the archive options are not updated or preserved.
            } else {
                rightPane.setViewportView(eventNoteGroupPanel.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("To Do Lists")) { // Selection of a To Do List
            // Make the corresponding GroupInfo -
            GroupInfo groupInfo = new GroupInfo(theNodeString, GroupType.TODO_LIST);
            groupInfo.archiveName = archiveName;

            // Load the archived group panel, if it exists (but there is no operationally valid reason why it wouldn't).
            TodoNoteGroupPanel todoNoteGroupPanel = null;
            if (groupInfo.exists()) {
                // Yes, existence is also checked when attempting to load the data, but if it fails in that
                // case then you get a new, empty NoteGroup.  For archives - if we can't have the original
                // then we don't want one at all.
                todoNoteGroupPanel = new TodoNoteGroupPanel(groupInfo);
            }

            showWorkingDialog(false);

            if (todoNoteGroupPanel == null) {
                // We just tried to load it, so if it still null then we take it to mean that the file is
                // not there.  So we show a notice about that, before we remove this leaf from any
                // further user attempts to retrieve it.
                JOptionPane.showMessageDialog(this,
                        "Cannot read in the To Do List.\n" +
                                "This To Do list selection will be removed.",
                        "List not accessible", JOptionPane.WARNING_MESSAGE);

                // Nothing else can be done; the group was unavailable for some reason.
                closeGroup(); // This just removes the tree node, but it will reappear the next time
                // the archive is viewed, since the archive options are not updated or preserved.
            } else {
                rightPane.setViewportView(todoNoteGroupPanel.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("Search Results")) { // Selection of a Search Result List
            // Make the corresponding GroupInfo -
            GroupInfo groupInfo = new GroupInfo(theNodeString, GroupType.SEARCH_RESULTS);
            groupInfo.archiveName = archiveName;

            // Load the archived group panel, if it exists (but there is no operationally valid reason why it wouldn't).
            SearchResultGroupPanel searchResultGroupPanel = null;
            if (groupInfo.exists()) {
                // Yes, existence is also checked when attempting to load the data, but if it fails in that
                // case then you get a new, empty NoteGroup.  For archives - if we can't have the original
                // then we don't want one at all.
                searchResultGroupPanel = new SearchResultGroupPanel(groupInfo);
                searchResultGroupPanel.treePanel = this;
            }

            showWorkingDialog(false);

            if (searchResultGroupPanel == null) {
                // We just tried to load it, so if it still null then we take it to mean that the file is
                // not there.  So we show a notice about that, before we remove this leaf from any
                // further user attempts to retrieve it.
                showWorkingDialog(false);
                JOptionPane.showMessageDialog(this,
                        "Cannot read in the search results.\n" +
                                "This search results selection will be removed.",
                        "Results not accessible", JOptionPane.WARNING_MESSAGE);

                // Nothing else can be done; the group was unavailable for some reason.
                closeGroup(); // This just removes the tree node, but it will reappear the next time
                // the archive is viewed, since the archive options are not updated or preserved.
            } else {
                rightPane.setViewportView(searchResultGroupPanel.theBasePanel);
            } // end if
        } else if (theNodeString.equals("Year View")) {
            if (theYearView == null) {
                theYearView = new YearView(viewedDate);
                theYearView.setArchiveDate(theArchiveDate);
                theYearView.setTreePanel(this);
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
                theMonthView.setArchiveDate(theArchiveDate);
                theMonthView.setTreePanel(this);
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
                theAppDays.setArchiveDate(theArchiveDate);
                theAppDays.setAlteredDateListener(this);
            } else {
                theAppDays.setDate(selectedDate);
                setViewedDate(selectedDate);
            }
            rightPane.setViewportView(theAppDays.theBasePanel);
        } else if (theNodeString.equals("Month Notes")) {
            if (viewedDateGranularity == ChronoUnit.YEARS) {
                viewedDate = selectedDate;
            }
            viewedDateGranularity = ChronoUnit.MONTHS;

            if (theAppMonths == null) {
                theAppMonths = new MonthNoteGroupPanel(); // Takes current date as default initial 'choice'.
                theAppMonths.setArchiveDate(theArchiveDate);
                theAppMonths.setAlteredDateListener(this);
            } else {
                theAppMonths.setDate(viewedDate);
            }
            rightPane.setViewportView(theAppMonths.theBasePanel);
        } else if (theNodeString.equals("Year Notes")) {
            if (theAppYears == null) {
                theAppYears = new YearNoteGroupPanel();
                theAppYears.setArchiveDate(theArchiveDate);
                theAppYears.setAlteredDateListener(this);
            } else {
                viewedDateGranularity = ChronoUnit.YEARS;
                theAppYears.setDate(viewedDate);
            }
            rightPane.setViewportView(theAppYears.theBasePanel);
        } else {
            // Any other as-yet unhandled node on the tree.
            // Currently - just Week View
            JPanel jp = new JPanel(new GridBagLayout());
            jp.add(new JLabel(theNodeString));
            rightPane.setViewportView(jp);
        } // end if/else if
        //</editor-fold>

        showWorkingDialog(false); // This may have already been done, but no harm in doing it again.
    } // end treeSelectionChanged


    //-------------------------------------------------
    // Method Name:  updateAppOptions
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


    static boolean validateArchive(String archiveName) {
        // Get an Array of all the valid archive names
        String[] theNames = MemoryBank.dataAccessor.getArchiveNames();
        if(theNames == null) return false;

        // Convert the String Array to List
        List<String> list = Arrays.asList(theNames);

        // And check to see if the input param is among them.
        return list.contains(archiveName);
    }

} // end ArchiveTreePanel class

