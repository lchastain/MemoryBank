import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;
import java.util.Arrays;

class EditorHelper implements BranchHelperInterface {
    @Override
    public boolean allowRenameFrom(DefaultMutableTreeNode theNode) {
        String theName = theNode.toString();
        if (theName.equals("An Example Branch")) {
            JOptionPane.showMessageDialog(new JFrame(), "You can't rename the root!");
            return false;
        }
        return true;
    }

    @Override
    public boolean allowRenameTo(String theName) {
        if (theName.equals("thine")) {
            JOptionPane.showMessageDialog(new JFrame(), "That name is not allowed!");
            return false;
        }
        return true;
    }

    @Override
    public boolean doApply(MutableTreeNode mtn, ArrayList<NodeChange> changes) {
        for (Object nco : changes) {
            System.out.println(nco.toString());
        }
        return true;
    }

    @Override
    public String getDeleteCommand() {
        return "X";
    }

    @Override
    public boolean deleteAllowed(String s) {
        return true;
    }

    @Override
    public boolean makeParents() {
        return true;
    }

    @Override
    public ArrayList<String> getChoices() {
        return new ArrayList<>(Arrays.asList(
                "this", "that", "these", "those",
                "we", "you", "us", "they", "them",
                "what", "why", "when", "where", "who",
                "whom", "whomever", "how"
        ));
    }
} // end class EditorHelper
