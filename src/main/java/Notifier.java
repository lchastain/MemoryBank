import javax.swing.*;
import java.awt.*;

import static javax.swing.JOptionPane.QUESTION_MESSAGE;

// This interface provides a wrapper for a JOptionPane as its default methods that will be used by
//   the main application but the Test methods will replace the default methods here so that tests
//   can run without popping up dialogs that require user intervention.
public interface Notifier {

   default void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
       JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
   }

    default void showMessageDialog(Component parentComponent, Object message) {
        JOptionPane.showMessageDialog(parentComponent, message);
    }

    default int showOptionDialog(Component parentComponent, Object message, String title, int optionType, int messageType, Icon icon, Object[] options, Object initialValue) {
       return JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue);
   }

    default String showInputDialog(Component parentComponent, Object message, String title, int messageType)
            throws HeadlessException {
        return (String) JOptionPane.showInputDialog(parentComponent, message, title, messageType, null, null, null);
    }

    default int showConfirmDialog(Component parentComponent, Object message, String title, int optionType)
            throws HeadlessException {
        return JOptionPane.showConfirmDialog(parentComponent, message, title, optionType, QUESTION_MESSAGE);
    }


}

