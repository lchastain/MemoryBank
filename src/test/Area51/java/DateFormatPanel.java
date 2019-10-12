//
// This Panel will act as a User-Interface from which to specify
//   the format of a date.
//
import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.time.ZonedDateTime;

public class DateFormatPanel extends JPanel implements ItemListener {
    private static final long serialVersionUID = 1L;

    private static DateFormatBar dfb;
    private JCheckBox jcb1;
    private JCheckBox jcb3;
    private JCheckBox jcb4;
    private JCheckBox jcb5;
    private JCheckBox jcb6;
    private JCheckBox jcb7;
    private int visibility;

    DateFormatPanel() {
        super(new BorderLayout());

        JPanel westPanel = new JPanel(new GridLayout(0, 1, 0, 0));
        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        dfb = new DateFormatBar();

        // Date Format categories -
        jcb1 = new JCheckBox("Day");
        jcb3 = new JCheckBox("Month");
        jcb4 = new JCheckBox("Date");
        jcb5 = new JCheckBox("Time");
        jcb6 = new JCheckBox("Year");
        jcb7 = new JCheckBox("Era");

        jcb1.addItemListener(this);
        jcb3.addItemListener(this);
        jcb4.addItemListener(this);
        jcb5.addItemListener(this);
        jcb6.addItemListener(this);
        jcb7.addItemListener(this);

        westPanel.add(jcb1);
        westPanel.add(jcb3);
        westPanel.add(jcb4);
        westPanel.add(jcb5);
        westPanel.add(jcb6);
        westPanel.add(jcb7);
        add(westPanel, "West");

        eastPanel.add(dfb);
        add(eastPanel, "Center");
    } // end constructor

    long getDate() {
        return dfb.getDate();
    } // end getDate

    String getFormat() {
        return dfb.getFormat();
    } // end getFormat


    public void itemStateChanged(ItemEvent ie) {
        JCheckBox jcb = (JCheckBox) ie.getItem();
        int newState = ie.getStateChange();
        int adjust = (newState == ItemEvent.SELECTED) ? 1 : -1;
        if (jcb == jcb1) visibility += adjust;
        if (jcb == jcb3) visibility += adjust * 4;
        if (jcb == jcb4) visibility += adjust * 8;
        if (jcb == jcb5) visibility += adjust * 16;
        if (jcb == jcb6) visibility += adjust * 32;
        if (jcb == jcb7) visibility += adjust * 64;
        dfb.setVisibility(visibility);
        dfb.resetDateLabel();
    } // end itemStateChanged

    private void setCheckBoxes() {
        jcb1.removeItemListener(this);
        jcb3.removeItemListener(this);
        jcb4.removeItemListener(this);
        jcb5.removeItemListener(this);
        jcb6.removeItemListener(this);
        jcb7.removeItemListener(this);
        jcb1.setSelected((visibility & 1) != 0);
        jcb3.setSelected((visibility & 4) != 0);
        jcb4.setSelected((visibility & 8) != 0);
        jcb5.setSelected((visibility & 16) != 0);
        jcb6.setSelected((visibility & 32) != 0);
        jcb7.setSelected((visibility & 64) != 0);
        jcb1.addItemListener(this);
        jcb3.addItemListener(this);
        jcb4.addItemListener(this);
        jcb5.addItemListener(this);
        jcb6.addItemListener(this);
        jcb7.addItemListener(this);
    } // end setCheckBoxes

    public void setup(long initialDate, String format) {
        // Update this panel UI and all contents.
        SwingUtilities.updateComponentTreeUI(this);

        dfb.setup(initialDate, format);
        visibility = dfb.getVisibilityFromFormat();
        setCheckBoxes(); // Set checkboxes according to visibility.
    } // end setup


    public static void main(String[] argv) {
        DateFormatPanel dfp = new DateFormatPanel();
        long dl = ZonedDateTime.now().toEpochSecond();
        String df = "";
        dfp.setup(dl, df);

        int choice = JOptionPane.showConfirmDialog(
                new JPanel(),                 // parent component - for modality
                dfp,                          // UI Object
                "Specify a date format",      // pane title bar
                JOptionPane.OK_CANCEL_OPTION, // Option type
                JOptionPane.QUESTION_MESSAGE, // Message type
                null);                       // icon

        if (choice != JOptionPane.OK_OPTION) System.exit(0);
        df = dfp.getFormat(); // see DateFormatBar for format expl.
        dl = dfp.getDate();
        String s = FormatUtil.getDateString(dl, df);
        System.out.println("End result: " + s);
        System.exit(0);
    } // end main
} // end class DateFormatPanel





