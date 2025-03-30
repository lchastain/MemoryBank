import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;

public class MilestoneNoteGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(MilestoneNoteGroupPanel.class);
    private static final int DEFAULT_PAGE_SIZE = 25;

    JComponent theHeader;
    GroupProperties groupProperties;

    public MilestoneNoteGroupPanel(GroupInfo groupInfo, int pageSize) {
        super(pageSize);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty NoteGroup.
        myNoteGroup.myNoteGroupPanel = this;
        if (groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        int lastPage = theNotePager.getHighestPage();
        theNotePager.reset(lastPage); // Without this, the pager appears and shows 'page 0 of 0'.
        // But with it, if there are fewer than 2 pages, it remains non-visible.
        loadNotesPanel(lastPage);

        groupProperties = myNoteGroup.getGroupProperties();
        buildPanelContent(); // Content other than the groupDataVector
    } // end constructor

    public MilestoneNoteGroupPanel(GroupInfo groupInfo) {
        this(groupInfo, DEFAULT_PAGE_SIZE);
    }


    JComponent buildHeader() {
        // The Header Row -   The Title and a Pager control
        JPanel heading = new JPanel(new BorderLayout());
        heading.setBackground(Color.blue);
        String goalName = myNoteGroup.myProperties.getGroupName();
        JLabel titleLabel = new JLabel(goalName);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(Font.decode("Serif-bold-20"));

        heading.add(titleLabel, "Center");

        // Set the pager's background to the same color as this row,
        //   since other items on this row make the row slightly taller
        //   than the pager control (pager goes to the top, background shows thru at the bottom).
        theNotePager.setBackground(heading.getBackground());
        heading.add(theNotePager, "East");

        return heading;
    } // end buildHeader


    // Called from within the constructor to create and place the visual components of the panel.
    private void buildPanelContent() {
        log.info("Building components for a MilestoneNoteGroupPanel");
        theHeader = buildHeader();
        add(theHeader, BorderLayout.NORTH);  // Adds to theBasePanel
    }



    // Returns a MilestoneNoteComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    @Override
    public MilestoneNoteComponent getNoteComponent(int i) {
        return (MilestoneNoteComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    @Override
    JComponent makeNewNoteComponent(int i) {
        MilestoneNoteComponent noteComponent = new MilestoneNoteComponent(this, i);
        noteComponent.setVisible(false);
        return noteComponent;
    } // end makeNewNoteComponent


//    public static void main(String[] args) {
//        MemoryBank.debug = true;
//        MemoryBank.userEmail = "lex@doughmain.net";
//
//        MilestoneNoteGroupPanel theNoteGroupPanel;
//        JFrame testFrame = new JFrame();
//
//        testFrame.setTitle("Parentless MilestoneNoteGroupPanel Driver");
//        theNoteGroupPanel = new MilestoneNoteGroupPanel(new GroupInfo("Retire", GroupType.MILESTONE), 8);
//
//        testFrame.addWindowListener(new WindowAdapter() {
//            public void windowClosing(WindowEvent we) {
//                theNoteGroupPanel.preClosePanel();
//                System.exit(0);
//            }
//        });
//
//        // Needed to override the 'metal' L&F for Swing components.
//        String thePlaf = "com.sun.java.swing.plaf.windows.WindowsClassicLookAndFeel";
//        System.out.println("Setting Pluggable Look & Feel to: " + thePlaf);
//        String laf = UIManager.getSystemLookAndFeelClassName();
//        try {
//            UIManager.setLookAndFeel(thePlaf);
//        } catch (Exception ignored) {
//        }    // end try/catch
//        SwingUtilities.updateComponentTreeUI(theNoteGroupPanel.theBasePanel);
//
//        testFrame.getContentPane().add(theNoteGroupPanel.theBasePanel, "Center");
//        testFrame.pack();
//        testFrame.setSize(new Dimension(680, 600));
//        testFrame.setVisible(true);
//        testFrame.setLocationRelativeTo(null);
//    }

}
