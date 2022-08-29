import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class GoalGroupMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.userEmail = "lex@doughmain.net";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        TestUtil.getTheAppTreePanel();

        JFrame testFrame = new JFrame("Goal Panel Driver");

        GoalGroupPanel theGoalGroup = new GoalGroupPanel(new GroupInfo("Retire", GroupType.GOALS));

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                theGoalGroup.preClosePanel();
                System.exit(0);
            }
        });

        // Needed to override the 'metal' L&F for Swing components.
        String thePlaf = "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
        System.out.println("Setting Pluggable Look & Feel to: " + thePlaf);
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(thePlaf);
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
