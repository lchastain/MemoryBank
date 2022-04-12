import javax.swing.*;
import java.awt.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;

class TestUtil implements Notifier, SubSystem {
    private String theAnswerString;
    private int theAnswerInt;
    private int notifyCount;
    private Method theMethod;
    private Object theMessage;
    private AppTreePanel theAppTreePanel;

    TestUtil() {
        theAnswerString = "No comment";
        theAnswerInt = JOptionPane.OK_OPTION; // also YES_OPTION, both == 0
        notifyCount = 0;
        theMethod = null;
        theMessage = null;
    }

    // A SubSystem method
    @Override
    public void exit(int status) {
        System.out.println("System.exit(" + status + ") was called!");
    }

    // There should only ever be ONE AppTreePanel in a JVM.
    static AppTreePanel getTheAppTreePanel() {
        return Objects.requireNonNullElseGet(AppTreePanel.theInstance, () -> {
            AppTreePanel atp = new AppTreePanel(new JFrame(), MemoryBank.appOpts);
            atp.optionPane = new TestUtil();
            return atp;
        });
    }


    // A Notifier method
    @Override
    public int showConfirmDialog(Component parentComponent, Object message, String title, int optionType) {
        notifyCount++;
        System.out.println("(Confirm dialog) " + title + ":  " + message);
        if (theMethod != null) {
            try {
                if (theMessage == null) {
                    theMethod.invoke(message);  // See TodoNoteGroupTest.testSetOptions
                } else {
                    theMethod.invoke(message, theMessage);
                }
            } catch (IllegalAccessException | InvocationTargetException ee) {
                ee.printStackTrace();
            }
        }
        return theAnswerInt;
    }


    @Override
    public int showConfirmDialog(Component parentComponent, Object message, String title, int optionType, int messageType) {
        return showConfirmDialog(parentComponent, message, title, optionType);
    }


    @Override
    public void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
        notifyCount++;
        System.out.println(title + ":  " + message);
    }

    @Override
    public void showMessageDialog(Component parentComponent, Object message) {
        notifyCount++;
        System.out.println("Message:  " + message);
    }

    @Override
    public int showOptionDialog(Component parentComponent, Object message, String title, int optionType, int messageType, Icon icon, Object[] options, Object initialValue) {
        notifyCount++;
        theMessage = message;
        System.out.println(title + ":  " + message);
        return theAnswerInt;
    }

    @Override
    public String showInputDialog(Component parentComponent, Object message, String title, int messageType) throws HeadlessException {
        notifyCount++;
        System.out.println("(Input dialog) " + title + ":  " + message);
        System.out.println("  using supplied answer: " + theAnswerString);
        return theAnswerString;
    }

    @Override
    public String showInputDialog(Component parentComponent, Object message, String title, int messageType,
                                  Icon icon, Object[] selectionValues, Object initialSelectionValue) {
        notifyCount++;
        System.out.println("(Input dialog) " + title + ":  " + message);
        System.out.println("  using supplied answer: " + theAnswerString);
        return theAnswerString;
    }

    Object getTheMessage() {
        return theMessage;
    }

    int getNotifyCount() {
        return notifyCount;
    }

    void setNotifyCount(int n) {
        notifyCount = n;
    }

    // When the 'message' is really an Object reference, this method allows a test
    // to change the Object behavior.  You need to send in a method that is available
    // to that Object.
    void setTheMethod(Method theNewMethod) {
        theMethod = theNewMethod;
    }

    void setTheMessage(Object o) {
        theMessage = o;
    }

    // A calling context will set this 'answer' that will be used by the input dialog.
    void setTheAnswerInt(int i) {
        theAnswerInt = i;
    }


    // A calling context will set this 'answer' that will be used by the input dialog.
    void setTheAnswerString(String s) {
        theAnswerString = s;
    }



}