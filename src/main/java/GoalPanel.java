import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.util.ArrayList;

public class GoalPanel extends JPanel {
    private static final long serialVersionUID = 1L;
    private JList<String> lstGoals;
    // End of variables declaration

    public GoalPanel() {
        super();
        initializeComponent();
        this.setVisible(true);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always regenerated
     * by the Windows Form Designer. Otherwise, retrieving design might not work properly.
     * Tip: If you must revise this method, please backup this GUI file for JFrameBuilder
     * to retrieve your design properly in future, before revising this method.
     */
    private void initializeComponent() {
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
        // lstGoals 
        // 
        arr = new ArrayList<>();
        arr.add("List");
        arr.add("of");
        arr.add("goals");

        String[] array = new String[arr.size()];
        arr.toArray(array); // fill the array

        lstGoals.setListData(array);
        lstGoals.addListSelectionListener(this::lstGoals_valueChanged);
        // 
        // jspGoals 
        // 
        jspGoals.setViewportView(lstGoals);
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
        addComponent(contentPane, jspGoals, 18, 23, 102, 205);
        addComponent(contentPane, jspPlan, 140, 60, 294, 85);
        addComponent(contentPane, jButton1, 151, 208, 57, 25);
        addComponent(contentPane, jButton2, 254, 204, 55, 25);
        addComponent(contentPane, jButton3, 347, 204, 65, 25);
        // 
        // GoalPanel
        // 
        this.setTitle("Goals");
        this.setLocation(new Point(0, 0));
        this.setSize(new Dimension(478, 270));
    }


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

    // Just for JFrameBuilder -
    public void setTitle(String s) {
        // ignore, for now.
    } // end setTitle


    public static void main(String[] args) {
        JFrame.setDefaultLookAndFeelDecorated(true);
        JDialog.setDefaultLookAndFeelDecorated(true);
        try {
            UIManager.setLookAndFeel("com.sun.java.swing.plaf.windows.WindowsLookAndFeel");
        } catch (Exception ex) {
            System.out.println("Failed loading L&F: ");
            ex.printStackTrace();
        }
        JFrame jf = new JFrame();
        jf.getContentPane().setLayout(null);
        jf.getContentPane().add(new GoalPanel());
        jf.setSize(600, 400);
        jf.setVisible(true);
    }

}
