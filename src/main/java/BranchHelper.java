/*
 An implementation of BranchHelperInterface, in support of the TreeBranchEditor actions on
 the Tree Branches.
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;

@SuppressWarnings("rawtypes")
public class BranchHelper implements BranchHelperInterface {
    private static final Logger log = LoggerFactory.getLogger(BranchHelper.class);

    private final JTree theTree;  // The original tree, not the one from the editor.
    private final NoteGroupPanelKeeper theNoteGroupPanelKeeper;
    private final DefaultTreeModel theTreeModel;
    final DefaultMutableTreeNode theRoot;
    private int theIndex;  // keeps track of which row of the tree we're on.
    private String renameFrom;  // Used when deciding if special handling is needed.
    private String renameTo;    // Provides a way for us override the value.
    private GroupType theType;
    Notifier optionPane;  // for Testing
    private String thePrefix; // goal_, event_, todo_, search_
    String theAreaNodeName; // Goals, Events, To Do Lists, Search Results

    BranchHelper(JTree jt, NoteGroupPanelKeeper noteGroupPanelKeeper, DataArea areaName) {
        theTree = jt;
        theNoteGroupPanelKeeper = noteGroupPanelKeeper;
        theTreeModel = (DefaultTreeModel) theTree.getModel();
        theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        theAreaNodeName = areaName.toString();

        // This Helper is for one of these Branches -
        if (areaName.equals(DataArea.GOALS)) {
            thePrefix = "goal_";
            theType = GroupType.GOALS;
        } else if (areaName.equals(DataArea.UPCOMING_EVENTS)) {
            thePrefix = "event_";
            theType = GroupType.EVENTS;
        } else if (areaName.equals(DataArea.TODO_LISTS)) {
            thePrefix = "todo_";
            theType = GroupType.TODO_LIST;
        } else if(areaName.equals(DataArea.SEARCH_RESULTS)) {
            thePrefix = "search_";
            theType = GroupType.SEARCH_RESULTS;
        }
        assert thePrefix != null; // Doing it this way vs an 'else' section, we get full test coverage.

        optionPane = new Notifier() {}; // Uses all default methods.

        // Get the index of the tree node we're 'helping' (not the same as row number)
        theIndex = -1;
        DefaultMutableTreeNode dmtn = BranchHelperInterface.getNodeByName(theRoot, theAreaNodeName);
        if (dmtn != null) theIndex = theRoot.getIndex(dmtn);
    }

    @Override
    public boolean allowRenameFrom(DefaultMutableTreeNode theNode) {
        String theName = theNode.toString();
        if (theNode.getAllowsChildren()) {
            optionPane.showMessageDialog(new JFrame(), "You are not allowed to rename the tree branch: " + theName);
            return false;
        }
        renameFrom = theName.trim(); // Used in renameTo; trim is ok but don't mess with case.
        return true;
    }

    // This method is called from Tree operations and occurs before any rename is attempted.
    // So if it passes at that time, the subsequent rename operation does not need to call here again.
    @Override
    public boolean allowRenameTo(String theName) {
        renameTo = theName.trim();
        // This method is always called after a 'allowRenameFrom' call, which is where the 'renameFrom' var is set.
        GroupInfo groupInfo = new GroupInfo(renameFrom, theType);
        NoteGroup myNoteGroup = groupInfo.getNoteGroup();

        // If theName is the name we already have then the whole thing is a no-op.
        // No need to put out a complaint about that; just return a false.  But if
        // the only difference is in the casing then we will get past this check.
        if (theName.equals(groupInfo.getGroupName())) return false;

        String theComplaint = myNoteGroup.groupDataAccessor.getObjectionToName(theName);
        if (theComplaint.isEmpty()) return true;

        optionPane.showMessageDialog(AppTreePanel.theInstance.getTree(), theComplaint,
                "Error", JOptionPane.ERROR_MESSAGE);

        return false;
    } // end allowRenameTo

    @Override
    public boolean deleteAllowed(String theSelection) {
        // This method previously had only one usage; disallowing the removal of the Event Consolidated List.
        // However, since then I removed that list myself, permananently.  But this mechanism of potentially disabling
        // the ability to remove a selection - seems highly likely to be needed again, so leaving it as a
        // placeholder / example.
//        if(AREA_EVENT.equals(theAreaNodeName)) {
//            return !theSelection.equals(MemoryBank.appOpts.consolidatedEventsViewName);
//        }
        return true;
    }

    // This method is the handler for the 'Apply' button of the TreeBranchEditor.
    // For tree structure events, we don't address them individually but just accept
    //   them as a whole, by directly adopting the 'mtn' parameter as our new branch,
    //   regardless of whether leaves have been moved around or not.
    // That still leaves individual 'delete' and 'rename' actions to be handled here.
    // Note that code remains to handle a 'delete', but we no longer provide a path
    //   to do that from the editor.
    // For 'delete' and 'rename' actions, we need to examine the 'changes' list.  If for some
    //   reason we cannot perform all the tasks from the list, the final branch may no longer
    //   accurately represent the true state of the group data stores.  The user will be
    //   informed of issues but processing will continue, leaving the user to manually fix
    //   any mismatch between the branch and the data store.
    @Override
    public boolean doApply(MutableTreeNode mtn, ArrayList<NodeChange> changes) {
        // 'theIndex' is the location of the branch that we will replace.  It is set
        // in the constructor here and it is NOT the same as the row of the tree so
        // it is not error-prone due to changes such as collapse/expand events or a
        // new NoteGroup appearing above it.  The line below is a 'just in case'.
        if (theIndex == -1) return false;

        // Handle leaf renamings and deletions
        String deleteWarning = null;
        boolean doDelete = false;
        ems.setLength(0);
        for (NodeChange nodeChange : changes) {
            MemoryBank.debug(nodeChange.toString());
            GroupInfo groupInfo = new GroupInfo(nodeChange.nodeName, theType);
            if (nodeChange.changeType == NodeChange.RENAMED) {

                // Checking special cases where there is no corresponding data store -
                // We no longer have such a case in any area, but we did at one time so keep this condition
                //   until it is absolutely certain that it will not occur again.  'blarg123!' is just a placeholder.
                if(theAreaNodeName.equals("blarg123!")) {
                    if (nodeChange.nodeName.equals("a node with no associated data store")) {
                        // An exception to the norm - this one has no corresponding data store.
                        // This situation can occur because there is no way to disallow the rename attempt for one
                        //   leaf while still allowing renames to others.  So we arrive here with a rename directive
                        //   that we can just ignore, or take some other action.
                        // < now do whatever change is needed instead of the rename >
                        continue;
                    }
                }

                // Now attempt the rename
                NoteGroup myNoteGroup = groupInfo.getNoteGroup();
                try {
                    myNoteGroup.renameNoteGroup(nodeChange.renamedTo);
                    theNoteGroupPanelKeeper.remove(nodeChange.nodeName);
                } catch (Exception se) {
                    ems.append(se.getMessage()).append(System.lineSeparator());
                } // end try/catch
            } else if (nodeChange.changeType == NodeChange.REMOVED) {
                if (deleteWarning == null) {
                    deleteWarning = "Deletions of " + theAreaNodeName + " cannot be undone.";
                    deleteWarning += System.lineSeparator() + "Are you sure?";

                    doDelete = optionPane.showConfirmDialog(theTree, deleteWarning,
                            "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                }

                if (!doDelete) continue;
                // The end result of a 'No' to the delete confirmation will be that the leaf or leaves will have
                // already been removed from the branch but they will still be available for reselection
                // in the list of choices during future branch edit sessions.

                // Delete the group -
                MemoryBank.dataAccessor.getNoteGroupDataAccessor(groupInfo).deleteNoteGroupData();
                theNoteGroupPanelKeeper.remove(nodeChange.nodeName);
            }
        }  // end for each change

        // Show the Exception(s), if any.
        if (!ems.toString().equals("")) {
            optionPane.showMessageDialog(theTree, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
            if(changes.size() == 1) return false; // If there was only one change request, we can bail out right now.
        } // end if

        // Accept all Branch structure changes.
        // We saved this for last, in case the error message above kicked in and the user would want
        // to compare the original branch with the one shown in the editor before we make changes.
        theRoot.remove(theIndex);
        theRoot.insert(mtn, theIndex); // Goes back to the same place.
        theTreeModel.nodeStructureChanged(mtn); // Localized; the node does not 'collapse'.

        // We do this last step because now that the edits have been accepted, we do not want both
        // the 'official' branch and the 'editor' branch to be shown at the same time, identical
        // to each other yet with a still-active 'Cancel' button that it is too late to use since
        // all the edits had already been applied.  This way, the user gets a more final indication
        // that the editing session is completed.
        // If they do want to go back and see the branch editor again it is a simple click away
        // for them but by having them do that, they reset the editor to the new official branch
        // and choices as the starting point, and 'Cancel' would have no effect until they have
        // made more changes.
        if(null != AppTreePanel.theInstance) { // It may be null if we got here from a Test.
            AppTreePanel.theInstance.getAppMenuBar().showRestoreOption(false);
            // IF an 'undo deletion' menu option was being shown and was NOT used, then the
            // action of the user clicking on the 'Apply' button as they have done here needs
            // to take away that menu option.  That will happen when the menus are re-managed
            // upon showing the About panel, and it will also happen with any other Tree
            // selection now that the call to showRestoreOption() has set the flag to false.
            AppTreePanel.theInstance.showAbout();
        }
        return true;
    }  // end doApply


    @Override
    public ArrayList getChoices() {
        return MemoryBank.dataAccessor.getGroupNames(theType, false);
    }

    @Override
    public String getRenameToString() {
        return renameTo;
    }

} // end class BranchHelper
