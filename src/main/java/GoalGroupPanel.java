import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;

// This class takes much of its operation directly from a ToDo List.  But one functionality that it does not take is
//   the ability to merge with another group of the same type.  While that might make sense depending on the milestones,
//   there is no good way to merge the plan statements and so it will be better to have the user do any merging
//   themselves, manually.  They do have all the tools to do that, if that is the need.

public class GoalGroupPanel extends NoteGroupPanel implements DateSelection {
    private static final Logger log = LoggerFactory.getLogger(GoalGroupPanel.class);
    private static final int DEFAULT_PAGE_SIZE = 25;
    static String userInfo;

    JComponent theHeader;
    private ThreeMonthColumn tmc;  // For Date selection
    private MilestoneComponent milestoneComponent; // The currently selected one, tied to tmc.
    LogGroupPanel theLogGroupPanel; // used by getPanelsForTabs and also by preClosePanel
    GoalGroupProperties groupProperties;
    JPanel headingRow1; // Need to reference this when switching tabs.
    JComponent theGoalCenterPanel;
    JComponent theLogCenterPanel;
    JComponent tmcPanel;  // Added or removed depending on Tab


    static {
        MemoryBank.trace();
        userInfo = "Goal Narrative:  If the title alone does not convey the full intent of your Goal then here you ";
        userInfo += "can describe more precisely what it is that you wish to acquire or accomplish.  Together with ";
        userInfo += "the milestones that lay out the discrete individual steps, this becomes your Plan.";
        userInfo += "\n\nIf you need to comment on developments or issues related to this goal, make one or more ";
        userInfo += "log entries.  The most recent log entry goes to the top but you can manually reorder after ";
        userInfo += "that if needed. ";
    } // end static


