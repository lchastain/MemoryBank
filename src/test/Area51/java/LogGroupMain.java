import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LogGroupMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("lex@doughmain.net");

        LogNoteGroupPanel theLogNoteGroup;
        JFrame testFrame = new JFrame();

        testFrame.setTitle("Parentless LogPanel Driver");
        theLogNoteGroup = new LogNoteGroupPanel(new GroupInfo("Wooden Events", GroupType.LOG), 8);

//        testFrame.setTitle("LogPanel Driver for a To Do List");
//        theLogNoteGroup = new LogGroupPanel(new GroupInfo("DoIT", GroupType.TODO_LOG));

//        testFrame.setTitle("LogPanel Driver for a Goal");
//        theLogNoteGroup = new LogGroupPanel(new GroupInfo("Retire", GroupType.GOAL_LOG));

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                theLogNoteGroup.preClosePanel();
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
        SwingUtilities.updateComponentTreeUI(theLogNoteGroup.theBasePanel);

        testFrame.getContentPane().add(theLogNoteGroup.theBasePanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(680, 600));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
