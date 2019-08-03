//
// This Panel will act as a User-Interface from which to specify
//   the format of a time.
//

import java.awt.*;
import java.awt.event.*;
import java.util.*;            // Date
import javax.swing.*;

public class TimeFormatPanel extends JPanel implements ItemListener {
    private static final long serialVersionUID = 9181129054094982937L;

    static TimeFormatBar tfb;
    JCheckBox jcb1;
    JCheckBox jcb2;
    JCheckBox jcb3;
    JCheckBox jcb4;
    JCheckBox jcb5;
    int visibility;

    public TimeFormatPanel() {
        super(new BorderLayout());

        JPanel westPanel = new JPanel(new GridLayout(0, 1, 0, 0));
        JPanel eastPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        tfb = new TimeFormatBar();

        // Date Format categories -
        jcb1 = new JCheckBox("Hours");
        jcb2 = new JCheckBox("Minutes");
        jcb3 = new JCheckBox("Seconds");
        jcb4 = new JCheckBox("AM/PM");
        jcb5 = new JCheckBox("TZ");

        jcb1.addItemListener(this);
        jcb2.addItemListener(this);
        jcb3.addItemListener(this);
        jcb4.addItemListener(this);
        jcb5.addItemListener(this);

        westPanel.add(jcb1);
        westPanel.add(jcb2);
        westPanel.add(jcb3);
        westPanel.add(jcb4);
        westPanel.add(jcb5);
        add(westPanel, "West");

        eastPanel.add(tfb);
        add(eastPanel, "Center");
    } // end constructor

    public long getDate() {
        return tfb.getDate();
    } // end getDate

    public String getFormat() {
        return tfb.getFormat();
    } // end getFormat

    public String getRealFormat(String s) {
        return TimeFormatBar.getRealFormat(s);
    } // end getRealFormat

    public void itemStateChanged(ItemEvent ie) {
        JCheckBox jcb = (JCheckBox) ie.getItem();
        int newState = ie.getStateChange();
        int adjust = (newState == ItemEvent.SELECTED) ? 1 : -1;
        if (jcb == jcb1) visibility += adjust * 1;
        if (jcb == jcb2) visibility += adjust * 2;
        if (jcb == jcb3) visibility += adjust * 4;
        if (jcb == jcb4) visibility += adjust * 8;
        if (jcb == jcb5) visibility += adjust * 16;
        tfb.setVisibility(visibility);
        tfb.resetDateLabel();
    } // end itemStateChanged

    public void setCheckBoxes() {
        jcb1.removeItemListener(this);
        jcb2.removeItemListener(this);
        jcb3.removeItemListener(this);
        jcb4.removeItemListener(this);
        jcb5.removeItemListener(this);
        jcb1.setSelected((visibility & 1) != 0);
        jcb2.setSelected((visibility & 2) != 0);
        jcb3.setSelected((visibility & 4) != 0);
        jcb4.setSelected((visibility & 8) != 0);
        jcb5.setSelected((visibility & 16) != 0);
        jcb1.addItemListener(this);
        jcb2.addItemListener(this);
        jcb3.addItemListener(this);
        jcb4.addItemListener(this);
        jcb5.addItemListener(this);
    } // end setCheckBoxes

    public void setup(long initialDate, String format) {
        // Update this panel UI and all contents.
        SwingUtilities.updateComponentTreeUI(this);

        tfb.setup(initialDate, format);
        visibility = tfb.getVisibilityFromFormat();
        setCheckBoxes(); // Set checkboxes according to visibility.
    } // end setup

    public static void main(String argv[]) {
        String laf = UIManager.getSystemLookAndFeelClassName();
        try {
            UIManager.setLookAndFeel(laf);
        } catch (UnsupportedLookAndFeelException ulafe) {
        } catch (ClassNotFoundException cnfe) {
        } catch (IllegalAccessException iae) {
        } catch (InstantiationException ie) {
        }

        TimeFormatPanel dfp = new TimeFormatPanel();
        long dl = new Date().getTime();
        String df = "";
        dfp.setup(dl, df);

        int choice = JOptionPane.showConfirmDialog(
                new JPanel(),                 // parent component - for modality
                dfp,                          // UI Object
                "Specify a time format",      // pane title bar
                JOptionPane.OK_CANCEL_OPTION, // Option type
                JOptionPane.QUESTION_MESSAGE, // Message type
                null);                       // icon

        if (choice != JOptionPane.OK_OPTION) System.exit(0);
        df = dfp.getFormat(); // see TimeFormatBar for format expl.
        dl = dfp.getDate();
        String s = TimeFormatBar.getDateString(dl, df);
        System.out.println("End result: " + s);
        System.exit(0);
    } // end main
} // end class TimeFormatPanel

