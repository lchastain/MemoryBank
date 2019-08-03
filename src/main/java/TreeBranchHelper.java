import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;

public interface TreeBranchHelper {

    // Called by the TreeBranchEditor to see if there is any objection to a rename
    // of a node from or to the provided value.  If true, the rename goes thru. If
    // false, it simply discards the rename action.  If any user feedback is desired,
    // Your implementation can provide that before returning the boolean.
    public boolean allowRenameFrom(String theName);
    public boolean allowRenameTo(String theName);

    // The textual list of choices for all items that the user might select for
    // inclusion in the final Branch.  Usually includes everything in the branch
    // to edit, and more.  If your implementation returns a null, the editor will
    // default to using the nodes of the provided branch.
    public ArrayList<String> getChoices();

    // Called by the TreeBranchEditor to determine whether or not to provide a 'Delete'
    // button for each of the choices.  If false then no choice will have a Delete button.
    public boolean deletesAllowed();

    // Called by the TreeBranchEditor to determine whether or not to allow a drop of
    // one leaf onto another, thereby making the drop target into a 'parent'.  If
    // false then the drop is not allowed.  Drops 'between' leaves for the purposes
    // of reordering the nodes of the branch are always allowed, as are drops onto
    // nodes that are already a parent.
    public boolean makeParents();

    // The handler for the 'Apply' button.
    public void doApply(MutableTreeNode mtn, ArrayList<NodeChange> changes);

    // What text appears on the 'Remove' button.  Ex:  'Delete', 'Remove', or
    // something else.  If your implementation returns a null, the editor will
    // use a default of 'X'.
    public String getDeleteCommand();
}
