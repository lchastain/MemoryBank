import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// This panel has a BorderLayout, where the NORTH component provides info about the source of the link
// and the CENTER component is a JSplitPane with highly variable content.  The left side of the split
// has a JTree with selectable nodes (in a JScrollPane) and the right side is used to show the effects
// of the selections.  Panel/layout nesting is done where needed in order to achieve the desired visual
// effects, but at the cost of clarity, at times.  Comments are there to help with that.

public class LinkTargetSelectionPanel extends JPanel implements TreeSelectionListener, NoteSelection {
    private static final long serialVersionUID = 1L;
    private static final Logger log = LoggerFactory.getLogger(LinkTargetSelectionPanel.class);

    private JTree theTree;
    private final JScrollPane rightPane;
    private final AppOptions appOpts;
    private String baseTargetSelectionString;  // used in building up the targetSelectionLabel
    JLabel targetSelectionLabel;
    JPanel theInfoPanel;
    JLabel infoPanelTitleLabel;
    JTextArea infoPanelTextArea;
    String chosenCategory;
    String theNodeString;
    NoteGroup selectedTargetGroup;
    CalendarNoteGroup calendarNoteGroup;
    NoteData selectedNoteData;

    public LinkTargetSelectionPanel(NoteData theFromEntity) {
        super(new BorderLayout());
        chosenCategory = "";
        appOpts = MemoryBank.appOpts;

        // Set up for selection reporting -
        baseTargetSelectionString = "Linking:&nbsp; " + AppUtil.makeRed(theFromEntity.noteString);
        baseTargetSelectionString += " &nbsp; TO: &nbsp; ";

        createTree();  // Create the tree.
        theTree.addTreeSelectionListener(this); // Listen for when the selection changes.
        theTree.setFocusable(false);  // Do not let focus escape from the right side; close off other avenues.

        // Create the scroll pane and add the tree to it.
        // This will be the left side of the JSplitPane.
        JScrollPane treeView = new JScrollPane(theTree);
        treeView.setFocusable(false);

        // Create the viewing pane.  This pane will be used to show all Tree selection results.
        // This will be the right side of the JSplitPane.
        rightPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);

        // Make the split pane and set its left and right sides to the treeView and rightPane, respectively.
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setPreferredSize(new Dimension(840, 520));
        if (appOpts.paneSeparator > 0) splitPane.setDividerLocation(appOpts.paneSeparator);
        else splitPane.setDividerLocation(140);
        splitPane.setLeftComponent(treeView);
        splitPane.setRightComponent(rightPane);

        // Declare a selection result label and add it to the top of the base panel.
        targetSelectionLabel = new JLabel();
        targetSelectionLabel.setFont(Font.decode("Dialog-bold-12"));
        add(targetSelectionLabel, BorderLayout.NORTH);
        resetTargetSelectionLabel(); // No selection, to start.

        // Declare the messaging panel Title label.
        infoPanelTitleLabel = new JLabel();
        infoPanelTitleLabel.setHorizontalAlignment(JLabel.LEFT);
        infoPanelTitleLabel.setOpaque(true);
        infoPanelTitleLabel.setFont(Font.decode("Serif-bold-20"));

        // Declare the info panel and its text area, add the title label and text area
        theInfoPanel = new JPanel(new BorderLayout());
        infoPanelTextArea = new JTextArea();
        infoPanelTextArea.setFont(Font.decode("Serif-bold-14"));
        infoPanelTextArea.setEnabled(false);
        infoPanelTextArea.setDisabledTextColor(Color.black);
        theInfoPanel.add(infoPanelTitleLabel, BorderLayout.NORTH);
        theInfoPanel.add(infoPanelTextArea, BorderLayout.CENTER);

        // Add the split pane to this panel.
        add(splitPane, BorderLayout.CENTER);

