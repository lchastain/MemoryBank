import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.util.ArrayList;
import java.util.Arrays;

class EditorHelper implements TreeBranchHelper {
    @Override
    public boolean allowRenameFrom(DefaultMutableTreeNode theNode) {
        String theName = theNode.toString();
        if(theName.equals("An Example Branch")) {
            JOptionPane.showMessageDialog(new JFrame(), "You can't rename the root!");
            return false;
        }
        return true;
    }

    @Override
    public boolean allowRenameTo(String theName) {
        if(theName.equals("thine")) {
            JOptionPane.showMessageDialog(new JFrame(), "That name is not allowed!");
            return false;
        }
        return true;
    }

    @Override
    public void doApply(MutableTreeNode mtn, ArrayList changes) {
        for(Object nco: changes) {
            System.out.println(nco.toString());
        }
        System.exit(0);
    }

    @Override
    public String getDeleteCommand() {
        return "X";
    }

    @Override
    public boolean deletesAllowed() { return true; }

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


public class TreeBranchEditorMain {

    public static void main(String[] args) {
        try
        {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
        }
        catch (Exception ex)
        {
            System.out.println("Failed loading L&F: " + ex.getMessage());
        }

        // Make a 'test' Tree Branch
        DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode("An Example Branch");
        dmtn.add(new DefaultMutableTreeNode("that"));
        dmtn.add(new DefaultMutableTreeNode("these"));
        dmtn.add(new DefaultMutableTreeNode("when"));
        TreeBranchEditor tbe = new TreeBranchEditor(dmtn, new EditorHelper());

        // Make the frame and add ourselves to it.
        JFrame testFrame = new JFrame("TreeBranchEditorExample Test");
        testFrame.getContentPane().add(tbe);
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Center the Frame in the available screen area
        testFrame.pack();
        testFrame.setSize(500, 550);
        testFrame.setLocationRelativeTo(null);

        testFrame.setVisible(true);

    } // end main

} // end class TreeBranchEditorExample


