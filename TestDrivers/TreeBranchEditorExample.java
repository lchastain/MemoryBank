import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.MutableTreeNode;
import java.awt.event.ActionEvent;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Vector;

class TBH implements TreeBranchHelper {
    @Override
    public boolean isNameValid(String theName) {
        // The 'if' is just to get some usage for 'theName'; not really needed.
        if(!theName.equals("badbadbad")) return true;
        return true;
    }

    @Override
    public String reason() {
        return ""; //"Just because";
    }

    @Override
    public boolean doApply(MutableTreeNode mtn, ArrayList newChoices) {
        return true;
    }

    @Override
    public String getDeleteCommand() {
        return "X";
    }

    @Override
    public boolean deleteAllowed() { return true; }

    @Override
    public boolean makeParents() {
        return true;
    }

    @Override
    public ArrayList<String> getChoices() {
        ArrayList<String> theLeafChoices = new ArrayList<String> (Arrays.asList(
                "this", "that", "these", "those",
                "we", "you", "us", "they", "them",
                "what", "why", "when", "where", "who",
                "whom", "whomever", "how"
        ));
        return theLeafChoices;
    }
}

public class TreeBranchEditorExample {
    static final long serialVersionUID = -1L;

    public static void main(String args[]) {
        //URL myURL = TreeBranchEditorExample.class.getClassLoader().getResource("logback.xml");
        //System.out.println(myURL);
        try
        {
            //UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel");
            //UIManager.setLookAndFeel("javax.swing.plaf.metal.MetalLookAndFeel");
        }
        catch (Exception ex)
        {
            System.out.println("Failed loading L&F: " + ex.getMessage());
        }

        final Vector<String> selections = new Vector<String>(2);


        // Make a 'test' Tree Branch
        DefaultMutableTreeNode dmtn = new DefaultMutableTreeNode("An Example Branch");
        dmtn.add(new DefaultMutableTreeNode("that"));
        dmtn.add(new DefaultMutableTreeNode("these"));
        dmtn.add(new DefaultMutableTreeNode("when"));
        TreeBranchEditor tbe = new TreeBranchEditor(dmtn, new TBH());

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


/**/