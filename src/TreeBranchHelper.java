import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;

public interface TreeBranchHelper {
    // Called by the TreeBranchEditor to see if there is any objection to a rename
    // of a node from or to the provided value.  If false, it simply discards the
    // rename action.  Your implementation is responsible for providing any user feedback.
    public boolean allowRenameFrom(String theName);
    public boolean allowRenameTo(String theName);

    // The textual list of choices for all items that the user might select for
    // inclusion in the final Branch.
    public ArrayList<String> getChoices();

    // Called by the TreeBranchEditor to determine whether or not to provide a 'Delete'
    // button for each of the choices.  If false then no choice will have a Delete button.
    public boolean deletesAllowed();

    // Called by the TreeBranchEditor to determine whether or not to allow a drop of
    // one leaf onto another, thereby making the drop target into a 'parent'.  If
    // false then the drop is not allowed.
    public boolean makeParents();

    // The handler for an accepted Tree edit session.
    public void doApply(MutableTreeNode mtn, ArrayList newChoices, ArrayList changes);

    // What text appears on the 'Delete' button.  Ex:  'Delete', 'X', or something else.
    public String getDeleteCommand();
}
