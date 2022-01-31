import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// This class is a grouping of three other panels - To Do, Log, and Milestones.
public class GoalGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(GoalGroupPanel.class);
    static String userInfo;

    private final GoalGroupProperties groupProperties;
    private TodoNoteGroupPanel theTodoNoteGroupPanel;
    private LogNoteGroupPanel theLogNoteGroupPanel;
    private MilestoneNoteGroupPanel theMilestoneNoteGroupPanel;
    private JPanel headingRow1; // Need to reference this when switching tabs.
    private JComponent theTodoCenterPanel;
    private JComponent theLogCenterPanel;
    private JComponent theMilestonesCenterPanel;
    private JComponent tmcPanel;  // Added or removed depending on Tab


    static {
        MemoryBank.trace();
        userInfo = "Goal Narrative:  If the title alone does not convey the full intent of your Goal then here you ";
        userInfo += "can describe more precisely what it is that you wish to acquire or accomplish.  Together with ";
        userInfo += "the milestones that lay out the discrete individual steps, this becomes your Plan.";
        userInfo += "\n\nIf you need to comment on developments or issues related to this goal, make one or more ";
        userInfo += "log entries.  The most recent log entry goes to the top but you can manually reorder after ";
        userInfo += "that if needed. ";
    } // end static


    public GoalGroupPanel(GroupInfo groupInfo) {
        super();
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
        myNoteGroup.myNoteGroupPanel = this;

        groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();

        buildPanelContent(); // Content other than the groupDataVector
    } // end constructor


    public GoalGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.GOALS));
    }


    @SuppressWarnings({"unchecked", "rawtypes"})
        // Returns a JTabbedPane where the first tab holds the true header and the second tab remains null.
        // Visually this works even when tabs are changed; for actual content changes below the header, the
        // JTabbedPane's changeListener handles that, to make it 'look' like the tabs hold the content when
        // in reality the content of the center of the basePanel is just swapped out.
    JComponent buildHeader() {
        // The two-row Header for the GoalGroup
        //-----------------------------------------------------
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The First Header Row -   Title
        headingRow1 = new JPanel(new BorderLayout());
        headingRow1.setBackground(Color.blue);
        String goalName = myNoteGroup.myProperties.getGroupName();
        String goalPlan = groupProperties.goalPlan;
        String longTitle = groupProperties.longTitle;
        if (longTitle == null || longTitle.isEmpty()) longTitle = goalName;
        JLabel titleLabel = new JLabel(longTitle);
        titleLabel.setText(longTitle);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(Font.decode("Serif-bold-20"));
        if (goalPlan != null && !goalPlan.trim().isEmpty()) {
            String theTip = AppUtil.getTooltipString(goalPlan);
            // Wrap in HTML and PREserve the original formatting, to hold on to indents and multi-line.
            theTip = "<html><pre>" + theTip + "</pre></html>";
            titleLabel.setToolTipText(theTip);
        } else {
            titleLabel.setToolTipText("Click here to enter / edit the Goal plan");
        }

        // Use a NoteData to hold the longer title and the Plan.
        NoteData titleNoteData = new NoteData(goalName); // In this case the noteString does not get used.
        titleNoteData.setSubjectString(longTitle);
        titleNoteData.setExtendedNoteString(goalPlan);

        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                boolean planChanged = editExtendedNoteComponent(titleNoteData);
                if (planChanged) {
                    if (titleNoteData.subjectString.isEmpty()) {
                        titleNoteData.subjectString = goalName;
                        // In case the user cleared the entry completely; we don't want the title line to go away.
                        // But a single space - makes a seemingly empty blue title line.  If that's what they want
                        // to see then we allow it; at least it is still re-selectable, to change to something else.
                    }
                    titleLabel.setText(titleNoteData.subjectString);
                    // set the values in group properties...
                    String goalPlan = titleNoteData.extendedNoteString;
                    ((GoalGroupProperties) myNoteGroup.myProperties).longTitle = titleNoteData.subjectString;
                    ((GoalGroupProperties) myNoteGroup.myProperties).goalPlan = goalPlan;
                    if (goalPlan != null && !goalPlan.trim().isEmpty()) {
                        titleLabel.setToolTipText(goalPlan);
                    } else {
                        titleLabel.setToolTipText("Click here to enter / edit the Goal plan");
                    }

                    setGroupChanged(true);
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                setStatusMessage("Click here to edit your Goal Name and Plan");
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setStatusMessage(" ");
            }
        });
        headingRow1.add(titleLabel, "Center");
        headingRow1.add(theTodoNoteGroupPanel.theNotePager, "East");

        // The Second Header Row -   Status dropdowns
        JPanel headingRow2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));

        JPanel currentStatusPanel = new JPanel(new FlowLayout());
        currentStatusPanel.add(new JLabel("Current Status:"));
        JComboBox currentStatus = new JComboBox<>();
        currentStatus.setModel(new DefaultComboBoxModel(GoalGroupProperties.CurrentStatus.values()));
        if (groupProperties.currentStatus != null) currentStatus.setSelectedItem(groupProperties.currentStatus);
        currentStatus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GoalGroupProperties groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();
                JComboBox jComboBox = (JComboBox) e.getSource();
                GoalGroupProperties.CurrentStatus currentStatus = (GoalGroupProperties.CurrentStatus) jComboBox.getSelectedItem();
                if (currentStatus != groupProperties.currentStatus) {
                    setGroupChanged(true);
                    groupProperties.currentStatus = currentStatus;
                }
            }
        });
        currentStatusPanel.add(currentStatus);

        JPanel overallStatusPanel = new JPanel(new FlowLayout());
        overallStatusPanel.add(new JLabel("Progress:"));
        JComboBox overallStatus = new JComboBox<>();
        overallStatus.setModel(new DefaultComboBoxModel(GoalGroupProperties.OverallStatus.values()));
        if (groupProperties.overallStatus != null) overallStatus.setSelectedItem(groupProperties.overallStatus);
        else overallStatus.setSelectedIndex(1);
        overallStatus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GoalGroupProperties groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();
                JComboBox jComboBox = (JComboBox) e.getSource();
                GoalGroupProperties.OverallStatus overallStatus = (GoalGroupProperties.OverallStatus) jComboBox.getSelectedItem();
                if (overallStatus != groupProperties.overallStatus) {
                    setGroupChanged(true);
                    groupProperties.overallStatus = overallStatus;
                }
            }
        });
        overallStatusPanel.add(overallStatus);

        headingRow2.add(new JLabel("        "));
        headingRow2.add(currentStatusPanel);
        headingRow2.add(new JLabel("             "));
        headingRow2.add(overallStatusPanel);

        heading.add(headingRow1);  // Title (reactive JLabel, with tooltip and extended note)
        heading.add(headingRow2);  // Status Dropdowns

        // Now the tabbed pane part -
        JTabbedPane theTabbedPane = new JTabbedPane();
        theTabbedPane.addTab("To Do", heading);
        theTabbedPane.addTab("Log", null);
        theTabbedPane.addTab("Milestones", null);

        theTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JTabbedPane pane = (JTabbedPane) e.getSource();
                int index = pane.getSelectedIndex();
                //System.out.println(index);
                switch (index) {
                    case 0:  // To Do List
                        theBasePanel.remove(theLogCenterPanel);
                        theBasePanel.remove(theMilestonesCenterPanel);
                        theBasePanel.add(theTodoCenterPanel, BorderLayout.CENTER);
                        theBasePanel.add(tmcPanel, BorderLayout.EAST);

                        headingRow1.remove(theLogNoteGroupPanel.theNotePager);
                        headingRow1.remove(theMilestoneNoteGroupPanel.theNotePager);
                        headingRow1.add(theTodoNoteGroupPanel.theNotePager, BorderLayout.EAST);
                        break;
                    case 1: // Log Entries
                        theBasePanel.remove(theTodoCenterPanel);
                        theBasePanel.remove(tmcPanel);
                        theBasePanel.remove(theMilestonesCenterPanel);
                        theBasePanel.add(theLogCenterPanel, BorderLayout.CENTER);

                        headingRow1.remove(theTodoNoteGroupPanel.theNotePager);
                        headingRow1.remove(theMilestoneNoteGroupPanel.theNotePager);
                        headingRow1.add(theLogNoteGroupPanel.theNotePager, BorderLayout.EAST);
                        break;
                    case 2: // Milestones
                        theBasePanel.remove(theTodoCenterPanel);
                        theBasePanel.remove(tmcPanel);
                        theBasePanel.remove(theLogCenterPanel);
                        theBasePanel.add(theMilestonesCenterPanel, BorderLayout.CENTER);

                        headingRow1.remove(theTodoNoteGroupPanel.theNotePager);
                        headingRow1.remove(theLogNoteGroupPanel.theNotePager);
                        headingRow1.add(theMilestoneNoteGroupPanel.theNotePager, BorderLayout.EAST);
                }
                theBasePanel.validate();
                theBasePanel.repaint();
            }
        });

        return theTabbedPane;
    } // end buildHeader


    // Called from within the constructor to create and place the visual components of the panel.
    @SuppressWarnings({"rawtypes"})
    private void buildPanelContent() {
        // Make a TodoNoteGroupPanel and get its center component (used when switching tabs)
        theTodoNoteGroupPanel = new TodoNoteGroupPanel(new GroupInfo(getGroupName(), GroupType.GOAL_TODO));
        theTodoNoteGroupPanel.parentNoteGroupPanel = this; // For menu adjustments.
        BorderLayout theTodoLayout = (BorderLayout) theTodoNoteGroupPanel.theBasePanel.getLayout();
        theTodoCenterPanel = (JComponent) theTodoLayout.getLayoutComponent(BorderLayout.CENTER);

        // For ToDo Date selection
        ThreeMonthColumn tmc = new ThreeMonthColumn();
        tmc.setSubscriber(theTodoNoteGroupPanel);
        theTodoNoteGroupPanel.setThreeMonthColumn(tmc);

        // Placed tmc in a panel with a FlowLayout, to prevent stretching.
        tmcPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tmcPanel.add(tmc);


        // Make a LogGroupPanel and get its center component (used when switching tabs)
        theLogNoteGroupPanel = new LogNoteGroupPanel(new GroupInfo(getGroupName(), GroupType.GOAL_LOG));
        theLogNoteGroupPanel.parentNoteGroupPanel = this; // For menu adjustments.
        BorderLayout theLogLayout = (BorderLayout) theLogNoteGroupPanel.theBasePanel.getLayout();
        theLogCenterPanel = (JComponent) theLogLayout.getLayoutComponent(BorderLayout.CENTER);

        // Make a MilestoneNoteGroupPanel and get its center component (used when switching tabs)
        theMilestoneNoteGroupPanel = new MilestoneNoteGroupPanel(new GroupInfo(getGroupName(), GroupType.MILESTONE));
        theMilestoneNoteGroupPanel.parentNoteGroupPanel = this; // For menu adjustments.
        BorderLayout theLayout = (BorderLayout) theMilestoneNoteGroupPanel.theBasePanel.getLayout();
        theMilestonesCenterPanel = (JComponent) theLayout.getLayoutComponent(BorderLayout.CENTER);

        JComponent theHeader = buildHeader();
        add(theHeader, BorderLayout.NORTH);            // Adds to theBasePanel
        add(theTodoCenterPanel, BorderLayout.CENTER);
        add(tmcPanel, BorderLayout.EAST);
    }

    @Override
    public boolean editExtendedNoteComponent(NoteData noteData) {
        setDefaultSubject("Goal Title"); // Panel uses this when calling editExtendedNoteComponent.
        // Prevent base class from constructing its own.
        extendedNoteComponent = new ExtendedNoteComponent("Goal Title");
        extendedNoteComponent.setPhantomText(userInfo);
        return super.editExtendedNoteComponent(noteData);
    }

    @Override
    void preClosePanel() {
        theTodoNoteGroupPanel.preClosePanel();
        theLogNoteGroupPanel.preClosePanel();
        theMilestoneNoteGroupPanel.preClosePanel();
        super.preClosePanel();
    }

    @Override
    public void updateGroup() {
        theTodoNoteGroupPanel.updateGroup();
        theLogNoteGroupPanel.updateGroup();
        theMilestoneNoteGroupPanel.updateGroup();
        super.updateGroup();
    }

}
