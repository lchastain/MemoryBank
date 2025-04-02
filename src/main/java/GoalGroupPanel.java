import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

// This class is a grouping of four other panels - To Do, Log, Milestones, and Notes.
@SuppressWarnings({"unchecked", "rawtypes"})
public class GoalGroupPanel extends NoteGroupPanel {
    private static final Logger log = LoggerFactory.getLogger(GoalGroupPanel.class);

    private final GoalGroupProperties groupProperties;
    JTabbedPane theTabbedPane;
    private JPanel headingRow1; // Need to reference this when switching tabs.
    private JComponent theTodoCenterPanel;
    private JComponent theLogCenterPanel;
    private JComponent theMilestonesCenterPanel;
    private JComponent theNotesCenterPanel;
    private JComponent tmcPanel;  // Added or removed depending on Tab

    TodoNoteGroupPanel theTodoNoteGroupPanel;
    LogNoteGroupPanel theLogNoteGroupPanel;
    MilestoneNoteGroupPanel theMilestoneNoteGroupPanel;
    DateTimeNoteGroupPanel theDateTimeNoteGroupPanel;

    public GoalGroupPanel(GroupInfo groupInfo) {
        super();
        setDefaultSubject("Goal Title"); // Base class needs/uses this in its editNoteData.
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.  If none, we get an empty GoalGroup.
        myNoteGroup.myNoteGroupPanel = this;

        groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();

        buildPanelContent(); // Content other than the groupDataVector
        setListMenu(AppMenuBar.getNodeMenu("Goal"));
    } // end constructor


    public GoalGroupPanel(String groupName) {
        this(new GroupInfo(groupName, GroupType.GOALS));
    }


    @Override
    // As a composite (foster) NoteGroupPanel, the logic of menu item enablement needs some elaboration:
    // For a change to the Goal's title or plan, the items are enabled regardless of the state of the NoteGroup
    // in the active tab.  Otherwise, the changed states of all the NoteGroup tabs is used.
    protected void adjustMenuItems(boolean b) {
        //MemoryBank.debug("GoalGroupPanel.adjustMenuItems <" + b + ">");
        if (myNoteGroup.groupChanged) {
            super.adjustMenuItems(true);
        } else {
            boolean doit = theTodoNoteGroupPanel.myNoteGroup.groupChanged;
            doit |= theLogNoteGroupPanel.myNoteGroup.groupChanged;
            doit |= theMilestoneNoteGroupPanel.myNoteGroup.groupChanged;
            doit |= theDateTimeNoteGroupPanel.myNoteGroup.groupChanged;
            super.adjustMenuItems(doit);
        }
    } // end adjustMenuItems


