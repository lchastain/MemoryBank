import javax.swing.*;
import java.awt.*;

// This interface provides a wrapper for a JOptionPane as its default methods that will be used by
//   the main application but the Test methods will replace the default methods here so that tests
//   can run without popping up dialogs that require user intervention.
public interface Notifier {

   default void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
       JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
   }

//    default void showMessageDialog(Component parentComponent, Object message, String title, int messageType) {
//
//        Method m = null;
//        Class<JOptionPane> c = JOptionPane.class;
//        try {
//            m = c.getMethod("showMessageDialog", Component.class, Object.class, String.class, int.class);
//        } catch (NoSuchMethodException e) {
//            e.printStackTrace();
//        }
//
//        if(m != null) {
//            try {
//                m.invoke(this, parentComponent, message, title, messageType);
//            } catch (IllegalAccessException | InvocationTargetException ee) {
//                ee.printStackTrace();
//            }
//        }
//    }
}

