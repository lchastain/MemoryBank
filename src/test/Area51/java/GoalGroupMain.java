import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GoalGroupMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("lex@doughmain.net");

        JFrame testFrame = new JFrame("Goal Panel Driver");

        GoalGroupPanel theGoalGroup = new GoalGroupPanel(new GroupInfo("Retire", GroupType.GOALS), 8);

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                theGoalGroup.preClosePanel();
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
        testFrame.setSize(new Dimension(680, 600));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
