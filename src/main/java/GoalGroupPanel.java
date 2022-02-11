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
import java.util.ArrayList;

// This class is a grouping of three other panels - To Do, Log, and Milestones.
@SuppressWarnings({"unchecked", "rawtypes"})
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
                        String theTip = AppUtil.getTooltipString(goalPlan);
                        // Wrap in HTML and PREserve the original formatting, to hold on to indents and multi-line.
                        theTip = "<html><pre>" + theTip + "</pre></html>";
                        titleLabel.setToolTipText(theTip);
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
    // The view will be a Tabbed Pane, with the initial Tab showing a ToDo List.
    @SuppressWarnings({"rawtypes"})
    private void buildPanelContent() {
        GroupInfo theGroupInfo;  // Support an archiveName, if there is one.
        // Make a TodoNoteGroupPanel and get its center component (used when switching tabs)
        theGroupInfo = new GroupInfo(getGroupName(), GroupType.GOAL_TODO);
        theGroupInfo.archiveName = myNoteGroup.myGroupInfo.archiveName;
        theTodoNoteGroupPanel = new TodoNoteGroupPanel(theGroupInfo);
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
        theGroupInfo = new GroupInfo(getGroupName(), GroupType.GOAL_LOG);
        theGroupInfo.archiveName = myNoteGroup.myGroupInfo.archiveName;
        theLogNoteGroupPanel = new LogNoteGroupPanel(theGroupInfo);
        theLogNoteGroupPanel.parentNoteGroupPanel = this; // For menu adjustments.
        BorderLayout theLogLayout = (BorderLayout) theLogNoteGroupPanel.theBasePanel.getLayout();
        theLogCenterPanel = (JComponent) theLogLayout.getLayoutComponent(BorderLayout.CENTER);

        // Make a MilestoneNoteGroupPanel and get its center component (used when switching tabs)
        theGroupInfo = new GroupInfo(getGroupName(), GroupType.MILESTONE);
        theGroupInfo.archiveName = myNoteGroup.myGroupInfo.archiveName;
        theMilestoneNoteGroupPanel = new MilestoneNoteGroupPanel(theGroupInfo);
        theMilestoneNoteGroupPanel.parentNoteGroupPanel = this; // For menu adjustments.
        BorderLayout theLayout = (BorderLayout) theMilestoneNoteGroupPanel.theBasePanel.getLayout();
        theMilestonesCenterPanel = (JComponent) theLayout.getLayoutComponent(BorderLayout.CENTER);

        JComponent theHeader = buildHeader();
        add(theHeader, BorderLayout.NORTH);            // Adds to theBasePanel
        add(theTodoCenterPanel, BorderLayout.CENTER);
        add(tmcPanel, BorderLayout.EAST);
    }

    @Override
    void deletePanel() {
        // This is for the member panels; the deletion of the Goal Panel will have already been done by now.
        // Called from NoteGroup.deleteNoteGroup.
        theTodoNoteGroupPanel.myNoteGroup.deleteNoteGroup();
        theLogNoteGroupPanel.myNoteGroup.deleteNoteGroup();
        theMilestoneNoteGroupPanel.myNoteGroup.deleteNoteGroup();
    }

    @Override
    public boolean editExtendedNoteComponent(NoteData noteData) {
        setDefaultSubject("Goal Title"); // Base class needs/uses this in its editExtendedNoteComponent.
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

    // Called from the menu bar:  AppTreePanel.handleMenuBar() --> saveGroupAs() --> saveAs()
    // Prompts the user for a new list name, checks it for validity,
    // then if ok, saves the file with that name.
    boolean saveAs() {
        Frame theFrame = JOptionPane.getFrameForComponent(theBasePanel);

        String thePrompt = "Please enter the new Goal name";
        int q = JOptionPane.QUESTION_MESSAGE;
        String newName = optionPane.showInputDialog(theFrame, thePrompt, "Save As", q);

        // The user cancelled; return with no complaint.
        if (newName == null) return false;

        newName = newName.trim(); // eliminate outer space.

        // Test new name validity.
        String theComplaint = myNoteGroup.groupDataAccessor.getObjectionToName(newName);
        if (!theComplaint.isEmpty()) {
            optionPane.showMessageDialog(theFrame, theComplaint,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }

        // Get the current list name -
        String oldName = getGroupName();

        // If the new name equals the old name, just do the save as the user
        //   has asked and don't tell them that they are an idiot.  But no
        //   other actions on the filesystem or the tree will be taken.
        if (newName.equals(oldName)) {
            preClosePanel();
            return false;
        } // end if

        // Check to see if the destination NoteGroup already exists.
        // If so then complain and refuse to do the saveAs.

        // Other applications might offer the option of overwriting the existing data.  This was considered
        // and rejected because of the possibility of overwriting data that is currently being shown in
        // another panel.  We could check for that as well, but decided not to because - why should we go to
        // heroic efforts to handle a user request where it seems like they may not understand what it is
        // that they are asking for?  This is the same approach that was taken in the 'rename' handling.
        ArrayList<String> groupNames = myNoteGroup.getGroupNames();
        if (groupNames.contains(newName)) {
            ems = "A Goal named " + newName + " already exists!\n";
            ems += "  operation cancelled.";
            optionPane.showMessageDialog(theFrame, ems,
                    "Error", JOptionPane.ERROR_MESSAGE);
            return false;
        } // end if

        // After we refuse to do the operation due to a preexisting destination NoteGroup with the same name,
        // the user has several recourses, depending on what it was they really wanted to do - they could
        // delete the preexisting NoteGroup or rename it, after which a second attempt at this operation
        // would succeed, or they could realize that they had been having a senior moment and abandon the
        // effort, or they could choose a different new name and try again.

        // So we got past the pre-existence check. Now change the name, update Properties and the data accessor.
        log.debug("Saving " + oldName + " as " + newName);
        GroupProperties myGroupProperties = myNoteGroup.getGroupProperties();

        // 'setGroupName' sets the name of the group, which translates into an
        // in-place change of the name of the list held by the GoalListKeeper.
        // Unfortunately, that list will still have the old title, so it still needs
        // to be removed from the keeper, to force a reload when re-accessed.
        // The calling context (AppTreePanel) must take care of the removal.
        myGroupProperties.setGroupName(newName);
        GroupInfo myGroupInfo = new GroupInfo(myGroupProperties);

        // The data accessor (constructed along with this Panel) has the old name; need to update.
        myNoteGroup.groupDataAccessor = MemoryBank.dataAccessor.getNoteGroupDataAccessor(myGroupInfo);
        setGroupChanged(true);

        // Also handle the rename for our encapsulated Groups.  The name validity checking that
        // was done for the Goal ~should~ be sufficient for all its wrapped members as well.
        //--------------------------------------------------------------------------------------
        GroupProperties todoGroupProperties = theTodoNoteGroupPanel.myNoteGroup.getGroupProperties();
        todoGroupProperties.setGroupName(newName);
        GroupInfo todoGroupInfo = new GroupInfo(todoGroupProperties);
        theTodoNoteGroupPanel.myNoteGroup.groupDataAccessor = MemoryBank.dataAccessor.getNoteGroupDataAccessor(todoGroupInfo);
        theTodoNoteGroupPanel.setGroupChanged(true);

        GroupProperties logGroupProperties = theLogNoteGroupPanel.myNoteGroup.getGroupProperties();
        logGroupProperties.setGroupName(newName);
        GroupInfo logGroupInfo = new GroupInfo(logGroupProperties);
        theLogNoteGroupPanel.myNoteGroup.groupDataAccessor = MemoryBank.dataAccessor.getNoteGroupDataAccessor(logGroupInfo);
        theLogNoteGroupPanel.setGroupChanged(true);

        GroupProperties milestoneGroupProperties = theMilestoneNoteGroupPanel.myNoteGroup.getGroupProperties();
        milestoneGroupProperties.setGroupName(newName);
        GroupInfo milestoneGroupInfo = new GroupInfo(milestoneGroupProperties);
        theMilestoneNoteGroupPanel.myNoteGroup.groupDataAccessor = MemoryBank.dataAccessor.getNoteGroupDataAccessor(milestoneGroupInfo);
        theMilestoneNoteGroupPanel.setGroupChanged(true);
        //--------------------------------------------------------------------------------------
        preClosePanel(); // This handles the save for all of them.

        return true;
    } // end saveAs

    @Override
    public void updateGroup() {
        theTodoNoteGroupPanel.updateGroup();
        theLogNoteGroupPanel.updateGroup();
        theMilestoneNoteGroupPanel.updateGroup();
        super.updateGroup();
    }

}
