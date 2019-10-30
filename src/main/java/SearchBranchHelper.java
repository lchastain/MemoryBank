/*
 A custom TreeBranchHelper, in support of the TreeBranchEditor actions on
 the SearchResults Branch.  In addition to the interface methods, there are several
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;

public class SearchBranchHelper implements TreeBranchHelper {
    private static Logger log = LoggerFactory.getLogger(SearchBranchHelper.class);

    private static StringBuilder ems = new StringBuilder();  // Error Message String
    private static final int MAX_FILENAME_LENGTH = 32; // Arbitrary, but helps with UI issues.
    private JTree theTree;  // The original tree, not the one from the editor.
    private NoteGroupKeeper theNoteGroupKeeper;
    private DefaultTreeModel theTreeModel;
    private DefaultMutableTreeNode theRoot;
    private int theIndex;  // keeps track of which row of the tree we're on.
    private String renameFrom;

    // Construct this class with the JTree that contains the TodoBranch.
    SearchBranchHelper(JTree jt, NoteGroupKeeper noteGroupKeeper) {
        theTree = jt;
        theNoteGroupKeeper = noteGroupKeeper;
        theTreeModel = (DefaultTreeModel) theTree.getModel();
        theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();

        // Get the index of the SearchResults node (not the same as row number)
        theIndex = -1;
        DefaultMutableTreeNode dmtn = getSearchResultsNode(theRoot);
        if (dmtn != null) theIndex = theRoot.getIndex(dmtn);
    }

    @SuppressWarnings("rawtypes")
    static DefaultMutableTreeNode getSearchResultsNode(DefaultMutableTreeNode theRoot) {
        DefaultMutableTreeNode dmtn = null;
        Enumeration bfe = theRoot.breadthFirstEnumeration();

        while (bfe.hasMoreElements()) {
            dmtn = (DefaultMutableTreeNode) bfe.nextElement();
            if (dmtn.toString().equals("Search Results")) {
                break;
            }
        }
        return dmtn;
    }


    @Override
    public boolean allowRenameFrom(String theName) {
        if (theName.equals("To Do Lists")) {
            JOptionPane.showMessageDialog(new JFrame(), "You are not allowed to rename the root!");
            return false;
        }
        renameFrom = theName.trim(); // Used in renameTo; trim is ok but don't mess with case.
        return true;
    }

    @Override
    public boolean allowRenameTo(String theName) {
        // If theName is also our 'renameFrom' name then the whole thing is a no-op.
        // No need to put out a complaint about that; just return a false.
        if (theName.trim().equals(renameFrom)) return false;
        // But we do support case-sensitive filesystems, so if 'case' is the only difference
        // between the two names then we will fall thru to the 'file exists' complaint, on a
        // case-insensitive filesystem.

        String theComplaint = nameCheck(theName);
        if (!theComplaint.isEmpty()) {
            JOptionPane.showMessageDialog(theTree, theComplaint,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    @Override
    public ArrayList<String> getChoices() {
        ArrayList<String> theChoices = new ArrayList<>();

        // Get a list of To Do lists in the user's data directory.
        File dataDir = new File(MemoryBank.userDataHome + File.separatorChar + "SearchResults");
        String[] theFileList = dataDir.list(
                new FilenameFilter() {
                    // Although this filter does not account for directories, it is
                    // known that the 'MemoryBank.userDataHome' will not under normal program
                    // operation contain any directory with a name starting with 'todo_'.
                    public boolean accept(File f, String s) {
                        return s.startsWith("search_");
                    }
                }
        );

        // Create the list of files.
        if (theFileList != null) {
            log.debug("Number of todolist files found: " + theFileList.length);
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

    @Override
    public boolean deletesAllowed() {
        return true;
    }

    @Override
    public boolean makeParents() {
        return false;
    }

    // This method is the handler for the 'Apply' button of the TreeBranchEditor.
    // For tree structure events, we don't address them individually but just accept
    // them as a whole, by directly adopting the 'mtn' parameter as our new branch.
    // But in addition to (possibly) having an effect on the final branch, the
    // rename and delete actions from the editor imply changes to the filesystem
    // and those directives are also handled here.  For these actions, we need to
    // examine the 'changes' list.  But what happens if we cannot perform all the
    // tasks from the list?  In that case, the branch may no longer accurately
    // represent the true state of the todolist files and the user will be informed.
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
        String basePath = SearchResultGroup.basePath();
        for (Object nco : changes) {
            NodeChange nodeChange = (NodeChange) nco;
            MemoryBank.debug(nco.toString());
            if (nodeChange.changeType == NodeChange.RENAMED) {
                // Now attempt the rename
                String oldNamedFile = basePath + "search_" + nodeChange.nodeName + ".json";
                String newNamedFile = basePath + "search_" + nodeChange.renamedTo + ".json";
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
                String deleteFile = basePath + "search_" + nodeChange.nodeName + ".json";
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
        MemoryBank.getAppTreePanel().showAbout();
    }  // end doApply

    @Override
    public String getDeleteCommand() {
        return null;
    }

    //-------------------------------------------------------------------
    // Method Name:  nameCheck
    //
    // Check for the following 'illegal' file naming conditions:
    //   No entry, or whitespace only.
    //   The name ends in '.json'.
    //   The name contains more than MAX_FILENAME_LENGTH chars.
    //   The filesystem refuses to create a file with this name.
    //
    // Return Value - A 'complaint' string if name is not valid,
    //    otherwise an empty string.
    //-------------------------------------------------------------------
    private static String nameCheck(String theProposedName) {
        Exception e = null;
        ems.setLength(0);

        String testName = theProposedName.trim();

        // Check for non-entry (or evaporation).
        if (testName.isEmpty()) ems.append("No New List Name was supplied!");

        // Refuse unwanted help.
        if (testName.endsWith(".json")) {
            ems.append("The name you supply cannot end in '.json'");
        } // end if

        // Check for legal max length.
        // The input field used in this class would not allow this one, but
        //   since this method is static and accepts input from outside
        //   sources, this check should be done.
        if (testName.length() > MAX_FILENAME_LENGTH) {
            ems.append("The new name is limited to " + MAX_FILENAME_LENGTH + " characters");
        } // end if

        // Any problems found up to this point would not allow us to continue checking.
        if (!ems.toString().isEmpty()) return ems.toString();

        // Do we want to check to see if a file with this name already exists?
        //   No.  The handler for the renaming event will disallow that particular
        //   situation, so we don't need to repeat that logic here.

        // Note - I thought it would be a good idea to check for 'illegal'
        //   characters in the filename, but when I started testing, the
        //   Windows OS accepted %, -, $, ! and &; not sure what IS illegal.
        //   Of course there ARE illegal characters, and the ones above may
        //   still be illegal on another OS.  So, the best way to
        //   detect them is to try to create a file using the name we're
        //   checking.  Any io error and we can fail this check.

        // Now try to create the file, with a '.test' extension; this will not
        // conflict with any 'legal' existing file in this directory, so if
        // there is any problem at all then we can report a failure.

        String theFilename = SearchResultGroup.basePath() + testName + ".test";
        File f = new File(theFilename);
        MemoryBank.debug("Name checking new file: " + f.getAbsolutePath());
        boolean b = false; // Used only for 'greening' the code.
        try {
            b = f.delete();
            // If the file does not already exist, this simply returned a false.
            // If it did already exist then we must consider how it got there and
            // that this attempt to delete will quite probably throw a Security
            // Exception. But if it does not then the file is gone now so we go on.

            b = f.createNewFile();
            // We didn't test the return value here because anything short of an
            // Exception means that (thanks to the previous delete) the value is
            // 'true' and we just now created a file with the specified name.

            // The above call would have worked even without the previous delete,
            // but in the case of a preexisting file we would not have been sure
            // that we had overcome any possible security exception.  Since we
            // know that we just now created this file, we also know that it is
            // writable and do not need to check 'canWrite'.

            b = f.delete(); // So, delete the test file; name test passed.
        } catch (IOException | SecurityException ee) {
            e = ee;  // Identify now, handle below.
        } finally {
            if (!b) System.out.print(""); // We didn't care about any results given to 'b'.
        } // end try/catch

        // Handle Exceptions, if any.
        if (e != null) ems.append(e.getMessage());

        return ems.toString();
    } // end nameCheck

} // end class TodoBranchHelper
