import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.io.File;
import java.util.ArrayList;

// A GoalGroup (like any other NoteGroup) is not itself saved (serialized).  Its
// properties are what goes out to the data file.  But unlike the other NoteGroup
// children, it has no list of NoteData items.

public class GoalGroup extends NoteGroup {
    private static Logger log = LoggerFactory.getLogger(GoalGroup.class);
    static String areaName;
    static String areaPath;
    static String filePrefix;

    // This is saved/loaded
    GoalGroupProperties myProperties; // Variables - flags and settings

    static {
        areaName = "Goals"; // Directory name under user data.
        areaPath = MemoryBank.userDataHome + File.separatorChar + areaName + File.separatorChar;
        filePrefix = "goal_";
        MemoryBank.trace();
    } // end static

    public GoalGroup(String groupName) {
        super(groupName, GroupProperties.GroupType.GOALS, 10);

        log.debug("Constructing: " + groupName);

        addNoteAllowed = false;

        setGroupFilename(areaPath + filePrefix + groupName + ".json");

        // All our goal data is in the properties, unlike other NoteGroup that also have a NoteData vector.
        saveWithoutData = true;

        updateGroup(); // This will load the properties (myProperties)

        buildPanelContent(); // Content other than the groupDataVector
    }

    // Called from within the constructor to create and place the visual components of the panel.
    private void buildPanelContent() {
//        theBasePanel.removeAll();
//        theBasePanel.revalidate();

        // Now the multi-row Header for the GoalGroup -
        //-----------------------------------------------------
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The First Header Row -   Title
        JPanel headingRow1 = new JPanel(new BorderLayout());
        headingRow1.setBackground(Color.blue);
        JLabel goalNameLabel = new JLabel(myProperties.getName());
        String longTitle = myProperties.longTitle;
        if(null != longTitle && !longTitle.isEmpty()) goalNameLabel.setText(longTitle);
        goalNameLabel.setHorizontalAlignment(JLabel.CENTER);
        goalNameLabel.setBackground(Color.blue);
        goalNameLabel.setForeground(Color.white);
        goalNameLabel.setFont(Font.decode("Serif-bold-20"));
        goalNameLabel.setToolTipText("Click here to enter a longer Goal title");
        headingRow1.add(goalNameLabel, "Center");

        // The Second Header Row -  Goal Plan
        //----------------------------------------------------------
        JPanel headingRow2 = new JPanel(new BorderLayout());
        String userInfo;
        userInfo = "Enter your plan for accomplishing this goal.  It should be stated in general terms\n";
        userInfo += "to describe what needs to be done, but not how.  If it boils down to a single task\n";
        userInfo += "then it should go to a To Do List and not here.  That To Do List item (or any other\n";
        userInfo += "type of note) can then be linked to this Goal.";
        final String defaultText = userInfo;
        final JTextArea goalPlan = new JTextArea(defaultText);
        goalPlan.setPreferredSize(new Dimension(goalPlan.getPreferredSize().width, 80));
        goalPlan.setForeground(Color.GRAY);
        goalPlan.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (goalPlan.getText().equals(defaultText)) {
                    goalPlan.setText("");
                    goalPlan.setForeground(Color.BLACK); // looks a bit gray, when on cyan.
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (goalPlan.getText().isEmpty()) {
                    goalPlan.setForeground(Color.GRAY);
                    goalPlan.setText(defaultText);
                }
            }
        });
        headingRow2.add(goalPlan, BorderLayout.CENTER);

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
        JLabel listHeader = new JLabel("Linked Notes");
        listHeader.setHorizontalAlignment(JLabel.CENTER);
        listHeader.setFont(Font.decode("Serif-bold-14"));
        headingRow3.add(listHeader, BorderLayout.CENTER);
        headingRow3.add(overallStatusPanel, BorderLayout.EAST);



        heading.add(headingRow1);
        heading.add(headingRow2);
        heading.add(headingRow3);
        add(heading, BorderLayout.NORTH);
    }


    // Called from within the constructor to create and place the visual components of the panel.
    private void buildPanelContent0() {
        ArrayList<String> arr;

        JLabel jLabel1 = new JLabel();
        // Variables declaration
        JTextField txtfGoalText = new JTextField();
        JScrollPane jspGoals = new JScrollPane();
        JTextArea txtaPlan = new JTextArea();
        JScrollPane jspPlan = new JScrollPane();
        JButton jButton1 = new JButton();
        JButton jButton2 = new JButton();
        JButton jButton3 = new JButton();
        JPanel contentPane = theBasePanel;

        // 
        // jLabel1 
        // 
        jLabel1.setText("Some way to display and select related todo lists.");
        // 
        // txtfGoalText 
        // 
        txtfGoalText.setText("The text of the goal");
        txtfGoalText.addActionListener(this::txtfGoalText_actionPerformed);
        //
        // txtaPlan
        // 
        txtaPlan.setText("The Plan - may include a description.");
        // 
        // jspPlan 
        // 
        jspPlan.setViewportView(txtaPlan);
        // 
        // jButton1 
        // 
        jButton1.setText("New");
        jButton1.addActionListener(this::jButton1_actionPerformed);
        // 
        // jButton2 
        // 
        jButton2.setText("Add");
        jButton2.addActionListener(this::jButton2_actionPerformed);
        // 
        // jButton3 
        // 
        jButton3.setText("Delete");
        jButton3.addActionListener(this::jButton3_actionPerformed);
        // 
        // contentPane 
        // 
        contentPane.setLayout(null);
        addComponent(contentPane, jLabel1, 153, 164, 276, 18);
        addComponent(contentPane, txtfGoalText, 138, 21, 298, 21);
        addComponent(contentPane, jspPlan, 140, 60, 294, 85);
        //
        // GoalPanel
        // 
        theBasePanel.setLocation(new Point(0, 0));
        theBasePanel.setSize(new Dimension(478, 270));
    }// end buildPanelContent


    /**
     * Add Component Without a Layout Manager (Absolute Positioning)
     */
    private void addComponent(Container container, Component c, int x, int y, int width, int height) {
        c.setBounds(x, y, width, height);
        container.add(c);
    }


    //--------------------------------------------------------------
    // Method Name: getProperties
    //
    //  Called by saveGroup.
    //  Returns an actual object, vs the overridden method
    //    in the base class that returns a null.
    //--------------------------------------------------------------
    @Override
    protected Object getProperties() {
        return myProperties;
    } // end getProperties


    @Override
    void makeProperties(String groupName, GroupProperties.GroupType groupType) {
        myProperties = new GoalGroupProperties(groupName);
    }

    @Override
    void setGroupData(Object[] theGroup) {
        BaseData.loading = true; // We don't want to affect the lastModDates!
        myProperties = AppUtil.mapper.convertValue(theGroup[0], GoalGroupProperties.class);
        //groupDataVector = AppUtil.mapper.convertValue(theGroup[1], new TypeReference<Vector<?LinkData?>>() { });
        // Need to define the link type for reversing a link, get a list of sources that is added to each time a link is made.
        // may be similar to SearchResultData / component.
        BaseData.loading = false; // Restore normal lastModDate updating.
    }

    private void txtfGoalText_actionPerformed(ActionEvent e) {
        System.out.println("\ntxtfGoalText_actionPerformed(ActionEvent e) called.");
    }

    private void jButton1_actionPerformed(ActionEvent e) {
        System.out.println("\njButton1_actionPerformed(ActionEvent e) called.");
    }

    private void jButton2_actionPerformed(ActionEvent e) {
        System.out.println("\njButton2_actionPerformed(ActionEvent e) called.");
    }

    private void jButton3_actionPerformed(ActionEvent e) {
        System.out.println("\njButton3_actionPerformed(ActionEvent e) called.");
    }

}
