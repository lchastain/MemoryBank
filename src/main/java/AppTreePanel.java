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
import java.util.Vector;


public class AppTreePanel extends JPanel implements TreeSelectionListener {
    static final long serialVersionUID = 1L; // JPanel wants this but we will not serialize.
    static AppTreePanel ltTheTree;
    static AppMenuBar amb;

    private static Logger log = LoggerFactory.getLogger(AppTreePanel.class);
    private static final int LIST_GONE = -3; // used in constr, createTree

    private Notifier optionPane;
    //-------------------------------------------------------------------

    private JTree tree;
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
    private DayNoteGroup theAppDays;
    private MonthNoteGroup theAppMonths;
    private YearNoteGroup theAppYears;
    private MonthView theMonthView;
    private YearView theYearView;
    private EventNoteGroup theEvents;
    private Vector<NoteData> noteDataVector;   // For searching
    private Vector<NoteData> foundDataVector;  // Search results
    private NoteGroupKeeper theTodoListKeeper;  // keeper of all loaded 'to do' lists.
    private NoteGroupKeeper theSearchResultsKeeper;  // keeper of all loaded SearchResults.
    private SearchPanel spTheSearchPanel;
    private JPanel aboutPanel;
    private JSplitPane splitPane;

    private LocalDate currentDateChoice;
    private String theLastTreeSelection;
    private DefaultMutableTreeNode theRootNode;

    // Predefined Tree Paths to 'leaf' nodes.
    private TreePath dayNotesPath;
    private TreePath monthNotesPath;
    private TreePath yearNotesPath;
    private TreePath monthViewPath;
    private TreePath weekViewPath;
    private TreePath upcomingEventsPath;

    private AppOptions appOpts;

    private boolean blnRestoringSelection;

    public AppTreePanel(JFrame aFrame, AppOptions appOpts) {
        super(new GridLayout(1, 0));
        amb = new AppMenuBar();
        aFrame.setJMenuBar(amb);
        ltTheTree = this;
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

        currentDateChoice = LocalDate.now();

        // Restore the last selection.
        MemoryBank.update("Restoring the previous selection");
        blnRestoringSelection = true;
        if (appOpts.theSelectionRow >= 0)
            // If there is a 'good' value stored as the previous selection.
            // But the value COULD be higher than any 'legal' value - still need to handle that (or explain why it's ok)
            tree.setSelectionRow(appOpts.theSelectionRow);
        else {
            // There are a few cases where there would be no previous selection.
            // Ex: Showing the About graphic, First-time-ever for this user, lost or corrupted AppOptions.
            // In these cases leave as-is and the view will go to the About graphic.

            // One other case, if the previous selection was a todo list that is no longer there.
            // In this case the tree has been recreated without it, so go to the todo branch.
            if (appOpts.theSelectionRow == LIST_GONE) {
                appOpts.theSelectionRow = tree.getRowForPath(todolistsPath);
                tree.setSelectionRow(appOpts.theSelectionRow);
            } // end if
        } // end if

        blnRestoringSelection = false;

        TreePath tp = tree.getSelectionPath();
        if (tp != null) {
            theLastTreeSelection = tp.getLastPathComponent().toString();
        }
    } // end constructor for AppTreePanel


    // Adds a search result branch to the tree.
    private void addSearchResult(String searchResultName) {
        // Remove the tree selection listener while we
        //   rebuild this portion of the tree.
        tree.removeTreeSelectionListener(this);

        // Make a new tree node for the result file whose path
        //   is given in the input parameter
        DefaultMutableTreeNode tmpNode;
        tmpNode = new DefaultMutableTreeNode(searchResultName, false);

        // Add to the tree under the Search Results branch
        DefaultMutableTreeNode nodeSearchResults = SearchBranchHelper.getSearchResultsNode(theRootNode);
        nodeSearchResults.add(tmpNode);
        treeModel.nodeStructureChanged(nodeSearchResults);

        // Select the new list.
        TreeNode[] pathToRoot = tmpNode.getPath();
        tree.addTreeSelectionListener(this);
        tree.setSelectionPath(new TreePath(pathToRoot));
    } // end addSearchResult


