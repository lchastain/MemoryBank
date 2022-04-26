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
    // With so many tests needing instances of the AppTreePanel, this method is needed as a gatekeeper
    //   to ensure that there is always only one instance of the AppTreePanel at a time.
    static AppTreePanel getTheAppTreePanel() {
        AppTreePanel atp;
        atp = Objects.requireNonNullElseGet(AppTreePanel.theInstance, () -> new AppTreePanel(new JFrame(), MemoryBank.appOpts));
        atp.optionPane = new TestUtil(); // If the instance comes from here, its Notifier must be the TestUtil.
        return atp;
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

    // A utility function to retrieve a specified JMenuItem.
    // Calling contexts will perform a 'doClick' on the returned value.
    JMenuItem getMenuItem(String menu, String text) {
        JMenu jm;
        JMenuItem jmi = null;
        AppTreePanel appTreePanel = getTheAppTreePanel();

        int numMenus = appTreePanel.getAppMenuBar().getMenuCount();
        for (int i = 0; i < numMenus; i++) {
            jm = appTreePanel.getAppMenuBar().getMenu(i);
            if (jm == null) continue;
            //System.out.println("Menu: " + jm.getText());
            if (jm.getText().equals(menu)) {
                for (int j = 0; j < jm.getItemCount(); j++) {
                    jmi = jm.getItem(j);
                    if (jmi == null) continue; // Separator
                    //System.out.println("    Menu Item text: " + jmi.getText());
                    if (jmi.getText().equals(text)) return jmi;
                } // end for j
            }
        } // end for i

        return jmi;
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