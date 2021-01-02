import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;

// This class takes much of its operation directly from a ToDo List.  But one functionality that it does not take is
//   the ability to merge with another group of the same type.  While that might make sense depending on the milestones,
//   there is no good way to merge the plan statements and so it will be better to have the user do any merging
//   themselves, manually.  They do have all the tools to do that, if that is the need.

public class GoalGroupPanel extends NoteGroupPanel implements DateSelection {
    private static final Logger log = LoggerFactory.getLogger(GoalGroupPanel.class);
    static String userInfo;
    static String defaultPlanText;

    JLabel titleLabel;
    private ThreeMonthColumn tmc;  // For Date selection
    private MilestoneComponent milestoneComponent;

    static {
        MemoryBank.trace();
        userInfo = "Goal Narrative:  If the title alone does not convey the full intent of your Goal then here you ";
        userInfo += "can describe more precisely what it is that you wish to acquire or accomplish.  Together with ";
        userInfo += "the milestones that lay out the discrete individual steps, this becomes your Plan.";
        userInfo += "\n\nIf you want to comment on developments or issues related to this goal, you could put those ";
        userInfo += "comments here as a snapshot of current status but if you want to track your progress over time ";
        userInfo += "then put them in a note appropriate its time period, then link that note back to this ";
        userInfo += "goal.  Your comments can of course include your own estimate of a percentage of goal ";
        userInfo += "completion, if desired.";
//        userInfo += "\n\nIf you want to comment on developments or issues related to this goal, put those comments ";
//        userInfo += "in a note appropriate to the time period where it best fits, then link that note back to this ";
//        userInfo += "goal.  Your comments can include your estimation of a percentage of goal completeness, if desired.";

//        userInfo = "Enter the major (remaining) steps for achieving this goal.  These are the milestones ";
//        userInfo += "(in order when appropriate), without specifics as to how they will be accomplished.  ";
//        userInfo += "The tasks needed to complete each milestone should go to a To Do List and those ";
//        userInfo += "To Do List items (or any other type of note) can then be linked back to this Goal.";
        defaultPlanText = userInfo;

    } // end static


    public GoalGroupPanel(GroupInfo groupInfo) {
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
        myNoteGroup.myNoteGroupPanel = this;
        if(groupInfo.archiveName != null) setEditable(false); // Archived groups are non-editable
        loadNotesPanel();
        buildPanelContent(); // Content other than the groupDataVector
    }


    public GoalGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.GOALS));
    }


    // Called from within the constructor to create and place the visual components of the panel.
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void buildPanelContent() {
        tmc = new ThreeMonthColumn();
        tmc.setSubscriber(this);

        // Placed tmc in a panel with a FlowLayout, to prevent stretching.
        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        eastPanel.add(tmc);
        theBasePanel.add(eastPanel, BorderLayout.EAST);

        GoalGroupProperties groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();

        // The multi-row Header for the GoalGroup -
        //-----------------------------------------------------
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The First Header Row -   Title
        JPanel headingRow1 = new JPanel(new BorderLayout()); // Need to put the title into a separate panel, because -
        headingRow1.setBackground(Color.blue); // it covers width of the panel, not just the length of the title.
        String goalName = myNoteGroup.myProperties.getGroupName();
        String goalPlan = groupProperties.goalPlan;
        String longTitle = groupProperties.longTitle;
        if(longTitle == null || longTitle.isEmpty()) longTitle = goalName;
        titleLabel = new JLabel(longTitle);
        titleLabel.setText(longTitle);
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setForeground(Color.white);
        titleLabel.setFont(Font.decode("Serif-bold-20"));
        if(goalPlan != null && !goalPlan.trim().isEmpty()) {
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
                if(planChanged) {
                    if(titleNoteData.subjectString.isEmpty()) {
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
                    if(goalPlan != null && !goalPlan.trim().isEmpty()) {
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

        // The Second Header Row -   Status
        JPanel headingRow2 = new JPanel(new DndLayout());

        JPanel currentStatusPanel = new JPanel(new FlowLayout());
        currentStatusPanel.add(new JLabel("Current Status:"));
        JComboBox currentStatus = new JComboBox<>();
        currentStatus.setModel(new DefaultComboBoxModel(GoalGroupProperties.CurrentStatus.values()));
        if(groupProperties.currentStatus != null) currentStatus.setSelectedItem(groupProperties.currentStatus);
        currentStatus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GoalGroupProperties groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();
                JComboBox jComboBox = (JComboBox) e.getSource();
                GoalGroupProperties.CurrentStatus currentStatus = (GoalGroupProperties.CurrentStatus) jComboBox.getSelectedItem();
                if(currentStatus != groupProperties.currentStatus) {
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
        if(groupProperties.overallStatus != null) overallStatus.setSelectedItem(groupProperties.overallStatus);
        else overallStatus.setSelectedIndex(1);
        overallStatus.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GoalGroupProperties groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();
                JComboBox jComboBox = (JComboBox) e.getSource();
                GoalGroupProperties.OverallStatus overallStatus = (GoalGroupProperties.OverallStatus) jComboBox.getSelectedItem();
                if(overallStatus != groupProperties.overallStatus) {
                    setGroupChanged(true);
                    groupProperties.overallStatus = overallStatus;
                }
            }
        });
        overallStatusPanel.add(overallStatus);

        JLabel listHeader = new JLabel("Milestones");
        listHeader.setHorizontalAlignment(JLabel.CENTER);
        listHeader.setFont(Font.decode("Serif-bold-14"));
        headingRow2.add(listHeader, "Stretch");
        headingRow2.add(currentStatusPanel, BorderLayout.WEST);
        headingRow2.add(new JLabel("      "));
        headingRow2.add(overallStatusPanel, BorderLayout.EAST);

        heading.add(headingRow1);
        heading.add(headingRow2);
        add(heading, BorderLayout.NORTH);
    }


    // Interface to the Three Month Calendar; called by the tmc.
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
        if(noteData instanceof TodoNoteData) {
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


    ThreeMonthColumn getThreeMonthColumn() {
        return tmc;
    }

    @Override
    JComponent makeNewNote(int i) {
        MilestoneComponent tnc = new MilestoneComponent(this, i);
        tnc.setVisible(false);
        return tnc;
    } // end makeNewNote


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

}
