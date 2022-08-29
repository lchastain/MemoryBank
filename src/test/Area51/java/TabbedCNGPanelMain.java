// This 'main' for the class must reside in the test area, to grant it access to the TestUtil,
//   which is needed to instantiate an AppTreePanel, which is used by the NoteGroupPanels.
public class TabbedCNGPanelMain {

//    public static void main(String[] args) {
//        MemoryBank.debug = true;
//        MemoryBank.userEmail = "lex@doughmain.net");
//        MemoryBank.dataAccessor = DataAccessor.getDataAccessor(DataAccessor.AccessType.FILE);
//        JFrame testFrame = new JFrame("Tabbed Calendar Note Group Panel Driver");
//
//        // We don't use the AppTreePanel directly from here, but needs to have been instantiated
//        // because the Panels wrapped by the TabbedCalendarNoteGroupPanel will need it.
//        TestUtil.getTheAppTreePanel();
//
//        TabbedCalendarNoteGroupPanel tcngPanel = new TabbedCalendarNoteGroupPanel();
//
//        testFrame.addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent we) {
//                System.exit(0);
//            }
//        });
//
//        // Needed to override the 'metal' L&F for Swing components.
//        String thePlaf = "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
//        System.out.println("Setting Pluggable Look & Feel to: " + thePlaf);
//        //String laf = UIManager.getSystemLookAndFeelClassName();
//        try {
//            UIManager.setLookAndFeel(thePlaf);
//        } catch (Exception ignored) {
//        }    // end try/catch
//        SwingUtilities.updateComponentTreeUI(tcngPanel.theBasePanel);
//
//        testFrame.getContentPane().add(tcngPanel.theBasePanel, "Center");
//        testFrame.pack();
//        testFrame.setSize(new Dimension(680, 600));
//        testFrame.setVisible(true);
//        testFrame.setLocationRelativeTo(null);
//    }
}
