import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

// This class provides a stand-alone example of usage of the TimeChooser.
// For production usage it can be displayed in a JOptionPane and the
// chooser can be queried for settings after the dialog has closed.

public class TimeChooserMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;

        Frame tcFrame = new Frame("TimeChooser Driver");

        MemoryBank.appOpts.timeFormat = AppOptions.TimeFormat.MILITARY; // Change this,  to test.
        TimeChooser timeChooser = new TimeChooser();

        tcFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.out.println("TimeChooser selection choice is: " + timeChooser.getChoice());
                System.exit(0);
            }
        });

        // Needed to override the 'metal' L&F for Swing components.
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception ignored) {
        }    // end try/catch
        SwingUtilities.updateComponentTreeUI(timeChooser);

        timeChooser.setShowSeconds();
        tcFrame.add(timeChooser);
        tcFrame.pack();
        tcFrame.setVisible(true);
        tcFrame.setLocationRelativeTo(null);
    }

}
