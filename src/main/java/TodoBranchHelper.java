/*
 A custom TreeBranchHelper, in support of the TreeBranchEditor actions on
 the TodoBranch.  In addition to the interface methods, there are several
 static methods that support other actions on the TodoBranch, that will be
 called by the AppTreePanel or other branches.  For actions on the TodoLists
 themselves (such as save, load, etc), see the TodoNoteGroup class.
*/

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.tree.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class TodoBranchHelper implements TreeBranchHelper {
    private static Logger log = LoggerFactory.getLogger(TodoBranchHelper.class);

    private JTree theTree;  // The original tree, not the one from the editor.
    private NoteGroupKeeper theNoteGroupKeeper;
    private DefaultTreeModel theTreeModel;
    private DefaultMutableTreeNode theRoot;
    private int theIndex;  // keeps track of which node (branch) of the tree we're on.
    private String renameFrom;
    Notifier optionPane;  // non-private access, for Tests.

    public TodoBranchHelper(JTree jt, NoteGroupKeeper noteGroupKeeper) {
        theTree = jt;
        theNoteGroupKeeper = noteGroupKeeper;
        theTreeModel = (DefaultTreeModel) theTree.getModel();
        theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();

        optionPane = new Notifier() {
        }; // Uses all default methods.

        // Get the index of the SearchResults node (not the same as row number)
        theIndex = -1;
        DefaultMutableTreeNode dmtn = TreeBranchHelper.getNodeByName(theRoot, "To Do Lists");
        if (dmtn != null) theIndex = theRoot.getIndex(dmtn);
    }

    // This method will return a TreePath for the provided String,
    // regardless of whether or not it really is a node on the tree.
    static TreePath getTodoPathFor(JTree jt, String s) {
        DefaultTreeModel tm = (DefaultTreeModel) jt.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) tm.getRoot();
        DefaultMutableTreeNode clonedRoot = AppTreePanel.deepClone(theRoot);
        DefaultMutableTreeNode theTodoNode = TreeBranchHelper.getNodeByName(clonedRoot, "To Do Lists");

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(s);

        // This is the 'tricky' part - we don't really want to add this node;
        // we just want the system to create the TreeNode array for us so we
        // can get and return the TreePath.  So - we didn't add it to the
        // 'real' tree, but a clone of the root.
        theTodoNode.add(newNode);

        return new TreePath(newNode.getPath());
    }

    static void addNewList(JTree jt) {
        String newName = "";
        String prompt = "Enter a name for the new To Do List";
        String title = "Add a new To Do List";

        newName = (String) JOptionPane.showInputDialog(
                jt,                           // parent component - for modality
                prompt,                       // prompt
                title,                        // pane title bar
                JOptionPane.QUESTION_MESSAGE, // type of pane
                null,                         // icon
                null,                         // list of choices
                newName);                     // initial value

        if (newName == null) return;      // No user entry; dialog was Cancelled.
        newName = nameAdjust(newName);

        DefaultTreeModel theTreeModel = (DefaultTreeModel) jt.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        DefaultMutableTreeNode dmtn = TreeBranchHelper.getNodeByName(theRoot, "To Do Lists");
        if (dmtn != null) {
            // Declare a tree node for the new list.
            DefaultMutableTreeNode newList;

            // Allowing 'add' to act as a back-door selection of a list
            // that actually already exists is ok, but do
            // not add this choice to the branch if it is already there.
            boolean addNodeToBranch = true;
            newList = (DefaultMutableTreeNode) getChild(dmtn, newName);
            if (newList == null) {
                newList = new DefaultMutableTreeNode(newName);
            } else {
                addNodeToBranch = false;
                // This also means that we don't need the checkFilename.
            }

            if (addNodeToBranch) {
                // Ensure that the new name meets our requirements.
                String theComplaint = TreeBranchHelper.checkFilename(newName, NoteGroup.basePath(TodoNoteGroup.areaName));
                if (!theComplaint.isEmpty()) {
                    JOptionPane.showMessageDialog(jt, theComplaint,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                // Add the new list name to the tree
                dmtn.add(newList);
                theTreeModel.nodeStructureChanged(dmtn);
            }

            // Select the list.
            TreePath tp = getPath(dmtn);
            jt.expandPath(tp);
            jt.setSelectionPath(new TreePath(newList.getPath()));
        }
    } // end addNewList


    @SuppressWarnings("rawtypes") // Adding a type then causes 'unchecked' problem.
    private static MutableTreeNode getChild(DefaultMutableTreeNode dmtn, String name) {
        Enumeration children = dmtn.children();
        while (children.hasMoreElements()) {
            DefaultMutableTreeNode achild = (DefaultMutableTreeNode) children.nextElement();
            if (achild.toString().equals(name)) {
                return achild;
            }
        }
        return null;
    }

    // Called when adding a new list.  First, it trims any leading and trailing spaces.
    // Then it checks to see if the file already exists.  But rather than considering that
    // to be an error condition, we allow this situation to be a back-door selection method
    // rather than an 'Add'.  The only thing is - on a case-insensitive file system the file
    // may exist but not necessarily with the same casing as the name that was entered by
    // the user.  So - if we find that it does exist, we adopt that name and casing which
    // may be different.  After that, they can change the casing if desired, via the rename
    // mechanism.  But note that it would have to be a two-step process; a case-only name
    // change would look like a same-file conflict (unlike 'Add', the rename operation does
    // consider that to be an error).  So for an example of changing case via a rename:
    // 'upper' ==> 'UPPER' could be accomplished by 'upper' ==> 'upper1' ==> "UPPER".
    private static String nameAdjust(String name) {
        String adjustedName;
        adjustedName = name.trim();
        if (!adjustedName.isEmpty()) {
            String newNamedFile = MemoryBank.userDataHome + File.separatorChar + "TodoLists" + File.separatorChar;
            newNamedFile += "todo_" + adjustedName + ".json";
            File f = new File(newNamedFile);
            if (f.exists()) {
                try {
                    String longCaseName = f.getCanonicalPath();
                    adjustedName = NoteGroup.prettyName(longCaseName);
                } catch (IOException ioe) {
                    System.out.println(ioe.getMessage());
                }
            }
        }
        return adjustedName;
    }


    public static TreePath getPath(TreeNode treeNode) {
        List<Object> nodes = new ArrayList<>();
        if (treeNode != null) {
            nodes.add(treeNode);
            treeNode = treeNode.getParent();
            while (treeNode != null) {
                nodes.add(0, treeNode);
                treeNode = treeNode.getParent();
            }
        }

        return nodes.isEmpty() ? null : new TreePath(nodes.toArray());
    }


    @Override
    public boolean allowRenameFrom(DefaultMutableTreeNode theNode) {
        String theName = theNode.toString();
        if(theNode.getAllowsChildren()) {
            JOptionPane.showMessageDialog(new JFrame(), "You are not allowed to rename the tree branch: " + theName);
            return false;
        }
        renameFrom = theName.trim(); // Used in renameTo; trim is ok but don't mess with case.
        return true;
    }

    @Override
    public boolean allowRenameTo(String theName) {
        // If theName is also our 'renameFrom' name then the whole thing is a no-op.
        // No need to put out a complaint about that; just return a false.  But if
        // there is a difference in the casing then we will get past this check.
        if (theName.trim().equals(renameFrom)) return false;
        // And that means we might get a 'file exists' complaint from checkFilename,
        // on a case-insensitive filesystem.

        String theComplaint = TreeBranchHelper.checkFilename(theName, NoteGroup.basePath("TodoLists"));
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

        // Get a list of To Do lists in the user's data directory.
        File dataDir = new File(NoteGroup.basePath(TodoNoteGroup.areaName));
        String[] theFileList = dataDir.list(
                new FilenameFilter() {
                    // Although this filter does not account for directories, it is
                    // known that the basePath will not under normal program
                    // operation contain any directory with a name starting with 'todo_'.
                    public boolean accept(File f, String s) {
                        return s.startsWith("todo_");
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
                theFile = afile.substring(5, theDot); // start after the 'todo_'

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

        // Handle 'To Do' file renamings and deletions
        String deleteWarning = null;
        boolean doDelete = false;
        ems.setLength(0);
        String basePath = NoteGroup.basePath(TodoNoteGroup.areaName);
        for (Object nco : changes) {
            NodeChange nodeChange = (NodeChange) nco;
            System.out.println(nco.toString());
            if (nodeChange.changeType == NodeChange.RENAMED) {
                // Now attempt the rename
                String oldNamedFile = basePath + "todo_" + nodeChange.nodeName + ".json";
                String newNamedFile = basePath + "todo_" + nodeChange.renamedTo + ".json";
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
                    deleteWarning = "Deletions of 'To Do' Lists cannot be undone.";
                    deleteWarning += System.lineSeparator() + "Are you sure?";

                    doDelete = JOptionPane.showConfirmDialog(theTree, deleteWarning,
                            "Warning", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
                }

                // The end result of a 'No' will be that the leaves will have already been
                // removed from the branch but they will still be available for reselection
                // in the list of choices during future branch edit sessions.
                if (!doDelete) continue;

                // Delete the file -
                String deleteFile = basePath + "todo_" + nodeChange.nodeName + ".json";
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
        theRoot.insert(mtn, theIndex);
        theTreeModel.nodeStructureChanged(mtn); // Localized; does not collapse the branch.

        // We do this last step because now that the edits have been accepted, we do not want to leave
        // both the 'official' branch and the 'editor' branch showing side-by-side, identical to
        // each other.  This way, the user gets a more final indication that the editing session
        // is completed, and it removes the possibility that they might click the 'Cancel' button,
        // which otherwise would revert the editor branch and choices (but not the 'official' ones.
        // If they do want to go back and see the branch editor again it is a simple click away
        // for them but by having them do that, they reset the editor to the new official branch
        // and choices as the starting point, and 'Cancel' would have no effect until they have
        // made more changes.
        AppTreePanel.theInstance.showAbout();
    }  // end doApply

    // Call this method to do a 'programmatic' rename of a TodoList
    // node on the Tree, as opposed to doing it manually via the
    // TreeBranchEditor.  It operates only on the tree and not with
    // any corresponding files; you must do that separately.
    // See the 'Save As...' methodology for a good example.
    static void renameTodoListLeaf(String oldname, String newname) {
        boolean changeWasMade = false;
        JTree jt = AppTreePanel.theInstance.getTree();
        DefaultTreeModel tm = (DefaultTreeModel) jt.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) tm.getRoot();
        DefaultMutableTreeNode theTodoBranch = TreeBranchHelper.getNodeByName(theRoot,"To Do Lists");

        // The tree is set for single-selection, so the selection will not be a collection but
        // a single value.  Nonetheless, Swing only provides a get for min and max and either
        // one will work for us.  Note that the TreePath returned by getSelectionPath()
        // will probably NOT work for reselection after we do the rename, so we use the row.
        int returnToRow = jt.getMaxSelectionRow();

        int numLeaves = theTodoBranch.getChildCount();
        DefaultMutableTreeNode leafLink;

        leafLink = theTodoBranch.getFirstLeaf();

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
        tm.nodeStructureChanged(theTodoBranch);

        // Reselect this tree node.
        jt.setSelectionRow(returnToRow);

    } // end renameTodoList


} // end class TodoBranchHelper
