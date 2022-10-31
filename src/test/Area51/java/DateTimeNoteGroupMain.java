import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class DateTimeNoteGroupMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.userEmail = "lex@doughmain.net";
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        TestUtil.getTheAppTreePanel();

        DateTimeNoteGroupPanel theNoteGroup;
        JFrame testFrame = new JFrame();

        testFrame.setTitle("Parentless DateTimeNoteGroupPanel Driver");
        theNoteGroup = new DateTimeNoteGroupPanel("When What");

//        testFrame.setTitle("Parentless PlainNoteGroupPanel Driver for a Goal");
//        thePlainNoteGroup = new PlainNoteGroupPanel(new GroupInfo("Retire", GroupType.GOAL_NOTES));

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                theNoteGroup.preClosePanel();
                System.exit(0);
            }
        });

        // Needed to override the 'metal' L&F for Swing components.
        String thePlaf = "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
        System.out.println("Setting Pluggable Look & Feel to: " + thePlaf);
        try {
            UIManager.setLookAndFeel(thePlaf);
        } catch (Exception ignored) {
        }    // end try/catch
        SwingUtilities.updateComponentTreeUI(theNoteGroup.theBasePanel);

        testFrame.getContentPane().add(theNoteGroup.theBasePanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(680, 600));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
