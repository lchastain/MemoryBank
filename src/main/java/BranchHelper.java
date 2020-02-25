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
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class BranchHelper implements BranchHelperInterface {
    private static Logger log = LoggerFactory.getLogger(BranchHelper.class);

    private JTree theTree;  // The original tree, not the one from the editor.
    private FileGroupKeeper theFileGroupKeeper;
    private DefaultTreeModel theTreeModel;
    private DefaultMutableTreeNode theRoot;
    private int theIndex;  // keeps track of which row of the tree we're on.
    private String renameFrom;  // Used when deciding if special handling is needed.
    private String renameTo;    // Provides a way for us override the value.
    private String theArea;
    private Notifier optionPane;  // for Testing
    private String thePrefix; // event_, todo_, search_
    private String theAreaNodeName; // Goals, Events, To Do Lists, Search Results
    private static final String AREA_GOAL = "Goals";
    private static final String AREA_EVENT = "Upcoming Events";
    private static final String AREA_TODO = "To Do Lists";
    private static final String AREA_SEARCH = "Search Results";

    BranchHelper(JTree jt, FileGroupKeeper fileGroupKeeper, String areaName) {
        theTree = jt;
        theFileGroupKeeper = fileGroupKeeper;
        theTreeModel = (DefaultTreeModel) theTree.getModel();
        theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        theArea = areaName;

        // This Helper is for one of these Branches -
        if (GoalGroup.areaName.equals(theArea)) {
            theAreaNodeName = AREA_GOAL;
            thePrefix = "goal_";
        } else if (EventNoteGroup.areaName.equals(theArea)) {
            theAreaNodeName = AREA_EVENT;
            thePrefix = "event_";
        } else if (TodoNoteGroup.areaName.equals(theArea)) {
            theAreaNodeName = AREA_TODO;
            thePrefix = "todo_";
        } else if(SearchResultGroup.areaName.equals(theArea)) {
            theAreaNodeName = AREA_SEARCH;
            thePrefix = "search_";
        }
        assert thePrefix != null; // Doing it this way vs an 'else' section, we get full test coverage.

        optionPane = new Notifier() {
        }; // Uses all default methods.

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

    // This is always called after a 'allowRenameFrom' call, which is where the 'renameFrom' var is set.
    @Override
    public boolean allowRenameTo(String theName) {
        renameTo = theName.trim();

        // If renameTo is also our 'renameFrom' name then the whole thing is a no-op.
        // No need to put out a complaint about that; just return a false.  But if
        // there is a difference in the casing then we will get past this check.
        if (renameTo.equals(renameFrom)) return false;

        // Now a special case, for a selection under Events, only - the Consolidated Events List Name.
        // This will be a non-intuitive way to reset it back to the default string, after the user
        // had changed it to something else and now wants to go back to how it was out-of-the-box
        // but there is no apparent 'reset' button for that.  The way to reset it will be to
        // simply clear it out; this method will see an empty string and can restore the default.
        if(renameFrom.equals(MemoryBank.appOpts.consolidatedEventsViewName)) {
            if(theAreaNodeName.equals(AREA_EVENT)) {
                if (theName.isEmpty()) {
                    renameTo = new AppOptions().consolidatedEventsViewName;
                    return true;
                }
            }
        }

        // It is important to check filename validity in the area where the new file would be created,
        // so that any possible Security Exception is seen.  Those Exceptions may not be seen in a
        // different area of the same filesystem.
        File aFile = new File(FileGroup.getFullFilename(theArea, theName));
        String theComplaint = BranchHelperInterface.checkFilename(theName, aFile.getParent());
        if (!theComplaint.isEmpty()) {
            optionPane.showMessageDialog(theTree, theComplaint,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    @Override
    public boolean deleteAllowed(String theSelection) {
        if(AREA_EVENT.equals(theAreaNodeName)) {
            return !theSelection.equals(MemoryBank.appOpts.consolidatedEventsViewName);
        }
        return true;
    }

    // This method is the handler for the 'Apply' button of the TreeBranchEditor.
    // For tree structure events, we don't address them individually but just accept
    // them as a whole, by directly adopting the 'mtn' parameter as our new branch.
    // But in addition to (possibly) having an effect on the final branch, the
    // rename and delete actions from the editor imply changes to the filesystem
    // and those directives are also handled here.  For these actions, we need to
    // examine the 'changes' list.  But what happens if we cannot perform all the
    // tasks from the list?  In that case, the branch may no longer accurately
    // represent the true state of the group data files and the user will be informed.
    @Override
    public void doApply(MutableTreeNode mtn, ArrayList<NodeChange> changes) {
        // 'theIndex' is the location of the branch that we will replace.  It is set
        // in the constructor here and it is NOT the same as the row of the tree so
        // it is not error-prone due to changes such as collapse/expand events or a
        // new NoteGroup appearing above it.  But the line below is a 'just in case'.
        if (theIndex == -1) return;

        // Handle file renamings and deletions
        String deleteWarning = null;
        boolean doDelete = false;
        ems.setLength(0);
        for (NodeChange nodeChange : changes) {
            MemoryBank.debug(nodeChange.toString());
            if (nodeChange.changeType == NodeChange.RENAMED) {

                // Checking for the special case -
                if(theAreaNodeName.equals("Upcoming Events")) {
                    if (nodeChange.nodeName.equals(MemoryBank.appOpts.consolidatedEventsViewName)) {
                        // An exception to the norm - this one has no corresponding file.
                        MemoryBank.appOpts.consolidatedEventsViewName = nodeChange.renamedTo;
                        continue;
                    }
                }

                // Now attempt the rename
                String oldNamedFile = FileGroup.getFullFilename(theArea, nodeChange.nodeName);
                String newNamedFile = FileGroup.getFullFilename(theArea, nodeChange.renamedTo);
                File f = new File(oldNamedFile);

                try {
                    if (!f.renameTo(new File(newNamedFile))) {
                        throw new Exception("Unable to rename " + nodeChange.nodeName + " to " + nodeChange.renamedTo);
                    } // end if
                    theFileGroupKeeper.remove(nodeChange.nodeName);
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

                // The end result of a 'No' will be that the leaves will have already been
                // removed from the branch but they will still be available for reselection
                // in the list of choices during future branch edit sessions.
                if (!doDelete) continue;

                // Delete the file -
                String deleteFile =  FileGroup.getFullFilename(theArea, nodeChange.nodeName);
                MemoryBank.debug("Deleting " + deleteFile);
                try {
                    if (!(new File(deleteFile)).delete()) { // Delete the file.
                        throw new Exception("Unable to delete " + nodeChange.nodeName);
                    } // end if
                    theFileGroupKeeper.remove(nodeChange.nodeName);
                } catch (Exception se) {
                    ems.append(se.getMessage()).append(System.lineSeparator());
                } // end try/catch
            }
        }  // end for each change

        // Show the Exception(s), if any.
        if (!ems.toString().equals("")) {
            optionPane.showMessageDialog(theTree, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
        } // end if

        // Accept all 'To Do' Branch structure changes.
        // We saved this for last, in case the error message above kicked in and the user
        // wants to compare the original branch with the one shown in the editor.
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
            // IF an 'undo deletion' menu option was being shown and was NOT used, then the
            // action of the user clicking on the 'Apply' button needs to take away that menu option.
            // That will happen when the menus are re-managed upon showing the About panel, and
            // it will also happen with any other Tree selection, after the call to
            // showRestoreOption() below sets the flag to false.
            AppTreePanel.appMenuBar.showRestoreOption(false);

            AppTreePanel.theInstance.showAbout();
        }
    }  // end doApply


    @Override
    public ArrayList<String> getChoices() {
        ArrayList<String> theChoices = new ArrayList<>();

        // Get a list of <theNodeName> files in that area of the user's data directory.
        File dataDir = new File(MemoryBank.userDataHome + File.separatorChar + theArea);
        String[] theFileList = dataDir.list(
                new FilenameFilter() {
                    // Although this filter does not account for directories, it is
                    // known that the dataDir will not under normal program
                    // operation contain directories (except for the Calendar notes).
                    public boolean accept(File f, String s) {
                        return s.startsWith(thePrefix);
                    }
                }
        );

        // Create the list of files.
        if(theAreaNodeName.equals(AREA_EVENT)) {
            theChoices.add(MemoryBank.appOpts.consolidatedEventsViewName);
        }
        if (theFileList != null) {
            log.debug("Number of " + theAreaNodeName + " files found: " + theFileList.length);
            int theDot;
            String theFile;
            for (String afile : theFileList) {
                theDot = afile.lastIndexOf(".json");
                theFile = afile.substring(thePrefix.length(), theDot); // start after the prefix

                theChoices.add(theFile);
            } // end for
        }
        return theChoices;
    }

    @Override
    public String getRenameToString() {
        return renameTo;
    }


    // Used by test methods
    // BUT - later versions will just directly set it, no need for a test-only method.  Remove this when feasible.
    public void setNotifier(Notifier newNotifier) {
        optionPane = newNotifier;
    }

} // end class BranchHelper
