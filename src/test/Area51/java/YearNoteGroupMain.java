import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class YearNoteGroupMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.userEmail = "g01@doughmain.net";

        JFrame testFrame = new JFrame("YearNoteGroup Driver");

        YearNoteGroupPanel yng = new YearNoteGroupPanel();

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.out.println("MonthNoteGroup selection choice is: " + yng.getDate());
                System.exit(0);
            }
        });

        // Needed to override the 'metal' L&F for Swing components.
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception ignored) {
        }    // end try/catch
        SwingUtilities.updateComponentTreeUI(yng.theBasePanel);

        testFrame.getContentPane().add(yng.theBasePanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(600, 500));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