        showInfoPanel("Linking");
    }

    void createTree() {
        log.debug("Creating the tree");

        // Temporary highly-reused variables
        DefaultMutableTreeNode trunk = new DefaultMutableTreeNode("App");
        DefaultMutableTreeNode branch; // An expandable node
        DefaultMutableTreeNode leaf;   // An end-node

        // We need a right-click listener, to give us a way to deselect the tree so that we
        //    can use that occurrence as a signal to go back to showing the opening info.
        MouseAdapter deselector = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                //System.out.println(e.toString());
                int m = e.getModifiers();
                boolean rightClick = false;
                if ((m & InputEvent.BUTTON3_MASK) != 0) rightClick = true;

                if (rightClick) {
                    theTree.collapsePath(theTree.getSelectionPath());
                    theTree.clearSelection();
                }
            }
        };


        // It begins -
        branch = new DefaultMutableTreeNode("Notes");
        trunk.add(branch);
        leaf = new DefaultMutableTreeNode("Day Notes", false);
        branch.add(leaf);
        leaf = new DefaultMutableTreeNode("Month Notes", false);
        branch.add(leaf);
        leaf = new DefaultMutableTreeNode("Year Notes", false);
        branch.add(leaf);

        branch = new DefaultMutableTreeNode("Goals");
        trunk.add(branch);
        for (String s : appOpts.goalsList) {
            // Add to the tree
            leaf = new DefaultMutableTreeNode(s, false);
            log.debug("  Adding List: " + s);
            branch.add(leaf);
        } // end for

        branch = new DefaultMutableTreeNode("Upcoming Events");
        trunk.add(branch);
        for (String s : appOpts.eventsList) {
            if (s.equals("Consolidated View")) continue; // Skip this one for now.  It will go away
            // altogether, eventually, and then this line can come out.

            // Add to the tree
            leaf = new DefaultMutableTreeNode(s, false);
            log.debug("  Adding List: " + s);
            branch.add(leaf);
        } // end for

        branch = new DefaultMutableTreeNode("To Do Lists", true);
        trunk.add(branch);
        for (String s : appOpts.tasksList) {
            // Add to the tree
            branch.add(new DefaultMutableTreeNode(s, false));
        } // end for

        // Create a default model based on the 'App' node that
        //   we've been growing, and create the tree from that model.
        DefaultTreeModel treeModel = new DefaultTreeModel(trunk);
        theTree = new JTree(treeModel);

        // Set to single selection mode.
        theTree.getSelectionModel().setSelectionMode
                (TreeSelectionModel.SINGLE_TREE_SELECTION);

        // Do not show the 'App' root of the tree.
        theTree.setRootVisible(false);

        // But do show the link that all children have to it.
        theTree.setShowsRootHandles(true);

        // Add a branch expansion listener, to select the branch that was expanded.
        //   (and clear the selection when it gets collapsed)
        TreeExpansionListener treeExpansionListener = new TreeExpansionListener() {
            @Override
            public void treeExpanded(TreeExpansionEvent event) {
                //System.out.println(event.toString());
                TreePath treePath = event.getPath();
                theTree.setSelectionPath(treePath);
            }

            @Override
            public void treeCollapsed(TreeExpansionEvent event) {
                theTree.clearSelection();
//                showInfoPanel("Linking");
            }

        };
        theTree.addTreeExpansionListener(treeExpansionListener);
        theTree.addMouseListener(deselector);
    } // end createTree


    // This is called by NoteComponent; implementation of the NoteSelection interface.
    @Override
    public void noteSelected(NoteData noteData) {
        selectedNoteData = noteData;
        resetTargetSelectionLabel();
    }

    private void resetTargetSelectionLabel() {
        String selectionText;
        String groupName = "";
        String targetNoteString = "";

        if (selectedTargetGroup != null) {
            groupName = selectedTargetGroup.getName();

            if (selectedNoteData != null) {
                targetNoteString = AppUtil.makeRed(selectedNoteData.noteString);
            }
        }

        if (chosenCategory.isEmpty()) {
            selectionText = baseTargetSelectionString + "&nbsp; (no category yet)";
        } else if (groupName.isEmpty()) {
            selectionText = baseTargetSelectionString + chosenCategory + ":&nbsp; (no selection yet)";
        } else if (targetNoteString.isEmpty()) {
            selectionText = baseTargetSelectionString + chosenCategory + ":&nbsp; " + AppUtil.makeRed(theNodeString);
        } else {
            selectionText = baseTargetSelectionString + chosenCategory + ":&nbsp; " + AppUtil.makeRed(theNodeString + " - ") + targetNoteString;
        }
        targetSelectionLabel.setText(AppUtil.makeHtml(selectionText));
    }

    private void showInfoPanel(String infoTitle) {
        if(infoTitle.endsWith("s")) chosenCategory = infoTitle.substring(0, infoTitle.length()-1); // prettyprinting
        String theMessage = "No info"; // Not used; just required initialization.
        infoPanelTitleLabel.setBackground(Color.cyan); // The default title background
        int theCount;

        switch (infoTitle) {
            case "Linking":
                chosenCategory = "";  // This 'value' results in a 'no category yet' report.
                infoPanelTitleLabel.setBackground(Color.pink); // The exception.
                // Text for the opening screen info message.
                theMessage = "\n This is where you can specify a new linkage.\n";
                theMessage += "\n First, on the tree to the left, select the category of the link.\n";
                theMessage += "   Each category will give further selection options.\n";
                theMessage += "   Right-click to deselect a category.\n";
                theMessage += "\n After a new link is created you will be able to further describe it \n";
                theMessage += "   by choosing the most appropriate relationship type from the dropdown control\n";
                theMessage += "   for the new line that will appear when you return to the linkages listing.\n";
                // What if this is a new user, looking at linkages when they do not yet have any notes at all?
                // Need a 'no data here yet' message, for all but Dates.
                break;
            case "Notes":
                theMessage = "\n From here you can select the type of note to be linked.\n";
                theMessage += "\n Once the note type is known, if there are notes for it then they will appear.\n";
                theMessage += "\n Make no note selection if you are linking to the overall list, only.\n";
                theMessage += "\n You can select a note by clicking on the text of the note.  A selection will have a red border.\n";
                theMessage += " If you need to clear a selection just leave the group, and then return.\n";
                theMessage += "\n If your selected type is date-based you can use the controls to set the view to a new date.";
                break;
            case "Goals":
                theCount = MemoryBank.appOpts.goalsList.size();
                theMessage = "\n From here you can select the Goal to be linked.\n";
                if (theCount == 0) {
                    theMessage += "\n Or at least you could have, if you had any Goals defined and enabled.\n";
                    theMessage += "\n Return here after defining (or re-enabling) at least one goal.\n";
                }
                break;
            case "Upcoming Events":
                theCount = MemoryBank.appOpts.eventsList.size();
                theMessage = "\n From here you can select the Event list to be linked.\n";
                if (theCount == 0) {
                    theMessage += "\n Or at least you could have, if you had any Event lists defined and enabled.\n";
                    theMessage += "\n Return here after defining (or re-enabling) one or more Event lists.\n";
                } else {
                    theMessage += "\n Once a selection is made, if there are specific events defined then an event listing will appear.\n";
                    theMessage += "\n If events are presented, you can select one.\n";
                    theMessage += "  Click on the text to see the red border.\n";
                    theMessage += "\n Make no event selection if you are linking to the entire event type.";
                    theMessage += "\n   You can clear an event selection by leaving the group, and then return.";
                }
                break;
            case "To Do Lists":
                theCount = MemoryBank.appOpts.tasksList.size();
                theMessage = "\n From here you can select the To Do List to to be linked.\n";
                if (theCount == 0) {
                    theMessage += "\n Or at least you could have, if you had any To Do lists defined and enabled.\n";
                    theMessage += "\n Return here after creating (or re-enabling) one or more To Do lists.\n";
                } else {
                    theMessage += "\n Once a selection is made, if there are specific tasks defined then a listing will appear.\n";
                    theMessage += "\n If tasks are presented, you can select one.\n";
                    theMessage += "  Click on the text to see the red border.\n";
                    theMessage += "\n Make no task selection if you are linking to the entire list.";
                    theMessage += "\n   You can clear a task selection by leaving the list, and then return.";
                }
                break;
        }

        // Show the message.
        infoPanelTitleLabel.setText("    " + infoTitle);
        infoPanelTextArea.setText(theMessage);
        rightPane.setViewportView(theInfoPanel);
        resetTargetSelectionLabel();
    }

    void treeSelectionChanged(TreePath newPath) {
        calendarNoteGroup = null;
        theNodeString = null;
        if (newPath == null) {  // When selection is cleared.
            showInfoPanel("Linking");
            return;
        }

        // Obtain a reference to the new selection.
        // This is better than 'theTree.getLastSelectedPathComponent()' because it works for
        //   normal tree selection events but also allows for 'phantom' selections; tree
        //   paths that were created and selected by code vs those that came from a user's
        //   mouse click event on an existing (visible and active) tree node.
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) (newPath.getLastPathComponent());
        if (node == null) return; // And this is just in case that somehow -didn't work out.

        // Get the string for the selected node.
        theNodeString = node.toString();
        log.debug("New tree selection: " + theNodeString);

        // Set up for a selection that is somehow missing its data (error handling)
        JLabel missingDataLabel = new JLabel();
        missingDataLabel.setFont(Font.decode("Serif-20"));
        missingDataLabel.setText("<html><b><center>" + theNodeString + "</center></b><br><i><center>Was Not Found!</center></i></html>");

        // Get the name of the node's parent.  Thanks to the way we have created the tree and
        // the unselectability of the tree root, we never expect the parent path to be null.
        String parentNodeName = newPath.getParentPath().getLastPathComponent().toString();
        boolean isTopLevel = parentNodeName.equals("App");

        // Clear the selection report - needed when the user has changed from a 'good'
        // selection to one that is somehow missing.
        selectedTargetGroup = null;
        selectedNoteData = null;
        resetTargetSelectionLabel();

        JPanel jp;  // This panel is used by the top-level selections.
        //   When used, put all content here and show it in the Viewport.

        //<editor-fold desc="Actions Depending on the selection">
        if (isTopLevel) { // This is for the expandable branches.
            theTree.expandPath(newPath);
            resetTargetSelectionLabel();

            // Show a message that is appropriate for the specific branch that is selected.
            showInfoPanel(theNodeString);
        } else if (parentNodeName.equals("Notes")) { // Selection of a Note type
            chosenCategory = "Note";

            // Static flags - must be set before the group is instantiated.
            MemoryBank.readOnly = true;
            NoteComponent.isEditable = false;

            switch (theNodeString) {
                case "Day Notes":
                    calendarNoteGroup = new DayNoteGroup();
                    break;
                case "Month Notes":
                    calendarNoteGroup = new MonthNoteGroup();
                    break;
                case "Year Notes":
                    calendarNoteGroup = new YearNoteGroup();
                    break;
            }
            // Reset the static flags
            NoteComponent.isEditable = true;
            MemoryBank.readOnly = false;

            if (calendarNoteGroup != null) {
                calendarNoteGroup.setSelectionMonitor(this);
                selectedTargetGroup = calendarNoteGroup;
                resetTargetSelectionLabel();
                rightPane.setViewportView(calendarNoteGroup.theBasePanel);
            } else {
                jp = new JPanel(new GridBagLayout());
                jp.add(new JLabel(theNodeString));
                rightPane.setViewportView(jp);
            }

        } else if (parentNodeName.equals("Goals")) { // Selection of a Goal
            chosenCategory = "Goal";

            // Static flags - must be set before the group is instantiated, and reset afterwards.
            MemoryBank.readOnly = true;
            NoteComponent.isEditable = false;
            GoalGroup goalGroup = (GoalGroup) NoteGroupFactory.getGroup(parentNodeName, theNodeString);
            NoteComponent.isEditable = true;
            MemoryBank.readOnly = false;

            if (goalGroup == null) {
                // We just tried to retrieve it or to load it, so if it is STILL null then we
                //   take it to mean that the file is effectively not there.
                // So show a 'not found' message.  But do not try to fix the problem from here;
                //   let the BranchEditors do that (hopefully behind the scenes); just leave it
                //   alone for now and let the user try something else.
                jp = new JPanel(new GridBagLayout()); // This centers the missingDataLabel; otherwise it left-justifies.
                jp.add(missingDataLabel);
                rightPane.setViewportView(jp);
            } else {
                goalGroup.setSelectionMonitor(this);
                selectedTargetGroup = goalGroup;
//                selectedTargetGroupProperties = goalGroup.getGroupProperties();
                resetTargetSelectionLabel();
                rightPane.setViewportView(goalGroup.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("Upcoming Events")) { // Selection of an Event group
            chosenCategory = "Upcoming Event";

            // Static flags - must be set before the group is instantiated, and reset afterwards.
            MemoryBank.readOnly = true;
            NoteComponent.isEditable = false;
            EventNoteGroup eventNoteGroup = (EventNoteGroup) NoteGroupFactory.getGroup(parentNodeName, theNodeString);
            NoteComponent.isEditable = true;
            MemoryBank.readOnly = false;

            if (eventNoteGroup == null) {
                // We just tried to retrieve it or to load it, so if it is STILL null then we
                //   take it to mean that the file is effectively not there.
                // So show a 'not found' message.  But do not try to fix the problem from here;
                //   let the BranchEditors do that (hopefully behind the scenes); just leave it
                //   alone for now and let the user try something else.
                jp = new JPanel(new GridBagLayout()); // This centers the missingDataLabel; otherwise it left-justifies.
                jp.add(missingDataLabel);
                rightPane.setViewportView(jp);
            } else {
                eventNoteGroup.setSelectionMonitor(this);
                selectedTargetGroup = eventNoteGroup;
//                selectedTargetGroupProperties = eventNoteGroup.myProperties;
                resetTargetSelectionLabel();
                rightPane.setViewportView(eventNoteGroup.theBasePanel);
            } // end if
        } else if (parentNodeName.equals("To Do Lists")) { // Selection of a To Do List
            chosenCategory = "To Do List";

            // Static flags - must be set before the group is instantiated, and reset afterwards.
            MemoryBank.readOnly = true;
            NoteComponent.isEditable = false;
            TodoNoteGroup todoNoteGroup = (TodoNoteGroup) NoteGroupFactory.getGroup(parentNodeName, theNodeString);
            NoteComponent.isEditable = true;
            MemoryBank.readOnly = false;

            if (todoNoteGroup == null) {
                // We just tried to retrieve it or to load it, so if it is STILL null
                //   then we take it to mean that the file is effectively not there.
                // So show a 'not found' message.  But do not try to remove or close a file,
                //   and no need to remove the selected leaf; just leave it alone and let
                //   the user try something else.
                jp = new JPanel(new GridBagLayout());
                jp.add(missingDataLabel);
                rightPane.setViewportView(jp);
            } else {
                todoNoteGroup.setSelectionMonitor(this);
                selectedTargetGroup = todoNoteGroup;
//                selectedTargetGroupProperties = todoNoteGroup.myProperties;
                resetTargetSelectionLabel();
                rightPane.setViewportView(todoNoteGroup.theBasePanel);
            } // end if
        } // end if/else if
        //</editor-fold>

// Need an alternative to this !!!!! - used in reverseLink creation

//        if(selectedTargetGroupProperties != null) {
//            selectedTargetGroupProperties.myNoteGroup = selectedTargetGroup;
//        }


    } // end treeSelectionChanged

    // The TreeSelectionListener method.  It does not get called unless the selection
    // event actually does represent a change from the current selection.  This
    // includes deselecting, but in that case 'newPath' is null.
    @Override
    public void valueChanged(TreeSelectionEvent e) {
        TreePath newPath = e.getNewLeadSelectionPath();
        treeSelectionChanged(newPath);
    }
}
