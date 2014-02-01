import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;

public interface TreeBranchHelper {
    public boolean isNameValid(String theName);
    public String reason();
    public ArrayList<String> getChoices();
    public boolean deleteAllowed();
    public boolean makeParents();
    public boolean doApply(MutableTreeNode mtn, ArrayList newChoices);
    public String getDeleteCommand();
}