    public GoalGroupPanel(GroupInfo groupInfo, int pageSize) {
        super(pageSize);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
        myNoteGroup.myNoteGroupPanel = this;
        if (groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        loadNotesPanel();

        groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();
//        getPanelsForTabs();

        theNotePager.reset(1); // Without this, the pager appears and shows 'page 0 of 0'.
        // But with it, if there are fewer than 2 pages, it remains non-visible.

        buildPanelContent(); // Content other than the groupDataVector
    } // end constructor

    public GoalGroupPanel(GroupInfo groupInfo) {
        this(groupInfo, DEFAULT_PAGE_SIZE);
    }

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

        // Set the pager's background to the same color as this row,
        //   since other items on this row make the row slightly taller
        //   than the pager control (pager goes to the top, background shows thru at the bottom).
        theNotePager.setBackground(headingRow1.getBackground());
        headingRow1.add(theNotePager, "East");

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
        theTabbedPane.addTab("Milestones", heading);
        theTabbedPane.addTab("Log", null);

        theTabbedPane.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                JTabbedPane pane = (JTabbedPane) e.getSource();
                int index = pane.getSelectedIndex();
                //System.out.println(index);
                switch(index) {
                    case 0:
                        theBasePanel.remove(theLogCenterPanel);
                        theBasePanel.add(theGoalCenterPanel, BorderLayout.CENTER);
                        theBasePanel.add(tmcPanel, BorderLayout.EAST);
                        headingRow1.remove(theLogGroupPanel.theNotePager);
                        headingRow1.add(theNotePager, BorderLayout.EAST);
                        break;
                    case 1:
                        theBasePanel.remove(theGoalCenterPanel);
                        theBasePanel.remove(tmcPanel);
                        theBasePanel.add(theLogCenterPanel, BorderLayout.CENTER);
                        headingRow1.remove(theNotePager);
                        headingRow1.add(theLogGroupPanel.theNotePager, BorderLayout.EAST);
                        break;
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
        tmc = new ThreeMonthColumn();
        tmc.setSubscriber(this);

        // Get the center component of this GoalGroupPanel (used when switching tabs)
        BorderLayout theGoalLayout = (BorderLayout) theBasePanel.getLayout();
        theGoalCenterPanel = (JComponent) theGoalLayout.getLayoutComponent(BorderLayout.CENTER);

        // Make a LogGroupPanel and get its center component (used when switching tabs)
        theLogGroupPanel = new LogGroupPanel(new GroupInfo(getGroupName(), GroupType.GOAL_LOG));
        theLogGroupPanel.parentNoteGroupPanel = this; // For menu adjustments.
        BorderLayout theLogLayout = (BorderLayout) theLogGroupPanel.theBasePanel.getLayout();
        theLogCenterPanel = (JComponent) theLogLayout.getLayoutComponent(BorderLayout.CENTER);

        // Placed tmc in a panel with a FlowLayout, to prevent stretching.
        tmcPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tmcPanel.add(tmc);
        add(tmcPanel, BorderLayout.EAST);  // Adds to theBasePanel

        theHeader = buildHeader();
        add(theHeader, BorderLayout.NORTH);  // Adds to theBasePanel
    }


    @Override
    // Interface to the Three Month Column; called by the tmc.
    public void dateSelected(LocalDate ld) {
        MemoryBank.debug("Date selected on TMC = " + ld);

        if (milestoneComponent == null) {
            String s;
            s = "You must select an item before a date can be linked!";
            setStatusMessage(s);
            tmc.setChoice(null);
            return;
        } // end if

        TodoNoteData tnd = (TodoNoteData) (milestoneComponent.getNoteData());
        tnd.setTodoDate(ld);
        milestoneComponent.setTodoNoteData(tnd);
    } // end dateSelected


    @Override
    public boolean editExtendedNoteComponent(NoteData noteData) {
        if (noteData instanceof TodoNoteData) {
            // Let the base class make this -
            extendedNoteComponent = null;
            setDefaultSubject(null);
        } else {
            setDefaultSubject("Goal Title"); // Panel uses this when calling editExtendedNoteComponent.
            // Prevent base class from constructing its own.
            extendedNoteComponent = new ExtendedNoteComponent("Goal Title");
            extendedNoteComponent.setPhantomText(userInfo);
        }
        return super.editExtendedNoteComponent(noteData);
    }


    // Returns a TodoNoteComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    @Override
    public MilestoneComponent getNoteComponent(int i) {
        return (MilestoneComponent) groupNotesListPanel.getComponent(i);
    } // end getNoteComponent


    // This method retrieves the private JScrollPanes from this GoalGroupPanel and its LogGroupPanel.
    // Later, we use the tabs of a JTabbedPane to switch our basePanel's center content between them.
    // The variables needed to be defined 'early', so that the tab changeListener can reference them.
    private void getPanelsForTabs() {
        BorderLayout theGoalLayout = (BorderLayout) theBasePanel.getLayout();
        theGoalCenterPanel = (JComponent) theGoalLayout.getLayoutComponent(BorderLayout.CENTER);

        theLogGroupPanel = new LogGroupPanel(new GroupInfo(getGroupName(), GroupType.GOAL_LOG));
        BorderLayout theLogLayout = (BorderLayout) theLogGroupPanel.theBasePanel.getLayout();
        theLogCenterPanel = (JComponent) theLogLayout.getLayoutComponent(BorderLayout.CENTER);
    }

    ThreeMonthColumn getThreeMonthColumn() {
        return tmc;
    }

    @Override
    JComponent makeNewNote(int i) {
        MilestoneComponent tnc = new MilestoneComponent(this, i);
        tnc.setVisible(false);
        return tnc;
    } // end makeNewNote


    @Override
    void preClosePanel() {
        theLogGroupPanel.preClosePanel();
        super.preClosePanel();
    }

    //  Several actions needed when a line has
    //    either gone active or inactive.
    void showComponent(MilestoneComponent nc, boolean showit) {
        if (showit) {
            milestoneComponent = nc;
            TodoNoteData tnd = (TodoNoteData) nc.getNoteData();

            // Show the previously selected date
            if (tnd.getTodoDate() != null) {
                tmc.setChoice(tnd.getTodoDate());
            }
        } else {
            milestoneComponent = null;
            tmc.setChoice(null);
        } // end if
    } // end showComponent

    @Override
    public void updateGroup() {
        theLogGroupPanel.updateGroup();
        super.updateGroup();
    }

}
