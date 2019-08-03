//
//

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.AbstractDocument;

public class TodoOpts extends JTabbedPane {
    private static final long serialVersionUID = -8478940342870392492L;

    private PriorityPanel priorityPanel;
    private printPanel pp;
    private SortPanel sp;
    private TodoListProperties tlp;

    public TodoOpts(TodoListProperties o) {
        super();
        tlp = o;
        priorityPanel = new PriorityPanel();
        pp = new printPanel();
        sp = new SortPanel(tlp.whenNoKey);
        addTab("Priority", null, priorityPanel,
                "Set priority visibility and limit");
        addTab("Print", null, pp, "Configure the printout");
        addTab("Sort", null, sp, "How to Sort");
    } // end constructor

    public TodoListProperties getValues() {
        String digit;
        String userInt;

        //----------------------------------------------------
        // Priority panel
        //----------------------------------------------------
        tlp.showPriority = priorityPanel.cb1.isSelected();

        // Idiot-proofing...
        userInt = priorityPanel.tf1.getText().trim();
        if (userInt.length() > 2) userInt = userInt.substring(0, 2).trim();
        if (userInt.length() == 0) userInt = "0";
        digit = userInt.substring(0, 1);
        String ints = "0123456789";
        if (!ints.contains(digit)) userInt = "0";
        if (userInt.length() == 2) {
            digit = userInt.substring(1, 2);
            if (!ints.contains(digit)) userInt = userInt.substring(0, 1);
        } // end if
        tlp.maxPriority = new Integer(userInt);

        // Print panel
        tlp.pHeader = pp.cb1.isSelected();
        tlp.pFooter = pp.cb2.isSelected();
        tlp.pBorder = pp.cb3.isSelected();
        tlp.pCSpace = pp.cb4.isSelected();
        tlp.pPriority = pp.cb5.isSelected();
        tlp.pDeadline = pp.cb6.isSelected();
        tlp.pEText = pp.cb7.isSelected();

        // Idiot-proofing...
        userInt = pp.tf2.getText().trim();
        if (userInt.length() > 2) userInt = userInt.substring(0, 2).trim();
        if (userInt.length() == 0) userInt = "99";
        digit = userInt.substring(0, 1);
        if (!ints.contains(digit)) userInt = "99";
        if (userInt.length() == 2) {
            digit = userInt.substring(1, 2);
            if (!ints.contains(digit)) userInt = userInt.substring(0, 1);
        } // end if
        tlp.pCutoff = new Integer(userInt);

        tlp.lineSpace = pp.sp.getValue();

        // Sort panel
        tlp.whenNoKey = sp.getNoKey();

        return tlp;
    } // end getValues

    class PriorityPanel extends JPanel implements ChangeListener {
        private static final long serialVersionUID = -7260327081749645585L;

        JCheckBox cb1;
        JPanel fp;
        JTextField tf1;  // Max Priority

        public PriorityPanel() {
            super(new BorderLayout());
            cb1 = new JCheckBox("Show Priority");
            cb1.setFont(new Font("Serif", Font.BOLD, 16));
            add(cb1, "North");

            fp = new JPanel(new FlowLayout());
            JLabel jl = new JLabel("Max Priority (0 - 99)");
            jl.setFont(new Font("Serif", Font.BOLD, 16));
            jl.setVerticalAlignment(JLabel.CENTER);
            fp.add(jl);

            // Make a 2-character text field that accepts numeric digits only.
            //------------------------------------------------------------------
            tf1 = new JTextField(2);
            AbstractDocument doc = (AbstractDocument) tf1.getDocument();
            doc.setDocumentFilter(new FixedSizeDocumentFilter(2));

            tf1.addKeyListener(new KeyAdapter() {
                public void keyTyped(KeyEvent e) {
                    char c = e.getKeyChar();

                    if (!((c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)
                            || (c == KeyEvent.VK_ENTER) || (c == KeyEvent.VK_TAB)
                            || (Character.isDigit(c)))) {
                        e.consume();
                    }
                }
            });

            tf1.setFont(new Font("Serif", Font.BOLD, 16));
            tf1.setText(String.valueOf(tlp.maxPriority));
            //------------------------------------------------------------------
            fp.add(tf1);

            add(fp, "Center");

            fp.setVisible(false);
            cb1.addChangeListener(this);
            cb1.setSelected(tlp.showPriority);
        } // end constructor

