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
import java.awt.*;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class TodoBranchHelper implements TreeBranchHelper {
    static final long serialVersionUID = -1L;
    private static Logger log = LoggerFactory.getLogger(TodoBranchHelper.class);

    private static StringBuilder ems = new StringBuilder();  // Error Message String
    private static final int MAX_FILENAME_LENGTH = 32; // Arbitrary, but helps with UI issues.
    private JTree theTree;  // The original tree, not the one from the editor.
    private DefaultTreeModel theTreeModel;
    private DefaultMutableTreeNode theRoot;
    private int todoIndex;
    private String renameFrom;

    // Construct this class with the JTree that contains the TodoBranch.
    public TodoBranchHelper(JTree jt) {
        theTree = jt;
        theTreeModel = (DefaultTreeModel) theTree.getModel();
        theRoot = (DefaultMutableTreeNode) theTreeModel.getRoot();
        todoIndex = -1;

        DefaultMutableTreeNode dmtn = getTodoNode(theRoot);
        if (dmtn != null) todoIndex = theRoot.getIndex(dmtn);
    }

    // This method will return a TreePath for the provided String,
    // regardless of whether or not it really is a node on the tree.
    static TreePath getTodoPathFor(JTree jt, String s) {
        DefaultTreeModel tm = (DefaultTreeModel) jt.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) tm.getRoot();
        DefaultMutableTreeNode clonedRoot = AppTreePanel.deepClone(theRoot);
        DefaultMutableTreeNode theTodoNode = getTodoNode(clonedRoot);

        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(s);

        // This is the 'tricky' part - we don't really want to add this node;
        // we just want the system to create the TreeNode array for us so we
        // can get and return the TreePath.  So - we didn't add it to the
        // 'real' tree, but a clone of the root.
        theTodoNode.add(newNode);

        return new TreePath(newNode.getPath());
    }

    @SuppressWarnings("rawtypes")
    static DefaultMutableTreeNode getTodoNode(DefaultMutableTreeNode theRoot) {
        DefaultMutableTreeNode dmtn = null;
        Enumeration bfe = theRoot.breadthFirstEnumeration();

        while (bfe.hasMoreElements()) {
            dmtn = (DefaultMutableTreeNode) bfe.nextElement();
            if (dmtn.toString().equals("To Do Lists")) {
                break;
            }
        }
        return dmtn;
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
        DefaultMutableTreeNode dmtn = getTodoNode(theRoot);
        if (dmtn != null) {
            // Declare a tree node for the new list.
            DefaultMutableTreeNode newList;

            // Allowing 'add' to act as a back-door selection is ok, but do
            // not add this choice to the branch if it is already there.
            boolean addNodeToBranch = true;
            newList = (DefaultMutableTreeNode) getChild(dmtn, newName);
            if (newList == null) {
                newList = new DefaultMutableTreeNode(newName);
            } else {
                addNodeToBranch = false;
            }

            if (addNodeToBranch) {
                // Ensure that the new name meets our requirements.
                if (!nameCheck(newName, jt)) return;

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
            String newNamedFile = MemoryBank.userDataHome + File.separatorChar;
            newNamedFile += "todo_" + adjustedName + ".json";
            File f = new File(newNamedFile);
            if (f.exists()) {
                try {
                    String longCaseName = f.getCanonicalPath();
                    adjustedName = TodoNoteGroup.prettyName(longCaseName);
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

        // Check to see if the destination file name already exists.
        // If so then complain and refuse to do the rename.
        String newNamedFile = MemoryBank.userDataHome + File.separatorChar;
        newNamedFile += "todo_" + theName.trim() + ".json";

        if ((new File(newNamedFile)).exists()) {
            ems.setLength(0);
            ems.append("A list with that name already exists!").append(System.lineSeparator());
            ems.append("  Rename operation cancelled.");
            JOptionPane.showMessageDialog(theTree, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } // end if

        return nameCheck(theName, null);
    }

    @Override
    public ArrayList<String> getChoices() {
        ArrayList<String> theChoices = new ArrayList<>();

        // Get a list of To Do lists in the user's data directory.
        File dataDir = new File(MemoryBank.userDataHome);
        String[] theFileList = dataDir.list(
                new FilenameFilter() {
                    // Although this filter does not account for directories, it is
                    // known that the 'MemoryBank.userDataHome' will not under normal program
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
    public void doApply(MutableTreeNode mtn, ArrayList<NodeChange> changes) {
        if (todoIndex == -1) return;

        // Handle 'To Do' file renamings and deletions
        String deleteWarning = null;
        boolean doDelete = false;
        ems.setLength(0);
        String basePath = MemoryBank.userDataHome + File.separatorChar;
        for (Object nco : changes) {
            NodeChange nc = (NodeChange) nco;
            System.out.println(nco.toString());
            if (nc.changeType == NodeChange.RENAMED) {
                // Now attempt the rename
                String oldNamedFile = basePath + "todo_" + nc.nodeName + ".json";
                String newNamedFile = basePath + "todo_" + nc.renamedTo + ".json";
                File f = new File(oldNamedFile);

                try {
                    if (!f.renameTo(new File(newNamedFile))) {
                        throw new Exception("Unable to rename " + nc.nodeName + " to " + nc.renamedTo);
                    } // end if
                    MemoryBank.getAppTreePanel().getTodoListKeeper().remove(nc.nodeName);
                } catch (Exception se) {
                    ems.append(se.getMessage()).append(System.lineSeparator());
                } // end try/catch

            } else if (nc.changeType == NodeChange.REMOVED) {

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
                String deleteFile = basePath + "todo_" + nc.nodeName + ".json";
                MemoryBank.debug("Deleting " + deleteFile);
                try {
                    if (!(new File(deleteFile)).delete()) { // Delete the file.
                        throw new Exception("Unable to delete " + nc.nodeName);
                    } // end if
                    MemoryBank.getAppTreePanel().getTodoListKeeper().remove(nc.nodeName);
                } catch (Exception se) {
                    ems.append(se.getMessage()).append(System.lineSeparator());
                }// User Exception
// end try/catch

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
    // Return Value - false if file name should not be allowed;
    //    otherwise, true.  Also, any 'false' return will be
    //    preceeded by an informative error dialog.
    //-------------------------------------------------------------------
    static boolean nameCheck(String theName, Component parent) {
        Exception e = null;
        ems.setLength(0);

        theName = theName.trim();

        // Check for non-entry (or evaporation).
        if (theName.equals("")) {
            ems.append("No New List Name was supplied!");
        } // end if

        // Refuse unwanted help.
        if (theName.endsWith(".json")) {
            ems.append("The name you supply cannot end in '.json'");
        } // end if

        // Check for legal max length.
        // The input field used in this class would not allow this one, but
        //   since this method is static and accepts input from outside
        //   sources, this check should be done.
        if (theName.length() > MAX_FILENAME_LENGTH) {
            ems.append("The new name is limited to " + MAX_FILENAME_LENGTH + " characters");
        } // end if

        if (!ems.toString().equals("")) {
            JOptionPane.showMessageDialog(parent, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } // end if

        // Check to see if a file with this name already exists?
        //   No; for an 'add' the AppTreePanel can handle that situation
        //   by simply opening it, a kind of back-door selection.

        // Note - I thought it would be a good idea to check for 'illegal'
        //   characters in the filename, but when I started testing, the
        //   Windows OS accepted %, -, $, ! and &; not sure what IS illegal.
        //   Of course there ARE illegal characters, and the ones above may
        //   also be illegal on another OS.  So, the best way to
        //   detect them is to try to create a file using the name we're
        //   checking.  Any io error and we can fail this check.

        // Now try to create the file, with a '.test' extension; this will not
        // conflict with any 'legal' existing file in this directory, so if
        // there is any problem at all then we can report a failure.

        String theFilename = MemoryBank.userDataHome + File.separatorChar + theName + ".test";
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
            if (!b) System.out.print("");
        } // end try/catch

        // Handle Exceptions, if any.
        if (e != null) {
            JOptionPane.showMessageDialog(parent, e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } // end if

        return true;
    } // end nameCheck

    //----------------------------------------------------------------
    // Method Name:  renameTodoListLeaf
    //
    // Call this method to do a 'programmatic' rename of a TodoList
    // node on the Tree.  It operates only on the MemoryBank tree and
    // not with any corresponding files; you can do that separately,
    // before or after this, if needed.

    // A calling context should verify the validity of the newname
    // before coming here.  See the 'save as' methodology for a good
    // example.
    //----------------------------------------------------------------
    static void renameTodoListLeaf(String oldname, String newname) {
        boolean changeWasMade = false;
        JTree jt = MemoryBank.getAppTreePanel().getTree();
        DefaultTreeModel tm = (DefaultTreeModel) jt.getModel();
        DefaultMutableTreeNode theRoot = (DefaultMutableTreeNode) tm.getRoot();
        DefaultMutableTreeNode theTodoBranch = getTodoNode(theRoot);

        // The tree is set for single-selection, so the selection will not be a collection but
        // a single value.  Nonetheless, Swing only provides a get for min and max and either
        // one will work for us.  Note that the TreePath returned by getSelectionPath()
        // will probably NOT work for reselection after we do the rename.
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
