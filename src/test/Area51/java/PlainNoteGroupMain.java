import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class PlainNoteGroupMain {

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("lex@doughmain.net");
        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
        TestUtil.getTheAppTreePanel();

        PlainNoteGroupPanel thePlainNoteGroup;
        JFrame testFrame = new JFrame();

        testFrame.setTitle("Parentless PlainNoteGroupPanel Driver");
        thePlainNoteGroup = new PlainNoteGroupPanel(new GroupInfo("Vanilla Sky", GroupType.NOTES), 8);

//        testFrame.setTitle("Parentless PlainNoteGroupPanel Driver for a Goal");
//        thePlainNoteGroup = new PlainNoteGroupPanel(new GroupInfo("Retire", GroupType.GOAL_NOTES));

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                thePlainNoteGroup.preClosePanel();
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
        SwingUtilities.updateComponentTreeUI(thePlainNoteGroup.theBasePanel);

        testFrame.getContentPane().add(thePlainNoteGroup.theBasePanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(680, 600));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
