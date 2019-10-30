import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

// A panel that shows a list of the Pluggable Look and Feels that are currently
// installed on the system, and allows the user to make a new selection.  The
// calling context must make the change, if any.
public class PlafSelectionPanel extends JPanel implements ActionListener {
    static final long serialVersionUID = 1L;

    private String selectedPlaf;
    private ButtonGroup bg;
    private HashMap<String, String> hm;

    private PlafSelectionPanel() {
        UIManager.LookAndFeelInfo[] lafiArray = UIManager.getInstalledLookAndFeels();
        // A null or zero count array result does not seem possible.
        // Defer handling of those cases until proven otherwise.

        String currentLaf = UIManager.getLookAndFeel().getName();

        bg = new ButtonGroup();
        hm = new HashMap<>();
        setLayout(new GridLayout(0, 1));

        for (UIManager.LookAndFeelInfo lafi : lafiArray) {
            //System.out.print(lafi.getClassName() + "\t");
            //System.out.println(lafi.getName());
            String theName = lafi.getName();
            String theClassName = lafi.getClassName();
            hm.put(theName, theClassName);
            JRadioButton jrb = new JRadioButton(theName);
            jrb.addActionListener(this);
            jrb.setActionCommand(theName);
            if (theName.equals(currentLaf)) jrb.setSelected(true);
            bg.add(jrb);
            add(jrb);
        }
    }

    private String getSelectedPlaf() {
        return selectedPlaf;
    }

    public void actionPerformed(ActionEvent e) {
        ButtonModel bm = bg.getSelection();
        String t = "Not selected";
        if (bm != null) t = bm.getActionCommand();
        //System.out.println(t);
        //System.out.println(hm.get(t));
        selectedPlaf = hm.get(t);
    }

    // Thursday, 1 August 2019
    // This method and the PlafSelectionPanel are being deprecated for use by the MemoryBank app.
    // But the concept is still too cool to just throw away so the deactivated code stays here, for now.
    //   removed from the AppMenuBar, View menu:    menuView.add(new JMenuItem("Set Look and Feel..."));
    //   removed from the AppMenuBar, Event menu:   menuViewEvent.add(new JMenuItem("Set Look and Feel..."));
    //   removed from the AppMenuBar, View Date menu:    menuViewDate.add(new JMenuItem("Set Look and Feel..."));
    //   removed from AppTreePanel, the HandleMenuBar method:
    //      else if (what.equals("Set Look and Feel...")) showPlafDialog(); Content of that method is now in main().

    // More notes:  The JFrame below was just instantiated to clear errors in the original code.  The panel is still
    // not working as it did originally; this file would need more work, for that.  The work needed:  do away with a
    // JOptionPane and move the panel into the currently-unused JFrame.  Then make a handler for closing the frame,
    // so the result could be seen because currently it goes away but the app remains 'running'.

    // Unlike other unused 'mains' here, this one calls up a panel that is also unused, so there is no need to keep
    // them in separate class files.
    public static void main(String[] args) {
        MemoryBank.debug = true;
        JFrame theFrame = new JFrame("PlafSelectionPanel Driver");

        PlafSelectionPanel pep = new PlafSelectionPanel();
        int doit = JOptionPane.showConfirmDialog(
                theFrame, pep,
                "Select a new Look and Feel", JOptionPane.OK_CANCEL_OPTION);

        if (doit == -1) return; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return;

        // This is where we would set the options...
        //boolean blnOrigShowPriority = myVars.showPriority;
        //myVars = to.getValues();

        try {
            UIDefaults uidefaults = UIManager.getLookAndFeelDefaults();
            UIManager.setLookAndFeel(pep.getSelectedPlaf());
            //appOpts.thePlaf = pep.getSelectedPlaf(); // this was a String type.
            SwingUtilities.updateComponentTreeUI(theFrame);
            // It looks like a nullPointerException stack trace is being printed as a result of
            // the above command, which seems to complete successfully anyway, after that.
            // This exception is not getting trapped by the catch section below.
            // I think it may be happening because of my implementation of the custom
            // scrollpane (need to find/review that code).  Seems to go thru without any other
            // trouble, tho, so we may be able to ignore this indefinitely.
            System.out.println("updatedComponentTreeUI"); // Shows that the above succeeded.
        } catch (Exception e) {
            System.out.println("Exception: " + e.getMessage());
            //e.printStackTrace();
        }



//        MemoryBank.setUserDataHome("g01@doughmain.net");
//
//        JFrame testFrame = new JFrame("RecurrencePanel Driver");
//
//        RecurrencePanel theRecurrencePanel = new RecurrencePanel();
//
//        testFrame.addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent we) {
//                System.exit(0);
//            }
//        });
//
//        testFrame.getContentPane().add(theRecurrencePanel, "Center");
//        testFrame.pack();
//        testFrame.setSize(new Dimension(340, 450));
//        testFrame.setVisible(true);
//        testFrame.setLocationRelativeTo(null);
    }


}
