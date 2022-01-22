import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LogGroupMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("lex@doughmain.net");

        LogGroupPanel theLogNoteGroup;
        JFrame testFrame = new JFrame();

        testFrame.setTitle("Parentless LogPanel Driver");
        theLogNoteGroup = new LogGroupPanel("Wooden Events");

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
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(laf);
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
