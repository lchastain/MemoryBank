import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;


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
        TreeBranchEditor tbe = new TreeBranchEditor(null, dmtn, new EditorHelper());

        // Make the frame and add ourselves to it.
        JFrame testFrame = new JFrame("TreeBranchEditor Driver");
        testFrame.getContentPane().add(tbe);
        testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        // Center the Frame in the available screen area
        testFrame.pack();
        testFrame.setSize(580, 500);
        testFrame.setLocationRelativeTo(null);

        testFrame.setVisible(true);

    } // end main

} // end class TreeBranchEditorMain


