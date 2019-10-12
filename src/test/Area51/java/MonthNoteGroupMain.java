import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MonthNoteGroupMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("g01@doughmain.net");

        JFrame testFrame = new JFrame("MonthNoteGroup Driver");

        MonthNoteGroup mng = new MonthNoteGroup();

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.out.println("MonthNoteGroup selection choice is: " + mng.getChoice());
                System.exit(0);
            }
        });

        // Needed to override the 'metal' L&F for Swing components.
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception ignored) {
        }    // end try/catch
        SwingUtilities.updateComponentTreeUI(mng);

        testFrame.getContentPane().add(mng, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(600, 500));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
