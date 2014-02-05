import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.MutableTreeNode;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Enumeration;

// TODO
// Fix the startup debug messages (user data loc)
// Test showAbout after removing all search results, then test again - toggle?
// Implement a list 'add'.
// Remove the old todolistmanager and handler.
// Use the branch editor for all other appropriate branches.
// app user name
// app window size



public class TodoBranchHelper implements TreeBranchHelper {
    private static Logger log = LoggerFactory.getLogger(TodoBranchHelper.class);
    private static String ems;  // Error Message String

    private JTree theTree;  // The original tree, not the one from the editor.
    private DefaultTreeModel theTreeModel = null;
    private DefaultMutableTreeNode theRoot;
    private int todoIndex;

    // Construct this class with the JTree that contains the TodoBranch.
    public TodoBranchHelper(JTree jt) {
        theTree = jt;
        theTreeModel = (DefaultTreeModel) theTree.getModel();
        theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        todoIndex = -1;

        DefaultMutableTreeNode dmtn = getTodoNode(theRoot);
        if(dmtn != null) todoIndex = theRoot.getIndex(dmtn);
    }

    public static DefaultMutableTreeNode getTodoNode(DefaultMutableTreeNode theRoot) {
        DefaultMutableTreeNode dmtn = null;
        Enumeration bfe = theRoot.breadthFirstEnumeration();

        while(bfe.hasMoreElements()) {
            dmtn = (DefaultMutableTreeNode) bfe.nextElement();
            if (dmtn.toString().equals("To Do Lists")) {
                break;
            }
        }
        return dmtn;
    }


    @Override
    public boolean allowRenameFrom(String theName) {
        if(theName.equals("To Do Lists")) {
            JOptionPane.showMessageDialog(new JFrame(), "You are not allowed to rename the root!");
            return false;
        }
        return true;
    }

    @Override
    public boolean allowRenameTo(String theName) {

        // Check to see if the destination file name already exists.
        // If so then complain and refuse to do the rename.
        String newNamedFile = MemoryBank.userDataDirPathName + File.separatorChar;
        newNamedFile += theName + ".todolist";

        if ((new File(newNamedFile)).exists()) {
            ems = "A list named " + theName + " already exists!\n";
            ems += "  Rename operation cancelled.";
            JOptionPane.showMessageDialog(theTree, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } // end if

        return true;
    }

    @Override
    public ArrayList<String> getChoices() {
        ArrayList<String> theChoices = new ArrayList<String>();

        // Get a list of To Do lists in the user's data directory.
        File dataDir = new File(MemoryBank.userDataDirPathName);
        String[] theFileList = dataDir.list(
                new FilenameFilter() {
                    // Although this filter does not account for directories, it is
                    // known that the 'MemoryBank.location' will not under normal program
                    // operation contain any directory ending in '.todolist'.
                    public boolean accept(File f, String s) {
                        return s.endsWith(".todolist");
                    }
                }
        );

        // Create the list of files.
        int theDot;
        String theFile;
        log.debug("Number of todolist files found: " + theFileList.length);
        for (String afile : theFileList) {
            theDot = afile.lastIndexOf(".todolist");
            theFile = afile.substring(0, theDot);

            theChoices.add(theFile);
        } // end for
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
    // them as a whole, by directly adopting the returned 'mtn' as our new branch.
    // But in addition to (possibly) having an effect on the final branch, the
    // rename and delete actions from the editor imply changes to the filesystem
    // and those directives are also handled here.  For these actions, we need to
    // examine the 'changes' list.  But what happens if we cannot perform all the
    // tasks from the list?  In that case, the branch may no longer accurately
    // represent the true state of the todolist files and the user will be informed.
    @Override
    public void doApply(MutableTreeNode mtn, ArrayList changes) {
        if(todoIndex == -1) return;

        // Handle 'To Do' file renamings and deletions
        String deleteWarning = null;
        boolean doDelete = false;
        ems = "";
        String basePath = MemoryBank.userDataDirPathName + File.separatorChar;
        for(Object nco: changes) {
            NodeChange nc = (NodeChange) nco;
            System.out.println(nco.toString());
            if(nc.changeType == NodeChange.RENAMED) {
                // Now attempt the rename
                String oldNamedFile = basePath + nc.nodeName + ".todolist";
                String newNamedFile = basePath + nc.renamedTo + ".todolist";
                File f = new File(oldNamedFile);

                try {
                    if (!f.renameTo(new File(newNamedFile))) {
                        throw new Exception("Unable to rename " + nc.nodeName + " to " + nc.renamedTo);
                    } // end if
                } catch (SecurityException se) {
                    ems += se.getMessage() + System.lineSeparator();
                } catch (Exception ue) {  // User Exception
                    ems += ue.getMessage() + System.lineSeparator();
                } // end try/catch

            }   else if(nc.changeType == NodeChange.REMOVED) {

                if(deleteWarning == null) {
                    deleteWarning = "Deletions of 'To Do' Lists cannot be undone.";
                    deleteWarning += System.lineSeparator() + "Are you sure?";

                    doDelete = JOptionPane.showConfirmDialog(theTree, deleteWarning,
                        "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                }

                // The end result of a 'No' will be that the leaves will have already been
                // removed from the branch but they will still be available for reselection
                // in the list of choices during future branch edit sessions.
                if(!doDelete) continue;

                // Delete the file -
                String deleteFile = basePath + nc.nodeName + ".todolist";
                MemoryBank.debug("Deleting " + deleteFile);
                try {
                    if (!(new File(deleteFile)).delete()) { // Delete the file.
                        throw new Exception("Unable to delete " + nc.nodeName);
                    } // end if
                } catch (SecurityException se) {
                    ems += se.getMessage() + System.lineSeparator();
                } catch (Exception ue) {  // User Exception
                    ems += ue.getMessage() + System.lineSeparator();
                } // end try/catch

            }
        }  // end for each change

        // Show the Exception(s), if any.
        if (!ems.equals("")) {
            JOptionPane.showMessageDialog(theTree, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
        } // end if

        // Accept all 'To Do' Branch structure changes.
        // We saved this for last, in case the error message above kicked in and the user
        // wants to compare the original branch with the one shown in the editor.
        theRoot.remove(todoIndex);
        theRoot.insert(mtn, todoIndex);
        theTreeModel.nodeStructureChanged(mtn);
        theTree.expandRow(todoIndex);

        // We do this last step because now that the edits have been accepted, we do not want both
        // the 'official' branch and the 'editor' branch to be shown side-by-side, identical to
        // each other.  This way, the user gets a more final indication that the editing session
        // is completed, and it removes the possibility that they might click the 'Cancel' button,
        // which otherwise would revert the editor branch and choices (but not the 'official' ones.
        // If they do want to go back and see the branch editor again it is a simple click away
        // for them but by having them do that, they reset the editor to the new official branch
        // and choices as the starting point, and 'Cancel' would have no effect until they have
        // made more changes.
        MemoryBank.getAppTree().showAbout();

    }  // end doApply

    @Override
    public String getDeleteCommand() {
        return null;
    }
}
