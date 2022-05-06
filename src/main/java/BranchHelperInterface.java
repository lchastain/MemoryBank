import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;
import java.util.Enumeration;

@SuppressWarnings("rawtypes")
public interface BranchHelperInterface {
    StringBuilder ems = new StringBuilder();  // Error Message String

    // Called by the TreeBranchEditor to see if there is any objection to a rename
    // of a node from or to the provided value.  If no objection then the return
    // value is true and the rename goes thru. If
    // false, it simply discards the rename action.  If any user feedback is desired,
    // Your implementation can provide that before returning the boolean.
    default boolean allowRenameFrom(DefaultMutableTreeNode theNode) {
        return true;
    }

    default boolean allowRenameTo(String theNewName) {
        return true;
    }

    // Called by the TreeBranchEditor to determine whether or not to provide a 'Delete'
    // button for a choice.  If false then the choice will have no Delete button.
    default boolean deleteAllowed(String theSelection) {
        return true;
    }

    // The textual list of choices for all items that the user might select for
    // inclusion in the final Branch.  Usually includes everything that is already
    // in the branch to edit, and any available but still unselected choices.  If
    // your implementation returns a null, the editor will default to using the
    // nodes of the provided branch.
    ArrayList getChoices();


    // This method will return the node with the specified name.  It does a breadth-first search and
    // returns the first one that it finds, so if a user has named a deeper node the same as the one
    // we're searching for it will not interfere with the one that was requested.  If for some reason
    // a lower one is desired then make the call with 'theRoot' referencing the deepest starting branch
    // rather than the root of the entire tree, especially since it goes no deeper than the first level.
    @SuppressWarnings("rawtypes")
    static DefaultMutableTreeNode getNodeByName(DefaultMutableTreeNode theRoot, String theName) {
        DefaultMutableTreeNode dmtn = null;
        Enumeration bfe = theRoot.breadthFirstEnumeration();

        while (bfe.hasMoreElements()) {
            DefaultMutableTreeNode nextNode = (DefaultMutableTreeNode) bfe.nextElement();
            if (nextNode.toString().equals(theName)) {
                dmtn = nextNode;
                break;
            }
        }
        return dmtn;
    }

    // Called by the TreeBranchEditor to determine whether or not to allow a drop of
    // one leaf onto another, thereby making the drop target into a 'parent'.  If
    // false then the drop is not allowed.  Drops 'between' leaves for the purposes
    // of reordering the nodes of the branch are always allowed, as are drops onto
    // nodes that are already a parent.
    default boolean makeParents() {
        return false;
    }

    // The handler for the 'Apply' button.
    boolean doApply(MutableTreeNode mtn, ArrayList<NodeChange> changes);

    // What text appears on the 'Remove' button.  Ex:  'Delete', 'Remove', or
    // something else.  If your implementation returns a null, the editor will
    // use a default of 'X'.
    default String getDeleteCommand() {
        return null;
    }

    // Provides a way for a Helper to override special renaming cases.
    default String getRenameToString() {
        return null;
    }
}
