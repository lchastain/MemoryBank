import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GoalGroupMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("g01@doughmain.net");

        JFrame testFrame = new JFrame("Goal Panel Driver");

        GoalGroup theGoalGroup = new GoalGroup("finances");

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        // Needed to override the 'metal' L&F for Swing components.
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(laf);
        } catch (Exception ignored) {
        }    // end try/catch
        SwingUtilities.updateComponentTreeUI(theGoalGroup.theBasePanel);

        testFrame.getContentPane().add(theGoalGroup.theBasePanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(540, 450));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
