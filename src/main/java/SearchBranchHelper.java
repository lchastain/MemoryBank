/*
 A custom TreeBranchHelper, in support of the TreeBranchEditor actions on
 the SearchResults Branch.  In addition to the base methods, there are several
 static methods that support other actions on the Search Results, that will be
 called by the AppTreePanel or other branches.  For actions on the SearchResults
 themselves (such as load, etc), see the SearchResultGroup class.
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

public class SearchBranchHelper implements TreeBranchHelper {
    private static Logger log = LoggerFactory.getLogger(SearchBranchHelper.class);

    private JTree theTree;  // The original tree, not the one from the editor.
    private NoteGroupKeeper theNoteGroupKeeper;
    private DefaultTreeModel theTreeModel;
    private DefaultMutableTreeNode theRoot;
    private int theIndex;  // keeps track of which row of the tree we're on.
    private String renameFrom;
    Notifier optionPane;  // non-private access, for Tests.

    SearchBranchHelper(JTree jt, NoteGroupKeeper noteGroupKeeper) {
        theTree = jt;
        theNoteGroupKeeper = noteGroupKeeper;
        theTreeModel = (DefaultTreeModel) theTree.getModel();
        theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();

        optionPane = new Notifier() {
        }; // Uses all default methods.

        // Get the index of the SearchResults node (not the same as row number)
        theIndex = -1;
        DefaultMutableTreeNode dmtn = TreeBranchHelper.getNodeByName(theRoot, "Search Results");
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
        // If theName is also our 'renameFrom' name then the whole thing is a no-op.
        // No need to put out a complaint about that; just return a false.  But if
        // there is a difference in the casing then we will get past this check.
        if (theName.trim().equals(renameFrom)) return false;
        // And that means we might get a 'file exists' complaint from checkFilename,
        // on a case-insensitive filesystem.

        // Since the areaName is different for each implementation of TreeBranchHelper, there would be no
        // value-added for default-implementing this method in the interface; any adopters of it would still
        // need to provide their own.  It is important to check filename validity in the area where the new
        // file would be created, so that any possible Security Exception is seen, and those Exceptions may
        // not be seen in a different area of the same filesystem.
        // May want to consider implementing the interface in a new base class; then the allowRenameTo method
        // could live there (and getChoices?) with areaName being an overridden member variable for each child
        // but - not sure of all the considerations, and naming issues -
        // BranchHelperInterface, BranchHelperImpl ?    So far the value added
        String theComplaint = TreeBranchHelper.checkFilename(theName, NoteGroup.basePath(SearchResultGroup.areaName));
        if (!theComplaint.isEmpty()) {
            optionPane.showMessageDialog(theTree, theComplaint,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    @Override
    public ArrayList<String> getChoices() {
        ArrayList<String> theChoices = new ArrayList<>();

        // Get a list of Search Results in the user's data directory.
        File dataDir = new File(NoteGroup.basePath(SearchResultGroup.areaName));
        String[] theFileList = dataDir.list(
                new FilenameFilter() {
                    // Although this filter does not account for directories, it is
                    // known that the basePath will not under normal program
                    // operation contain any directory with a name starting with 'search_'.
                    public boolean accept(File f, String s) {
                        return s.startsWith("search_");
                    }
                }
        );

        // Create the list of files.
        if (theFileList != null) {
            log.debug("Number of search result files found: " + theFileList.length);
            int theDot;
            String theFile;
            for (String afile : theFileList) {
                theDot = afile.lastIndexOf(".json");
                theFile = afile.substring(7, theDot); // start after the 'search_'

                theChoices.add(theFile);
            } // end for
        }
        return theChoices;
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
        for (Object nco : changes) {
            NodeChange nodeChange = (NodeChange) nco;
            MemoryBank.debug(nco.toString());
            if (nodeChange.changeType == NodeChange.RENAMED) {
                // Now attempt the rename
                String oldNamedFile = NoteGroup.basePath(SearchResultGroup.areaName) + "search_" + nodeChange.nodeName + ".json";
                String newNamedFile = NoteGroup.basePath(SearchResultGroup.areaName) + "search_" + nodeChange.renamedTo + ".json";
                File f = new File(oldNamedFile);

                try {
                    if (!f.renameTo(new File(newNamedFile))) {
                        throw new Exception("Unable to rename " + nodeChange.nodeName + " to " + nodeChange.renamedTo);
                    } // end if
                    theNoteGroupKeeper.remove(nodeChange.nodeName);
                } catch (Exception se) {
                    ems.append(se.getMessage()).append(System.lineSeparator());
                } // end try/catch

            } else if (nodeChange.changeType == NodeChange.REMOVED) {
                if (deleteWarning == null) {
                    deleteWarning = "Deletions of Search Results cannot be undone.";
                    deleteWarning += System.lineSeparator() + "Are you sure?";

                    doDelete = JOptionPane.showConfirmDialog(theTree, deleteWarning,
                            "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                }

                // The end result of a 'No' will be that the leaves will have already been
                // removed from the branch but they will still be available for reselection
                // in the list of choices during future branch edit sessions.
                if (!doDelete) continue;

                // Delete the file -
                String deleteFile =  NoteGroup.basePath(SearchResultGroup.areaName) + "search_" + nodeChange.nodeName + ".json";
                MemoryBank.debug("Deleting " + deleteFile);
                try {
                    if (!(new File(deleteFile)).delete()) { // Delete the file.
                        throw new Exception("Unable to delete " + nodeChange.nodeName);
                    } // end if
                    theNoteGroupKeeper.remove(nodeChange.nodeName);
                } catch (Exception se) {
                    ems.append(se.getMessage()).append(System.lineSeparator());
                } // end try/catch
            }
        }  // end for each change

        // Show the Exception(s), if any.
        if (!ems.toString().equals("")) {
            JOptionPane.showMessageDialog(theTree, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
        } // end if

        // Accept all 'To Do' Branch structure changes.
        // We saved this for last, in case the error message above kicked in and the user
        // wants to compare the original branch with the one shown in the editor.
        theRoot.remove(theIndex);
        theRoot.insert(mtn, theIndex); // Goes back to the same place.
        theTreeModel.nodeStructureChanged(mtn); // Localized; the node does not 'collapse'.

        // We do this last step because now that the edits have been accepted, we do not want both
        // the 'official' branch and the 'editor' branch to be shown side-by-side, identical to
        // each other.  This way, the user gets a more final indication that the editing session
        // is completed, and it removes the possibility that they might click the 'Cancel' button,
        // which otherwise would revert the editor branch and choices (but not the 'official' ones.
        // If they do want to go back and see the branch editor again it is a simple click away
        // for them but by having them do that, they reset the editor to the new official branch
        // and choices as the starting point, and 'Cancel' would have no effect until they have
        // made more changes.
        AppTreePanel.theInstance.showAbout();
    }  // end doApply

} // end class SearchBranchHelper