    @SuppressWarnings({"unchecked", "rawtypes"})
        // Returns a JTabbedPane where the first tab holds the true header and the second tab remains null.
        // Visually this works even when tabs are changed; for actual content changes below the header, the
        // JTabbedPane's changeListener handles that, to make it 'look' like the tabs hold the content when
        // in reality the content of the center of the basePanel is just swapped out.
    JComponent buildHeader() {
        // The two-row Header for the GoalGroupPanel
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
                boolean planChanged = editNoteData(titleNoteData);
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
        currentStatus.setFocusable(false); // Otherwise, 'up' arrows can get into this combobox, and change the value.
        currentStatus.setModel(new DefaultComboBoxModel(GoalGroupProperties.CurrentStatus.values()));
        if (groupProperties.currentStatus != null) currentStatus.setSelectedItem(groupProperties.currentStatus);
        currentStatus.addActionListener(e -> {
            GoalGroupProperties groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();
            JComboBox jComboBox = (JComboBox) e.getSource();
            GoalGroupProperties.CurrentStatus currentStatus1 = (GoalGroupProperties.CurrentStatus) jComboBox.getSelectedItem();
            if (currentStatus1 != groupProperties.currentStatus) {
                setGroupChanged(true);
                groupProperties.currentStatus = currentStatus1;
            }
        });
        currentStatusPanel.add(currentStatus);

        JPanel overallStatusPanel = new JPanel(new FlowLayout());
        overallStatusPanel.add(new JLabel("Progress:"));
        JComboBox overallStatus = new JComboBox<>();
        overallStatus.setFocusable(false); // Otherwise, 'up' arrows can get into this combobox, and change the value.
        overallStatus.setModel(new DefaultComboBoxModel(GoalGroupProperties.OverallStatus.values()));
        if (groupProperties.overallStatus != null) overallStatus.setSelectedItem(groupProperties.overallStatus);
        else overallStatus.setSelectedIndex(1);
        overallStatus.addActionListener(e -> {
            GoalGroupProperties groupProperties = (GoalGroupProperties) myNoteGroup.getGroupProperties();
            JComboBox jComboBox = (JComboBox) e.getSource();
            GoalGroupProperties.OverallStatus overallStatus1 = (GoalGroupProperties.OverallStatus) jComboBox.getSelectedItem();
            if (overallStatus1 != groupProperties.overallStatus) {
                setGroupChanged(true);
                groupProperties.overallStatus = overallStatus1;
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
        theTabbedPane = new JTabbedPane();
        theTabbedPane.addTab("To Do", heading);
        theTabbedPane.addTab("Log", null);
        theTabbedPane.addTab("Milestones", null);
        theTabbedPane.addTab("Notes", null);
        theTabbedPane.setToolTipTextAt(0, "Tasks needed for progress toward Goal completion");
        theTabbedPane.setToolTipTextAt(1, "Cumulative developments related to the Goal");
        theTabbedPane.setToolTipTextAt(2, "Notations of significant progress");
        theTabbedPane.setToolTipTextAt(3, "Random notes, wish list, thoughts in progress");

        theTabbedPane.addChangeListener(e -> {
            // If the Goal itself has changed (Title or Plan), preserve it now, before adjusting the
            //   menu to match the state of NoteGroup in the new tab.
            super.preClosePanel(); // Just the Goal plan, not all tabs (yet).

            // Clear out the previous components.
            // We don't need to be surgical about this; there is no complaint if one or more are not found.
            theBasePanel.remove(theTodoCenterPanel);
            theBasePanel.remove(tmcPanel);
            theBasePanel.remove(theLogCenterPanel);
            theBasePanel.remove(theMilestonesCenterPanel);
            theBasePanel.remove(theNotesCenterPanel);
            headingRow1.remove(theTodoNoteGroupPanel.theNotePager);
            headingRow1.remove(theLogNoteGroupPanel.theNotePager);
            headingRow1.remove(theMilestoneNoteGroupPanel.theNotePager);
            headingRow1.remove(theDateTimeNoteGroupPanel.theNotePager);

            JTabbedPane pane = (JTabbedPane) e.getSource();
            int index = pane.getSelectedIndex();
            switch (index) {
                case 0 -> {  // To Do List
                    theBasePanel.add(theTodoCenterPanel, BorderLayout.CENTER);
                    theBasePanel.add(tmcPanel, BorderLayout.EAST);
                    headingRow1.add(theTodoNoteGroupPanel.theNotePager, BorderLayout.EAST);
                }
                case 1 -> { // Log Entries
                    theBasePanel.add(theLogCenterPanel, BorderLayout.CENTER);
                    headingRow1.add(theLogNoteGroupPanel.theNotePager, BorderLayout.EAST);
                }
                case 2 -> { // Milestones
                    theBasePanel.add(theMilestonesCenterPanel, BorderLayout.CENTER);
                    headingRow1.add(theMilestoneNoteGroupPanel.theNotePager, BorderLayout.EAST);
                }
                case 3 -> { // Notes
                    theBasePanel.add(theNotesCenterPanel, BorderLayout.CENTER);
                    headingRow1.add(theDateTimeNoteGroupPanel.theNotePager, BorderLayout.EAST);
                }
            }
            theBasePanel.validate();
            theBasePanel.repaint();
        });

        return theTabbedPane;
    } // end buildHeader


    // Called from within the constructor to create and place the visual components of the panel.
    // The view will be a Header area over a Tabbed Pane, with the initial Tab showing a 'To Do' List.
    private void buildPanelContent() {
        GroupInfo theGroupInfo;  // Support an archiveName, if there is one.
        BorderLayout theLayout;

        // Make a TodoNoteGroupPanel and get its center component (used when switching tabs)
        theGroupInfo = new GroupInfo(getGroupName(), GroupType.GOAL_TODO);
        theGroupInfo.archiveName = myNoteGroup.myGroupInfo.archiveName;
        theTodoNoteGroupPanel = new TodoNoteGroupPanel(theGroupInfo);
        theTodoNoteGroupPanel.fosterNoteGroupPanel = this; // For menu adjustments.
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
        theLogNoteGroupPanel.fosterNoteGroupPanel = this; // For menu adjustments.
        BorderLayout theLogLayout = (BorderLayout) theLogNoteGroupPanel.theBasePanel.getLayout();
        theLogCenterPanel = (JComponent) theLogLayout.getLayoutComponent(BorderLayout.CENTER);

        // Make a MilestoneNoteGroupPanel and get its center component (used when switching tabs)
        theGroupInfo = new GroupInfo(getGroupName(), GroupType.MILESTONE);
        theGroupInfo.archiveName = myNoteGroup.myGroupInfo.archiveName;
        theMilestoneNoteGroupPanel = new MilestoneNoteGroupPanel(theGroupInfo);
        theMilestoneNoteGroupPanel.fosterNoteGroupPanel = this; // For menu adjustments.
        theLayout = (BorderLayout) theMilestoneNoteGroupPanel.theBasePanel.getLayout();
        theMilestonesCenterPanel = (JComponent) theLayout.getLayoutComponent(BorderLayout.CENTER);

        // Make a DateTimeNoteGroupPanel and get its center component (used when switching tabs)
        theGroupInfo = new GroupInfo(getGroupName(), GroupType.GOAL_NOTES);
        theGroupInfo.archiveName = myNoteGroup.myGroupInfo.archiveName;
        theDateTimeNoteGroupPanel = new DateTimeNoteGroupPanel(theGroupInfo);
        theDateTimeNoteGroupPanel.fosterNoteGroupPanel = this; // For menu adjustments.
        theLayout = (BorderLayout) theDateTimeNoteGroupPanel.theBasePanel.getLayout();
        theNotesCenterPanel = (JComponent) theLayout.getLayoutComponent(BorderLayout.CENTER);

        JComponent theHeader = buildHeader();
        add(theHeader, BorderLayout.NORTH);            // Adds to theBasePanel
        add(theTodoCenterPanel, BorderLayout.CENTER);
        add(tmcPanel, BorderLayout.EAST);
    } // end buildPanelContent

    // Clear all notes (which may span more than one page) and the interface.
    // This still leaves the GroupProperties.
    @Override
    void clearAllNotes() {
        int index = theTabbedPane.getSelectedIndex();
        switch (index) {
            case 0 ->  // To Do List
                    theTodoNoteGroupPanel.clearAllNotes();
            case 1 -> // Log Entries
                    theLogNoteGroupPanel.clearAllNotes();
            case 2 -> // Milestones
                    theMilestoneNoteGroupPanel.clearAllNotes();
            case 3 -> // Notes
                    theDateTimeNoteGroupPanel.clearAllNotes();
            default ->  // We don't expect this one to be used, but it covers the unexpected.
                    super.clearAllNotes();
        }
    } // end clearAllNotes


    @Override
    void deletePanel() {
        // This is for the member panels; the deletion of the Goal Panel will have already been done by now.
        // Called from NoteGroup.deleteNoteGroup.
        theTodoNoteGroupPanel.myNoteGroup.deleteNoteGroup();
        theLogNoteGroupPanel.myNoteGroup.deleteNoteGroup();
        theMilestoneNoteGroupPanel.myNoteGroup.deleteNoteGroup();
        theDateTimeNoteGroupPanel.myNoteGroup.deleteNoteGroup();

    }

    @Override
    void preCloseAndRefresh() {
        super.preClosePanel(); // This saves the Goal Plan (only).  No refresh needed.
        theTodoNoteGroupPanel.preCloseAndRefresh();
        theLogNoteGroupPanel.preCloseAndRefresh();
        theMilestoneNoteGroupPanel.preCloseAndRefresh();
        theDateTimeNoteGroupPanel.preCloseAndRefresh();
    }

    // This one is called upon app conclusion to save the data, but no need to update Panels.
    @Override
    void preClosePanel() {
        super.preClosePanel(); // This saves the Goal Plan (only)
        theTodoNoteGroupPanel.preClosePanel();
        theLogNoteGroupPanel.preClosePanel();
        theMilestoneNoteGroupPanel.preClosePanel();
        theDateTimeNoteGroupPanel.preClosePanel();
    } // end preClosePanel

    @Override
    // Reload the persisted data, and redisplay.
    public void refresh() {
        super.refresh();
        theTodoNoteGroupPanel.refresh();
        theLogNoteGroupPanel.refresh();
        theMilestoneNoteGroupPanel.refresh();
        theDateTimeNoteGroupPanel.refresh();
    } // end refresh


    @Override
    void renamePanel(String renameTo) {
        // A re-name of the Goal Panel needs to cascade thru to its tabs.
        theTodoNoteGroupPanel.myNoteGroup.renameNoteGroup(renameTo);
        theLogNoteGroupPanel.myNoteGroup.renameNoteGroup(renameTo);
        theMilestoneNoteGroupPanel.myNoteGroup.renameNoteGroup(renameTo);
        theDateTimeNoteGroupPanel.myNoteGroup.renameNoteGroup(renameTo);
    }

    // Called from the menu bar:  AppTreePanel.handleMenuBar() --> saveGroupAs() --> saveAs()
    // Prompts the user for a new list name, checks it for validity,
    // then if ok, saves the group with that name.
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
        ArrayList<String> groupNames = MemoryBank.dataAccessor.getGroupNames(GroupType.GOALS, true);
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

        GroupProperties noteGroupProperties = theDateTimeNoteGroupPanel.myNoteGroup.getGroupProperties();
        noteGroupProperties.setGroupName(newName);
        GroupInfo noteGroupInfo = new GroupInfo(noteGroupProperties);
        theDateTimeNoteGroupPanel.myNoteGroup.groupDataAccessor = MemoryBank.dataAccessor.getNoteGroupDataAccessor(noteGroupInfo);
        theDateTimeNoteGroupPanel.setGroupChanged(true);
        //--------------------------------------------------------------------------------------
        preClosePanel(); // This handles the save for all of them.

        return true;
    } // end saveAs
}
