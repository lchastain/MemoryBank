import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class TimeChooserMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;

        Frame tcFrame = new Frame("TimeChooser test");

        MemoryBank.military = true;  // Change this,  to test.
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