        public void stateChanged(ChangeEvent ce) {
            if (cb1.isSelected()) fp.setVisible(true);
            else fp.setVisible(false);
        } // end stateChanged
    } // end PriorityPanel

    class printPanel extends JPanel {
        private static final long serialVersionUID = 3266279232918234594L;

        JCheckBox cb1;
        JCheckBox cb2;
        JCheckBox cb3;
        JCheckBox cb4;
        JCheckBox cb5;
        JCheckBox cb6;
        JCheckBox cb7;
        JTextField tf2;
        spacing sp;

        public printPanel() {
            setLayout(new GridBagLayout());
            GridBagConstraints gbc = new GridBagConstraints();
            gbc.gridx = 0;
            gbc.gridy = 0;
            gbc.anchor = GridBagConstraints.WEST;

            JPanel p1 = new JPanel(new GridLayout(0, 2));
            cb1 = new JCheckBox("Header (List name and Date/Time)");
            cb2 = new JCheckBox("Footer (Page count)");
            cb3 = new JCheckBox("Box Line Border");
            cb4 = new JCheckBox("Completion Space");
            cb5 = new JCheckBox("Priority");
            cb6 = new JCheckBox("Deadline");
            cb7 = new JCheckBox("Extended Text");
            cb1.setSelected(tlp.pHeader);
            cb2.setSelected(tlp.pFooter);
            cb3.setSelected(tlp.pBorder);
            cb4.setSelected(tlp.pCSpace);
            cb5.setSelected(tlp.pPriority);
            cb6.setSelected(tlp.pDeadline);
            cb7.setSelected(tlp.pEText);
            p1.add(cb1);
            p1.add(cb2);
            p1.add(cb3);
            p1.add(cb4);
            p1.add(cb5);
            // p1.add(cb6);
            p1.add(cb7);
            add(p1, gbc);
            gbc.gridy++;

            JPanel p3 = new JPanel();
            p3.add(new JLabel("Priority Cutoff"));
            tf2 = new JTextField(2);
            tf2.setText(String.valueOf(tlp.pCutoff));
            p3.add(tf2);
            add(p3, gbc);
            gbc.gridy++;

            sp = new spacing(tlp.lineSpace);
            add(sp, gbc);
            gbc.gridy++;
        } // end constructor

        class spacing extends JPanel {
            private static final long serialVersionUID = -3621786804599232799L;

            JRadioButton z;
            JRadioButton o;
            JRadioButton t;

            public spacing(int start) {
                setLayout(new FlowLayout(FlowLayout.LEFT));

                z = new JRadioButton("0");
                o = new JRadioButton("1");
                t = new JRadioButton("2");
                if (start == 0) z.setSelected(true);
                if (start == 1) o.setSelected(true);
                if (start == 2) t.setSelected(true);

                ButtonGroup bg = new ButtonGroup();
                bg.add(z);
                bg.add(o);
                bg.add(t);

                add(new JLabel("Spacing Between Items   "));
                add(z);
                add(o);
                add(t);
            } // end constructor

            public int getValue() {
                if (z.isSelected()) return 0;
                if (o.isSelected()) return 1;
                if (t.isSelected()) return 2;
                return -1;
            } // end getValue
        } // end class spacing
    } // end class printPanel

    class SortPanel extends JPanel {
        private static final long serialVersionUID = 571253855183342953L;

        JRadioButton t;
        JRadioButton b;
        JRadioButton s;

        public SortPanel(int noKey) {
            setLayout(new GridLayout(4, 0));
            JLabel prompt = new JLabel("Sorting rule for items with no sort key:");

            prompt.setFont(new Font("Serif", Font.BOLD, 14));

            t = new JRadioButton("Place at Top");
            b = new JRadioButton("Place at Bottom");
            s = new JRadioButton("Leave in Place");
            if (noKey == 0) t.setSelected(true);
            if (noKey == 1) b.setSelected(true);
            if (noKey == 2) s.setSelected(true);

            ButtonGroup bg = new ButtonGroup();
            bg.add(t);
            bg.add(b);
            bg.add(s);

            add(prompt);
            add(t);
            add(b);
            add(s);
        } // end constructor

        public int getNoKey() {
            if (t.isSelected()) return 0;
            if (b.isSelected()) return 1;
            if (s.isSelected()) return 2;
            return -1;
        } // end getValue
    } // end class SortPanel

} // end TodoOpts
