import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GoalPanelMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("g01@doughmain.net");

        JFrame testFrame = new JFrame("Goal Panel Driver");

        GoalPanel theGoalPanel = new GoalPanel("finances");

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
        SwingUtilities.updateComponentTreeUI(theGoalPanel.theBasePanel);

        testFrame.getContentPane().add(theGoalPanel.theBasePanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(540, 450));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
