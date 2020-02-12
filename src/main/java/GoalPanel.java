import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class GoalPanel extends NoteGroup {
    private static final long serialVersionUID = 1L;
    private String goalFileName;
    static String areaName;
    private JList<String> lstGoals;
    // End of variables declaration

    static {
        areaName = "Goals"; // Directory name under user data.
        MemoryBank.trace();
    } // end static

    public GoalPanel(String fname) {
        super();

        MemoryBank.debug("Constructing: " + getName());

        goalFileName = basePath(areaName) + "goal_" + fname + ".json";
        saveWithoutData = true;
        updateGroup();

        buildPanelContent();
        this.setVisible(true);
    }

    // Called from within the constructor to create and place the visual components of the panel.
    private void buildPanelContent() {
        setLayout(new BorderLayout());

        JLabel goalTitleText = new JLabel("Get a degree in Engineering");
        goalTitleText.setHorizontalAlignment(JLabel.CENTER);
        goalTitleText.setFont(Font.decode("Serif-bold-20"));
        add(goalTitleText, BorderLayout.NORTH);

        JTextArea goalPlan = new JTextArea();
        add(goalPlan, BorderLayout.CENTER);
    }


    // Called from within the constructor to create and place the visual components of the panel.
    private void buildPanelContent0() {
        ArrayList<String> arr;

        JLabel jLabel1 = new JLabel();
        // Variables declaration
        JTextField txtfGoalText = new JTextField();
        lstGoals = new JList<>();
        JScrollPane jspGoals = new JScrollPane();
        JTextArea txtaPlan = new JTextArea();
        JScrollPane jspPlan = new JScrollPane();
        JButton jButton1 = new JButton();
        JButton jButton2 = new JButton();
        JButton jButton3 = new JButton();
        JPanel contentPane = this.getContentPane();

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
        this.setLocation(new Point(0, 0));
        this.setSize(new Dimension(478, 270));
    }// end buildPanelContent


    /**
     * Add Component Without a Layout Manager (Absolute Positioning)
     */
    private void addComponent(Container container, Component c, int x, int y, int width, int height) {
        c.setBounds(x, y, width, height);
        container.add(c);
    }


    // This method is only here to 'fool' JFramebuilder-generated code.
    public JPanel getContentPane() {
        return GoalPanel.this;
    }


    private void txtfGoalText_actionPerformed(ActionEvent e) {
        System.out.println("\ntxtfGoalText_actionPerformed(ActionEvent e) called.");
    }

    private void lstGoals_valueChanged(ListSelectionEvent e) {
        System.out.println("\nlstGoals_valueChanged(ListSelectionEvent e) called.");
        if (!e.getValueIsAdjusting()) {
            Object o = lstGoals.getSelectedValue();
            System.out.println(">>" + ((o == null) ? "null" : o.toString()) + " is selected.");
        }
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

    @Override
    public String getLeafFilename() {
        return goalFileName;
    }
}
