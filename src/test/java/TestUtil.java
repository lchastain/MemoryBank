import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.util.Enumeration;

class TestUtil implements Notifier {

    @Override
    public void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
        System.out.println(title + ":  " + message);
    }

    @Override
    public int showOptionDialog(Component parentComponent, Object message, String title, int optionType, int messageType, Icon icon, Object[] options, Object initialValue) {
        System.out.println(title + ":  " + message);
        return JOptionPane.OK_OPTION;
    }



    @SuppressWarnings("rawtypes")
    static DefaultMutableTreeNode getTreeNodeForString(DefaultMutableTreeNode theRoot, String theString) {
        DefaultMutableTreeNode dmtn = null;
        DefaultMutableTreeNode nextNode;
        Enumeration bfe = theRoot.breadthFirstEnumeration();

        while (bfe.hasMoreElements()) {
            nextNode = (DefaultMutableTreeNode) bfe.nextElement();
            if (nextNode.toString().equals(theString)) {
                dmtn = nextNode;
                break;
            }
        }
        return dmtn;
    }

}