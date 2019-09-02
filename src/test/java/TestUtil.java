import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Enumeration;

class TestUtil implements Notifier {
    private String theAnswer;
    private int notifyCount;
    private Method theMethod;
    private Object param1;

    TestUtil() {
        theAnswer = "No comment";
        notifyCount = 0;
        theMethod = null;
        param1 = null;
    }

    @Override
    public void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
        notifyCount++;
        System.out.println(title + ":  " + message);
    }

    @Override
    public int showOptionDialog(Component parentComponent, Object message, String title, int optionType, int messageType, Icon icon, Object[] options, Object initialValue) {
        notifyCount++;
        System.out.println(title + ":  " + message);
        return JOptionPane.OK_OPTION;
    }

    @Override
    public String showInputDialog(Component parentComponent, Object message, String title, int messageType) {
        notifyCount++;
        System.out.println("(Input dialog) " + title + ":  " + message);
        System.out.println("  using supplied answer: " + theAnswer);
        return theAnswer;
    }

    @Override
    public int showConfirmDialog(Component parentComponent, Object message, String title, int optionType) {
        notifyCount++;
        System.out.println("(Confirm dialog) " + title + ":  " + message);
        if (theMethod != null) {
            try {
                if(param1 == null) {
                    theMethod.invoke(message);  // See TodoNoteGroupTest.testSetOptions
                } else {
                    theMethod.invoke(message, param1);
                }
            } catch (IllegalAccessException | InvocationTargetException ee) {
                ee.printStackTrace();
            }
        }
        return 1;
    }


    int getNotifyCount() { return notifyCount; }
    void setNotifyCount(int n) { notifyCount = n; }

    // When the 'message' is really an Object reference, this method allows a test
    // to change the Object content.  You need to send in a method that is available
    // to that Object.
    void setMethod(Method newMethod) {
        theMethod = newMethod;
    }

    void setParam1(Object o) {
        param1 = o;
    }

    // A calling context will set this 'answer' that will be used by the input dialog.
    void setTheAnswer(String s) {
        theAnswer = s;
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