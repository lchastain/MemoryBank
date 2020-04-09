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
        super(groupName, GroupProperties.GroupType.GOALS, 1);

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

        // Now the 2-row Header for the GoalGroup -
        //-----------------------------------------------------
        JPanel heading = new JPanel();
        heading.setLayout(new BoxLayout(heading, BoxLayout.Y_AXIS));

        // The First Header Row -   Title
        JPanel headingRow1 = new JPanel(new BorderLayout());
        headingRow1.setBackground(Color.blue);
        JLabel goalNameLabel = new JLabel(myProperties.getName());
        goalNameLabel.setHorizontalAlignment(JLabel.CENTER);
        goalNameLabel.setBackground(Color.blue);
        goalNameLabel.setForeground(Color.white);
        goalNameLabel.setFont(Font.decode("Serif-bold-20"));

        headingRow1.add(goalNameLabel, "Center");

        // The Second Header Row -  Goal Plan
        //----------------------------------------------------------
        JPanel headingRow2 = new JPanel(new BorderLayout());
//        headingRow2.setBackground(Color.cyan);

        final JTextArea goalPlan = new JTextArea("Search");
        goalPlan.setPreferredSize(new Dimension(goalPlan.getPreferredSize().width, 80));

//        goalPlan.setOpaque(false);  // so the background color shows.
        goalPlan.setForeground(Color.GRAY);
        goalPlan.addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (goalPlan.getText().equals("Search")) {
                    goalPlan.setText("");
                    goalPlan.setForeground(Color.BLACK); // looks a bit gray, when on cyan.
                }
            }
            @Override
            public void focusLost(FocusEvent e) {
                if (goalPlan.getText().isEmpty()) {
                    goalPlan.setForeground(Color.GRAY);
                    goalPlan.setText("Search");
                }
            }
        });



        headingRow2.add(goalPlan, BorderLayout.CENTER);

        heading.add(headingRow1);
        heading.add(headingRow2);
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
        //groupDataVector = AppUtil.mapper.convertValue(theGroup[1], new TypeReference<Vector<LinkData>>() { });
        // Plan now is to embed a LinkagesEditorPanel, not to have a separate Vector.
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
