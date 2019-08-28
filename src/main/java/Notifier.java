import javax.swing.*;
import java.awt.*;

// This interface provides a wrapper for a JOptionPane as its default methods that will be used by
//   the main application but the Test methods will replace the default methods here so that tests
//   can run without popping up dialogs that require user intervention.
public interface Notifier {

   default void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
       JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
   }

   default int showOptionDialog(Component parentComponent, Object message, String title, int optionType, int messageType, Icon icon, Object[] options, Object initialValue) {
       return JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue);
   }

}

