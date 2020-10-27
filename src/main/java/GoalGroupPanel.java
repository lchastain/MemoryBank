import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.time.LocalDate;


public class GoalGroupPanel extends NoteGroupPanel implements DateSelection {
    private static final Logger log = LoggerFactory.getLogger(GoalGroupPanel.class);
    static String userInfo;
    static String defaultPlanText;

    private ThreeMonthColumn tmc;  // For Date selection
    private MilestoneComponent milestoneComponent;

    static {
        MemoryBank.trace();

        userInfo = "Enter the major (remaining) steps for achieving this goal.  These are the milestones ";
        userInfo += "(in order when appropriate), without specifics as to how they will be accomplished.  ";
        userInfo += "The tasks needed to complete each milestone should go to a To Do List and those ";
        userInfo += "To Do List items (or any other type of note) can then be linked back to this Goal.";
        defaultPlanText = userInfo; // final because it is used by event handlers

    } // end static

    public GoalGroupPanel(String groupName) {
        super(10);

        GroupInfo groupInfo = new GroupInfo(groupName, GroupInfo.GroupType.GOALS);
        myNoteGroup = groupInfo.getNoteGroup(); // This also loads the data, if any.
        myNoteGroup.myNoteGroupPanel = this;
        loadNotesPanel(); // previously was done via updateGroup; remove this comment when stable.

        buildPanelContent(); // Content other than the groupDataVector
    }

    // Called from within the constructor to create and place the visual components of the panel.
    private void buildPanelContent() {
        tmc = new ThreeMonthColumn();
        tmc.setSubscriber(this);

        // Wrapped tmc in a FlowLayout panel, to prevent stretching.
        JPanel pnl1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnl1.add(tmc);
        theBasePanel.add(pnl1, BorderLayout.EAST);


        // The multi-row Header for the GoalGroup -
        //-----------------------------------------------------
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The First Header Row -   Title
        JPanel headingRow1 = new JPanel(new BorderLayout());
        headingRow1.setBackground(Color.blue);
        JLabel goalNameLabel = new JLabel(myNoteGroup.myProperties.getGroupName());
        String longTitle = ((GoalGroupProperties) myNoteGroup.myProperties).longTitle;
        if (null != longTitle && !longTitle.isEmpty()) goalNameLabel.setText(longTitle);
        goalNameLabel.setHorizontalAlignment(JLabel.CENTER);
        goalNameLabel.setBackground(Color.blue);
        goalNameLabel.setForeground(Color.white);
        goalNameLabel.setFont(Font.decode("Serif-bold-20"));
        goalNameLabel.setToolTipText("Click here to enter a longer Goal title");
        headingRow1.add(goalNameLabel, "Center");

        // The Second Header Row -  Goal Plan
        //----------------------------------------------------------
        JPanel headingRow2 = new JPanel(new BorderLayout());
        String thePlanString = ((GoalGroupProperties) myNoteGroup.myProperties).goalPlan;
        if (thePlanString == null) thePlanString = defaultPlanText;
        headingRow2.add(makePlanTextArea(thePlanString), BorderLayout.CENTER);

        // The Third Header Row -   Status
        //----------------------------------------------------------
        JPanel headingRow3 = new JPanel(new BorderLayout());

        JPanel currentStatusPanel = new JPanel(new FlowLayout());
        currentStatusPanel.add(new JLabel("Current Status:"));
        JComboBox<String> currentStatus = new JComboBox<>();
        currentStatus.addItem("Not Started");
        currentStatus.addItem("Started");
        currentStatus.addItem("Stalled");
        currentStatus.addItem("Underway");
        currentStatusPanel.add(currentStatus);

        JPanel overallStatusPanel = new JPanel(new FlowLayout());
        overallStatusPanel.add(new JLabel("Overall Status:"));
        JComboBox<String> overallStatus = new JComboBox<>();
        overallStatus.addItem("Undefined");
        overallStatus.addItem("Defined");
        overallStatus.addItem("Ahead of Schedule");
        overallStatus.addItem("On Schedule");
        overallStatus.addItem("Behind Schedule");
        overallStatusPanel.add(overallStatus);

        headingRow3.add(currentStatusPanel, BorderLayout.WEST);
        JLabel listHeader = new JLabel("Milestones");
        listHeader.setHorizontalAlignment(JLabel.CENTER);
        listHeader.setFont(Font.decode("Serif-bold-14"));
        headingRow3.add(listHeader, BorderLayout.CENTER);
        headingRow3.add(overallStatusPanel, BorderLayout.EAST);

        heading.add(headingRow1);
        heading.add(headingRow2);
        heading.add(headingRow3);
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



    //--------------------------------------------------------
    // Method Name: getNoteComponent
    //
    // Returns a TodoNoteComponent that can be used to manipulate
    // component state as well as set/get underlying data.
    //--------------------------------------------------------
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


    JTextArea makePlanTextArea(String thePlanString) {
        final JTextArea goalPlanTextArea = new JTextArea(thePlanString);
        goalPlanTextArea.setLineWrap(true);
        goalPlanTextArea.setWrapStyleWord(true);
        goalPlanTextArea.setPreferredSize(new Dimension(goalPlanTextArea.getPreferredSize().width, 100));
        if (thePlanString.equals(defaultPlanText)) goalPlanTextArea.setForeground(Color.GRAY);

        // Desired behavior from the event listeners below:  Initial display contains only the default, gray text.
        //   If the user presses any key, the text area is cleared and the key they pressed, if printable,
        //     appears in the text area.  If not printable, the text area remains blank.  If the focus
        //     shifts away from the text area while it is empty, the default text is restored.
        goalPlanTextArea.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (goalPlanTextArea.getText().trim().isEmpty()) {
                    goalPlanTextArea.setForeground(Color.GRAY);
                    goalPlanTextArea.setText(defaultPlanText);
                }
            }
        });
        goalPlanTextArea.addKeyListener(new KeyAdapter() {
            boolean clearingDefault;

            @Override
            public void keyPressed(KeyEvent e) {
                super.keyPressed(e);
                char theyTyped = e.getKeyChar();
                if (goalPlanTextArea.getText().equals(defaultPlanText)) {
                    goalPlanTextArea.setText("");
                    goalPlanTextArea.setForeground(Color.BLACK);
                    clearingDefault = true;
                }

                if (Character.isLetterOrDigit(theyTyped)) {
                    clearingDefault = false;
                }

            }

            @Override
            public void keyReleased(KeyEvent e) {
                super.keyReleased(e);
                if (!clearingDefault) {
                    if (goalPlanTextArea.getText().trim().isEmpty()) {
                        goalPlanTextArea.setForeground(Color.GRAY);
                        goalPlanTextArea.setText(defaultPlanText);
                    }
                }
            }
        });
        return goalPlanTextArea;
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

}