    //-------------------------------------------------------
    // Method Name: closeGroup
    //
    // Removes the associated results file and then calls the
    //   method to remove the search result leaf.
    //-------------------------------------------------------
    void closeGroup() {
        // Obtain a reference to the current tree selection.
        // Since the App menu provides the only 'legal' entry to this
        // method by only showing the 'Close' option when a closeable
        // group is showing, it follows that the current tree selection
        // is the group that we want to close.
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
        if (node == null) return; // But this is here just in case someone got here another way.
        assert node.getChildCount() == 0; // another fail-safe; should never happen.

        // Remove these results from the tree but do not let the removal result in the need to
        // process another tree selection event.
        tree.removeTreeSelectionListener(this);
        DefaultMutableTreeNode theParent = (DefaultMutableTreeNode) node.getParent();
        node.removeFromParent();
        treeModel.nodeStructureChanged(theParent);
        tree.addTreeSelectionListener(this);

        // Do not attempt to save this group prior to changing the tree selection.
        theNoteGroup = null;

        // Select the parent branch
        TreeNode[] pathToRoot = theParent.getPath();
        tree.setSelectionPath(new TreePath(pathToRoot));
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
        leaf = new DefaultMutableTreeNode("Upcoming Events");
        trunk.add(leaf);
        pathToRoot = leaf.getPath();
        upcomingEventsPath = new TreePath(pathToRoot);

        leaf = new DefaultMutableTreeNode("Goals");
        trunk.add(leaf);

        //---------------------------------------------------
        // Graphical views
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
        // NoteGroup types
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
        branch = new DefaultMutableTreeNode("To Do Lists");
        trunk.add(branch);
        pathToRoot = branch.getPath();
        todolistsPath = new TreePath(pathToRoot);

        String theName;
        theTodoListKeeper = new NoteGroupKeeper();
        theSearchResultsKeeper = new NoteGroupKeeper();

        for (String s : appOpts.todoLists) {
            // First check to see that the file is 'here'.
            theName = MemoryBank.userDataHome + File.separatorChar + "TodoLists" + File.separatorChar + "todo_" + s + ".json";
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

        // Create the SearchResults node  (no longer a maybe - should always happen, now)
        DefaultMutableTreeNode nodeSearchResults = new DefaultMutableTreeNode("Search Results", true);
        trunk.add(nodeSearchResults);
        pathToRoot = nodeSearchResults.getPath();
        searchresultsPath = new TreePath(pathToRoot);

        // Restore previous search results, if any.
        if (!appOpts.searchResultList.isEmpty()) {
            for (int i = 0; i < appOpts.searchResultList.size(); i++) {
                String searchResultFilename = appOpts.searchResultList.get(i);
                nodeSearchResults.add(new DefaultMutableTreeNode(searchResultFilename, false));
            }
        } // end if

        // Create a default model based on the 'App' node that
        //   we've been growing, and create the tree from that model.
        treeModel = new DefaultTreeModel(trunk);
        tree = new JTree(treeModel);

        // At creation, all paths are collapsed.
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
    @SuppressWarnings("rawtypes")
    static DefaultMutableTreeNode deepClone(DefaultMutableTreeNode root) {
        DefaultMutableTreeNode newRoot = (DefaultMutableTreeNode) root.clone();
        for (Enumeration childEnum = root.children(); childEnum.hasMoreElements(); ) {
            newRoot.add(deepClone((DefaultMutableTreeNode) childEnum.nextElement()));
        }
        return newRoot;
    }

    // Used by Test
    NoteGroup getTheNoteGroup() {
        return theNoteGroup;
    }

    public JTree getTree() {
        return tree;
    }

    private void handleMenuBar(String what) {
        if (what.equals("Exit")) System.exit(0);
        else if (what.equals("About")) showAbout();
        else if (what.equals("Add New List...")) TodoBranchHelper.addNewList(tree);
        else if (what.equals("Close")) closeGroup();
        else if (what.equals("Clear Day")) theAppDays.clearGroup();
        else if (what.equals("Clear Month")) theAppMonths.clearGroup();
        else if (what.equals("Clear Year")) theAppYears.clearGroup();
        else if (what.equals("Clear Entire List")) theNoteGroup.clearGroup();
        else if (what.equals("Contents")) showHelp();
        else if (what.equals("Search...")) doSearch();
        else if (what.equals("Set Options...")) ((TodoNoteGroup) theNoteGroup).setOptions();
        else if (what.startsWith("Merge")) ((TodoNoteGroup) theNoteGroup).merge();
            //else if (what.startsWith("Print")) ((TodoNoteGroup) theNoteGroup).printList();
        else if (what.equals("Refresh")) theNoteGroup.refresh();
        else if (what.equals("Review...")) System.out.println("Review was selected.");
        else if (what.startsWith("Save As")) saveTodoListAs();
        else if (what.equals("Today")) showToday();
        else if (what.equals("undo")) {
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
        if (appOpts.viewsExpanded) tree.expandPath(viewsPath);
        if (appOpts.notesExpanded) tree.expandPath(notesPath);
        if (appOpts.todoListsExpanded) tree.expandPath(todolistsPath);
        if (appOpts.searchesExpanded) tree.expandPath(searchresultsPath);

    } // end resetTreeState


    //------------------------------------------------------------------------
    // Method Name:  saveTodoListAs
    //
    //------------------------------------------------------------------------
    private void saveTodoListAs() {
        String oldName = theNoteGroup.getName();
        if (((TodoNoteGroup) theNoteGroup).saveAs()) {
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
            // does the leaf name change, but the reload also changes the displayed
            // list title.
            TodoBranchHelper.renameTodoListLeaf(oldName, newName);
        }
    } // end saveTodoListAs

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
                tree.getLastSelectedPathComponent();

        // Examine the rightPane to see if the About graphic is already shown, or not.
        // There are simpler ways (such as aboutPanel.isShowing(), but that does not
        // work with the toggle test.
        JViewport viewport = rightPane.getViewport();
        JComponent theContent = (JComponent) viewport.getView();

        if (node == null && aboutPanel == theContent) { // This means we can toggle back to a previous tree selection.
            // Reset tree to the state it was in before.
            blnRestoringSelection = true;
            if (appOpts.viewsExpanded) tree.expandPath(viewsPath);
            else tree.collapsePath(viewsPath);
            if (appOpts.notesExpanded) tree.expandPath(notesPath);
            else tree.collapsePath(notesPath);
            if (appOpts.todoListsExpanded) tree.expandPath(todolistsPath);
            else tree.collapsePath(todolistsPath);
            tree.setSelectionRow(appOpts.theSelectionRow);
        } else {
            // Capture the current state; we may have to 'toggle' back to it.
            updateTreeState(false);
            tree.clearSelection();

            // Show it.
            rightPane.setViewportView(aboutPanel);

            amb.manageMenus("About"); // This will get the default / unhandled case.
        } // end if
    } // end showAbout


    // Called from YearView or MonthView, mouse dbl-click on date.
    void showDay() {
        MemoryBank.debug("showDay called.");
        tree.setSelectionPath(dayNotesPath);
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
            prettyName = prettyName.substring(5); // The prettifier still leaves the 'todo_' prefix

            if (!(srd.getFileFoundIn()).exists()) {
                String s;
                s = "Error in loading " + prettyName + " !\n";
                s += "The original list no longer exists.";
                optionPane.showMessageDialog(this, s, "Error", JOptionPane.ERROR_MESSAGE);
            } else {
                // The beauty of this action is that the path we are
                // selecting does not actually have to be present
                // for this to work and display the list.
                tree.setSelectionPath(TodoBranchHelper.getTodoPathFor(tree, prettyName));
            } // end if
        } else if (fname.equals("UpcomingEvents")) {
            tree.setSelectionPath(upcomingEventsPath);
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


    private void showHelp() {
        try {
            String s = MemoryBank.logHome + File.separatorChar;
            Runtime.getRuntime().exec("hh " + s + "MemoryBank.chm");
        } catch (IOException ioe) {
            // hh "badFile" - does NOT throw an exception, but puts up a 'cant find file' window/message.
            MemoryBank.debug(ioe.getMessage());
        } // end try/catch
    } // end showHelp


    void showMonth() {
        MemoryBank.debug("showMonth called.");
        // This method is called from an external context.
        tree.setSelectionPath(monthViewPath);
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
    // already today, no apparent change is made but it
    // could be made into a toggle between the date-based
    // view and the textual panel as described above, like
    // the 'showAbout' behavior.
    //--------------------------------------------------------
    void showToday() {
        // Make sure that the most recent changes, if any, are preserved.
        preClose();

        // Get the current tree selection
        TreePath tp = tree.getSelectionPath();

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

        // Clear the current tree selection, so they can select it again
        // and get back to a 'normal' view.
        tree.clearSelection();

    } // end showToday

    void showWeek() {
        MemoryBank.debug("showWeek called.");
        // This method is called from external contexts such as MonthViewCanvas and YearViewCanvas.
        // There IS not actually a view to show, here.  The rightPane is
        // just loaded with the text, 'Week View'.  Therefore when this node is selected directly
        // on the tree, it does not come here but just shows the text of the request that it does
        // not know how to handle.
        tree.setSelectionPath(weekViewPath);
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

    private void treeSelectionChanged(TreePath tp) {
        String strSelectionType; // used in menu management

        // Obtain a reference to the new selection.
        DefaultMutableTreeNode node = (DefaultMutableTreeNode)
                tree.getLastSelectedPathComponent();
        if (node == null) return;

        // We have started to handle the user's request; now
        //   disallow further input until we're finished.
        dlgWorkingDialog.setLocationRelativeTo(rightPane); // Re-center before showing.
        if (!blnRestoringSelection) showWorkingDialog(true);

        // Update the current selection row
        appOpts.theSelectionRow = tree.getMaxSelectionRow();

        if (theNoteGroup != null) {
            theNoteGroup.preClose();
        } // end if

        // Update the currentDateChoice so that it can be used to set the
        //   date to be shown before we display the newly selected group.
        if (tp != null) {
            theLastTreeSelection = tp.getLastPathComponent().toString();
            MemoryBank.debug("Last Selection was: " + theLastTreeSelection);
            updateCurrentDateChoice();
        } // end if

        String theNodeString = node.toString();
        String theParent = node.getParent().toString();
        MemoryBank.debug("New tree selection: " + theNodeString);
        appOpts.theSelection = theNodeString; // Preserved exactly, for app restart.
        strSelectionType = theNodeString;  // Default value; may change, below.

        //-----------------------------------------------------
        // These booleans will help us to avoid going down
        //   the wrong branch in a case where some bozo named
        //   their To Do list - 'To Do Lists'.  Other cases
        //   where a list may be named 'Year View' or any other
        //   name that matches an existing tree branch name,
        //   are caught by the fact that we first
        //   look to see if the parent is an expandable branch,
        //   before we start considering the text of the selection.
        //-----------------------------------------------------
        boolean isTodoBranch = theNodeString.equals("To Do Lists");
        boolean isSearchBranch = theNodeString.equals("Search Results");
        boolean isTopLevel = theParent.equals("App");

        theNoteGroup = null; // initialize

        //<editor-fold desc="Actions Depending on the selection">
        if (isTodoBranch && isTopLevel) {  // Edit the Todo parent branch
            // To Do List management - select, deselect, rename, reorder, remove
            // The 'tree' may change often.  We instantiate a new helper
            // and editor each time, to be sure all are in sync.
            TodoBranchHelper tbh = new TodoBranchHelper(tree, theTodoListKeeper);
            TreeBranchEditor tbe = new TreeBranchEditor(node, tbh);
            strSelectionType = "To Do Lists Branch Editor";
            rightPane.setViewportView(tbe);
        } else if (isSearchBranch && isTopLevel) {  // Edit the Search parent branch
            SearchBranchHelper sbh = new SearchBranchHelper(tree, theSearchResultsKeeper);
            TreeBranchEditor tbe = new TreeBranchEditor(node, sbh);
            strSelectionType = "Search Results Branch Editor";
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
            TodoNoteGroup todoNoteGroup;

            // If the list has been previously loaded during this session,
            // retrieve it from the list keeper.
            todoNoteGroup = (TodoNoteGroup) theTodoListKeeper.get(theNodeString);

            // Otherwise, prepare to load it.
            if (todoNoteGroup == null) {
                log.debug("Loading " + theNodeString + " from filesystem");
                todoNoteGroup = new TodoNoteGroup(theNodeString);
                theTodoListKeeper.add(todoNoteGroup);
            } else {
                log.debug("Retrieved " + theNodeString + " from the keeper");
            }

            theNoteGroup = todoNoteGroup;
            rightPane.setViewportView(theNoteGroup);
        } else if (theParent.equals("Search Results")) {
            // Selection of a Search Result List
            strSelectionType = "Search Result";  // For manageMenus
            SearchResultGroup searchResultGroup;

            // If the search has been previously loaded during this session,
            // we can retrieve the group from the keeper.
            searchResultGroup = (SearchResultGroup) theSearchResultsKeeper.get(theNodeString);

            // Otherwise construct it, but only if a file for it already exists.  This is NOT the
            // kind of group where if a file does not exist for it, we would go ahead and make one.
            if (searchResultGroup == null) {
                searchResultGroup = SearchResultGroup.getGroup(theNodeString);
                if(searchResultGroup != null) {
                    log.debug("Loaded " + theNodeString + " from filesystem");
                    theSearchResultsKeeper.add(searchResultGroup);
                }
            } else {
                log.debug("Retrieved '" + theNodeString + "' from the keeper");
            }

            if (searchResultGroup == null) {
                // We just tried to retrieve it or to load it, so if it is STILL
                //   null then we take it to mean that the file is not there.

                // We can show a notice about what went wrong and what we're
                // going to do about it, but that will only be helpful if
                // the user had just asked to see the search results, and NOT
                // in the case where this situation arose during a program
                // restart where the missing search results just happen to be
                // the last selection that had been made during a previous run,
                // and now it is being restored, possibly several days later.
                if (!blnRestoringSelection) {
                    // The user just now clicked on this node
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
            rightPane.setViewportView(theMonthView);
        } else if (theNodeString.equals("Day Notes")) {
            if (theAppDays == null) {
                theAppDays = new DayNoteGroup();
            }
            theNoteGroup = theAppDays;
            theAppDays.setChoice(currentDateChoice);
            rightPane.setViewportView(theAppDays);
        } else if (theNodeString.equals("Month Notes")) {
            if (theAppMonths == null) {
                theAppMonths = new MonthNoteGroup();
            }
            theNoteGroup = theAppMonths;
            theAppMonths.setChoice(currentDateChoice);
            rightPane.setViewportView(theAppMonths);
        } else if (theNodeString.equals("Year Notes")) {
            if (theAppYears == null) {
                theAppYears = new YearNoteGroup();
            }
            theNoteGroup = theAppYears;
            theAppYears.setChoice(currentDateChoice);
            rightPane.setViewportView(theAppYears);
        } else if (theNodeString.equals("Upcoming Events")) {
            if (theEvents == null) {
                theEvents = new EventNoteGroup();
            }
            theNoteGroup = theEvents;
            rightPane.setViewportView(theEvents);
        } else {
            // Any other as-yet unhandled node on the tree.
            // Currently - Week View, Icon Manager
            JPanel jp = new JPanel(new GridBagLayout());
            jp.add(new JLabel(theNodeString));
            rightPane.setViewportView(jp);
        } // end if/else if
        //</editor-fold>

        amb.manageMenus(strSelectionType);
        showWorkingDialog(false);
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
        appOpts.viewsExpanded = tree.isExpanded(viewsPath);
        appOpts.notesExpanded = tree.isExpanded(notesPath);
        appOpts.todoListsExpanded = tree.isExpanded(todolistsPath);
        appOpts.searchesExpanded = tree.isExpanded(searchresultsPath);

        //System.out.println("Divider Location: " + splitPane.getDividerLocation());
        appOpts.paneSeparator = splitPane.getDividerLocation();

        appOpts.theSelectionRow = tree.getMaxSelectionRow();
        // Current selection text was captured when the last selection
        //    was made, but the row may have changed due to expansion
        //    or collapsing of nodes above the selection.

        if (!updateLists) return;

        // Preserve the names of the active To Do Lists in the AppOptions.
        DefaultMutableTreeNode theTodoNode = TodoBranchHelper.getTodoNode(theRootNode);
        DefaultMutableTreeNode leafLink;
        int numLists;
        appOpts.todoLists.clear();

        numLists = theTodoNode.getChildCount();
        if (numLists > 0) {
            leafLink = theTodoNode.getFirstLeaf();
            while (numLists-- > 0) {
                String s = leafLink.toString();
                //MemoryBank.debug("  Preserving list: " + s);
                appOpts.todoLists.addElement(s);
                leafLink = leafLink.getNextLeaf();
            } // end while
        } // end if

        // Preserve the names of the active Search Results in the AppOpts.
        DefaultMutableTreeNode theSearchNode = SearchBranchHelper.getSearchResultsNode(theRootNode);
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

        final TreePath tp = e.getOldLeadSelectionPath();
        if (blnRestoringSelection) {
            // We don't need to handle this event from a separate
            //   thread because we don't need the 'working' dialog
            //   when restoring a previous selection because the
            //   corresponding file, if any, would have already
            //   been accessed and loaded.  Although there is one
            //   exception to that, at program restart but in that
            //   case we have the splash screen and main progress bar.
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

} // end AppTreePanel class

