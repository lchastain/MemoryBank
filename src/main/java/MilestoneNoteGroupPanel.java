import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class MilestoneNoteGroupPanel extends NoteGroupPanel implements IconKeeper {
    private static final Logger log = LoggerFactory.getLogger(MilestoneNoteGroupPanel.class);
    private static final int DEFAULT_PAGE_SIZE = 25;
    static String userInfo;

    JComponent theHeader;
    private MilestoneNoteComponent milestoneNoteComponent; // The currently selected one.
    GroupProperties groupProperties;
    transient NoteGroupPanel parentNoteGroupPanel;


    static {
        MemoryBank.trace();
        userInfo = "Goal Narrative:  If the title alone does not convey the full intent of your Goal then here you ";
        userInfo += "can describe more precisely what it is that you wish to acquire or accomplish.  Together with ";
        userInfo += "the milestones that lay out the discrete individual steps, this becomes your Plan.";
        userInfo += "\n\nIf you need to comment on developments or issues related to this goal, make one or more ";
        userInfo += "log entries.  The most recent log entry goes to the top but you can manually reorder after ";
        userInfo += "that if needed. ";
    } // end static


    public MilestoneNoteGroupPanel(GroupInfo groupInfo, int pageSize) {
        super(pageSize);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty NoteGroup.
        myNoteGroup.myNoteGroupPanel = this;
        if (groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        loadNotesPanel();

        groupProperties = myNoteGroup.getGroupProperties();

        theNotePager.reset(1); // Without this, the pager appears and shows 'page 0 of 0'.
        // But with it, if there are fewer than 2 pages, it remains non-visible.

        buildPanelContent(); // Content other than the groupDataVector
    } // end constructor

    public MilestoneNoteGroupPanel(GroupInfo groupInfo) {
        this(groupInfo, DEFAULT_PAGE_SIZE);
    }

    public MilestoneNoteGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.MILESTONE));
    }


    @SuppressWarnings({"rawtypes"})
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
    @SuppressWarnings({"rawtypes"})
    private void buildPanelContent() {
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
    JComponent makeNewNote(int i) {
        MilestoneNoteComponent noteComponent = new MilestoneNoteComponent(this, i);
        noteComponent.setVisible(false);
        return noteComponent;
    } // end makeNewNote


    @Override
    public AppIcon getDefaultIcon() {
        return null;
    }

    @Override
    public void setDefaultIcon(AppIcon li) {

    }

    public static void main(String[] args) {
        MemoryBank.debug = true;
        MemoryBank.setUserDataHome("lex@doughmain.net");

        MilestoneNoteGroupPanel theNoteGroupPanel;
        JFrame testFrame = new JFrame();

        testFrame.setTitle("Parentless MilestoneNoteGroupPanel Driver");
        theNoteGroupPanel = new MilestoneNoteGroupPanel(new GroupInfo("Retire", GroupType.MILESTONE), 8);

        testFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                theNoteGroupPanel.preClosePanel();
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
        SwingUtilities.updateComponentTreeUI(theNoteGroupPanel.theBasePanel);

        testFrame.getContentPane().add(theNoteGroupPanel.theBasePanel, "Center");
        testFrame.pack();
        testFrame.setSize(new Dimension(680, 600));
        testFrame.setVisible(true);
        testFrame.setLocationRelativeTo(null);
    }

}
