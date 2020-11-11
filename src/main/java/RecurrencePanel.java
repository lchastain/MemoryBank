import javax.swing.*;
import javax.swing.text.AbstractDocument;
import java.awt.*;
import java.awt.event.*;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class RecurrencePanel extends JPanel implements
        ActionListener, FocusListener, ItemListener {
    private static final long serialVersionUID = 1L;

    private static DateTimeFormatter dtf;

    private int intPreferredWidth;    // See note in constructor.
    private int intPreferredHeight;   // See note in constructor.
    private LocalDate dateStart;  // Used in itemStateChanged and recalcEnd
    private LocalDate dateStopBy;
    private int intStopAfter;
    LabelButton btnStopBy;
    private boolean blnHandleItems;
    static Notifier optionPane;

    //-----
    private JLabel lblStartDate;
    private JRadioButton rbtnNone;
    private JRadioButton rbtnDay;
    private JTextField txtfDayInterval;
    private JRadioButton rbtnWeek;
    private JCheckBox chkboxSunday;
    private JCheckBox chkboxMonday;
    private JCheckBox chkboxTuesday;
    private JCheckBox chkboxWednesday;
    private JCheckBox chkboxThursday;
    private JCheckBox chkboxFriday;
    private JCheckBox chkboxSaturday;
    private JTextField txtfWeekInterval;
    private JRadioButton rbtnMonth;
    private JTextField txtfMonthInterval;
    private JComboBox<String> comboxMonth;
    //-----
    private JRadioButton rbtnYear;
    private JComboBox<String> comboxYear;
    //-----
    private JPanel pnlEnd;
    //-----
    private JRadioButton rbtnForever;
    private JRadioButton rbtnStopAfter;
    private JTextField txtfStopAfter;
    //-----
    private JLabel lblStopBy;
    private JRadioButton rbtnStopBy;
    JPanel pnlStopBy;
    //-----
    // End of variables declaration

    static {
        optionPane = new Notifier() { }; // Uses all default methods.
    }

    public RecurrencePanel() {
        super();
        initializeComponent();

        // Although size was set explicitly in initializeComponent, each call to getSize()
        //   reported a smaller amount by (6,25) after each time the dialog was closed.
        //   This method of capturing the initial size is the workaround.  Now the
        //   EventNoteGroup calls getMinimumSize (defined below) to get the size value.
        intPreferredWidth = getSize().width;
        intPreferredHeight = getSize().height;

        blnHandleItems = true;
        reinitializeComponent();
    }

    //----------------------------------------------------------
    // Method Name: actionPerformed
    //
    // Either 'Enter' was pressed on a text field, or a combo box
    //   selection has been made.  OR - a test came here directly.
    //----------------------------------------------------------
    public void actionPerformed(ActionEvent e) {
        // System.out.println("actionPerformed: " + e.getActionCommand());
        if (blnHandleItems) recalcEnd();
    } // end actionPerformed


    /**
     * Add Component Without a Layout Manager (Absolute Positioning)
     */
    private void addComponent(Container container, Component c, int x, int y, int width, int height) {
        c.setBounds(x, y, width, height);
        container.add(c);
    } // end addComponent


    public void focusGained(FocusEvent e) {
        String strSource = e.getComponent().getName();
        if (strSource == null) return;

        // System.out.println(strSource);
        switch (strSource) {
            case "txtfDayInterval":
                rbtnDay.setSelected(true);
                txtfDayInterval.requestFocus();
                break;
            case "txtfWeekInterval":
                rbtnWeek.setSelected(true);
                txtfWeekInterval.requestFocus();
                break;
            case "txtfMonthInterval":
                rbtnMonth.setSelected(true);
                txtfMonthInterval.requestFocus();
                break;
            case "txtfStopAfter":
                rbtnStopAfter.setSelected(true);
                txtfStopAfter.requestFocus();
                break;
            case "comboxMonth":
                rbtnMonth.setSelected(true);
                break;
            case "comboxYear":
                rbtnYear.setSelected(true);
                break;
        }
    } // end focusGained

    public void focusLost(FocusEvent e) {
        // Check the 4 text fields for newly entered values.
        String strSource = e.getComponent().getName();
        if (strSource == null) return;

        // System.out.println("focusLost on " + strSource);
        if (blnHandleItems) recalcEnd();
    } // focusLost


    // This method is only here to 'fool' JFramebuilder-generated code.
    public JPanel getContentPane() {
        return this;
    }

    //------------------------------------------------------------
    // Method Name: getEndDateAfterMonths
    //
    // Calculates and returns the end date that occurs after
    //   the specified number of input months, adjusted to
    //   fit the pattern.
    //------------------------------------------------------------
    private LocalDate getEndDateAfterMonths(int months, String strMonthPattern) {
        LocalDate dateTheEndDate;

        // Get our start day, for multiple uses below.
        String strWhichOne = "first";
        int intDayOfWeek = AppUtil.getDayOfWeekInt(dateStart);

        // Keep the last known 'good' date, as we scan forward.
        LocalDate dateGood;


        // This calculation works for a simple numeric date and
        // does not consider the Monthly pattern.
//        calTmp.setNotes(Calendar.MONTH, months);
//        dateTheEndDate = calTmp.getTime();
        dateTheEndDate = dateStart.plusMonths(months);

        // Preserve the month value.
//        int intMonth = calTmp.get(Calendar.MONTH);
        int intMonth = dateTheEndDate.getMonthValue() - 1;

        // Examine the user-selected recurrence pattern.
        if (Character.isDigit(strMonthPattern.charAt(4))) {
            // If the pattern is the simple numeric, then we are done.
            assert true;
        } else if (strMonthPattern.toLowerCase().contains("weekend")) {
            // System.out.println("generalized - weekend");
            // Now set the calendar to the first one in this month -
            while (isWeekday(dateTheEndDate)) dateTheEndDate = dateTheEndDate.plusDays(1);
            dateGood = dateTheEndDate;
            // System.out.println("Adjusted to correct day: " + calTmp.getTime());

            while (!strMonthPattern.toLowerCase().contains(strWhichOne)) {
                switch (strWhichOne) {
                    case "first":
                        strWhichOne = "second";
                        break;
                    case "second":
                        strWhichOne = "third";
                        break;
                    case "third":
                        strWhichOne = "fourth";
                        break;
                    default:
                        strWhichOne = "keep going...";
                        break;
                }

                dateTheEndDate = dateTheEndDate.plusDays(1);

                // and keep going, if we need to,
                // to get to the next weekend day.
                while (isWeekday(dateTheEndDate)) dateTheEndDate = dateTheEndDate.plusDays(1);

                // System.out.println(strWhichOne + " " + calTmp.getTime());
                if (dateTheEndDate.getMonthValue() - 1 != intMonth) {
                    // System.out.println("Shot past - resetting.");
                    dateTheEndDate = dateGood;
                    break;
                } else {
                    if (!isWeekday(dateTheEndDate)) dateGood = dateTheEndDate;
                } // end if/else
            } // end while
        } else if (strMonthPattern.toLowerCase().contains("weekday")) {
            // System.out.println("generalized - weekday");
            // Now set the calendar to the first one in this month -
            dateTheEndDate = dateTheEndDate.withDayOfMonth(1);
            while (!isWeekday(dateTheEndDate)) dateTheEndDate = dateTheEndDate.plusDays(1);
            dateGood = dateTheEndDate;
            // System.out.println("Adjusted to correct day: " + calTmp.getTime());

            while (!strMonthPattern.contains(strWhichOne)) {
                switch (strWhichOne) {
                    case "first":
                        strWhichOne = "second";
                        break;
                    case "second":
                        strWhichOne = "third";
                        break;
                    case "third":
                        strWhichOne = "fourth";
                        break;
                    default:
                        strWhichOne = "keep going...";
                        break;
                }

//                calTmp.setNotes(Calendar.DATE, 1); // add a day
                dateTheEndDate = dateTheEndDate.plusDays(1);

                // and keep going, if we need to,
                // to get to the next weekend day.
//                while (!isWeekday(calTmp)) calTmp.setNotes(Calendar.DATE, 1);
                while (!isWeekday(dateTheEndDate)) dateTheEndDate = dateTheEndDate.plusDays(1);

                // System.out.println(strWhichOne + " " + calTmp.getTime());
                if (dateTheEndDate.getMonthValue() - 1 != intMonth) {
                    // System.out.println("Shot past - resetting.");
//                    calTmp.setTime(dateGood);
                    dateTheEndDate = dateGood;
                    break;
                } else {
//                    if (isWeekday(calTmp)) dateGood = calTmp.getTime();
                    if (isWeekday(dateTheEndDate)) dateGood = dateTheEndDate;
                } // end if/else
            } // end while
//            dateTheEndDate = calTmp.getTime();
        } else if (strMonthPattern.toLowerCase().contains("last day")) {
            // System.out.println("last day");
            while (true) {
//                dateGood = calTmp.getTime();
                dateGood = dateTheEndDate;
//                calTmp.setNotes(Calendar.DATE, 1); // add a day
                dateTheEndDate = dateTheEndDate.plusDays(1);

//                if (calTmp.get(Calendar.MONTH) != intMonth) {
                if (dateTheEndDate.getMonthValue() - 1 != intMonth) {
                    // System.out.println("Shot past - resetting.");
//                    calTmp.setTime(dateGood);
                    dateTheEndDate = dateGood;
                    break;
                } // end if
            } // end while
        } else {
            // System.out.println("specific day");
            // Now set the calendar to the first one in this month -
            dateTheEndDate = dateTheEndDate.withDayOfMonth(1);
            while (AppUtil.getDayOfWeekInt(dateTheEndDate) != intDayOfWeek) {
                dateTheEndDate = dateTheEndDate.plusDays(1);
            } // end while
            // System.out.println("Adjusted to correct day: " + calTmp.getTime());

            while (!strMonthPattern.contains(strWhichOne)) {
                switch (strWhichOne) {
                    case "first":
                        strWhichOne = "second";
                        break;
                    case "second":
                        strWhichOne = "third";
                        break;
                    case "third":
                        strWhichOne = "fourth";
                        break;
                    default:
                        strWhichOne = "keep going...";
                        break;
                }

                dateGood = dateTheEndDate;
                dateTheEndDate = dateTheEndDate.plusWeeks(1);

                // System.out.println(strWhichOne + " " + calTmp.getTime());
                if (dateTheEndDate.getMonthValue() - 1 != intMonth) {
                    // System.out.println("Shot past - resetting.");
                    dateTheEndDate = dateGood;
                    break;
                } // end if
            } // end while
        } // end if/else - general or specific or last

        return dateTheEndDate;
    } // end getEndDateAfterMonths

    // Needed to avoid possible NumberFormatExceptions
    private int getIntFromTextField(JTextField jtf) {
        String s = jtf.getText();
        if (s == null) return 0;
        s = s.trim();
        if(s.isEmpty()) return 0;
        return Integer.parseInt(s);
    }

    public Dimension getMinimumSize() {
        return new Dimension(intPreferredWidth, intPreferredHeight);
    } // end getMinimumSize


    // Used by the static JOptionPane 'show' methods.
    public Dimension getPreferredSize() {
        return getMinimumSize();
    }


    //-----------------------------------------------------------
    // Method Name: getRecurrenceSetting
    //
    // Should be called by the context that most recently
    //   displayed the panel, immediately after verifying
    //   that the recurrence is valid.  DO NOT call this
    //   method if !isRecurrenceValid().
    //-----------------------------------------------------------
    public String getRecurrenceSetting() {
        String strRecurSetting;
        String strEndRange;
        String strTmp;

        // First, set the Range End.  Invalid
        //   combinations here will be interpreted
        //   as 'Unending'.
        //----------------------------------------
        if (rbtnStopAfter.isSelected()) {
            strTmp = txtfStopAfter.getText();
            if ((strTmp.equals("")) || (Integer.parseInt(strTmp) == 0)) {
                strEndRange = "";  // Unending
            } else {
                if (Integer.parseInt(strTmp) == 1) {
                    // Stop after 1 time = no recurrence.
                    rbtnNone.setSelected(true);
                    strEndRange = "";  // Unending
                } else {
                    strEndRange = strTmp;  // digits as string
                } // end if
            } // end if
        } else if (rbtnStopBy.isSelected()) {
            if (dateStopBy == null) {
                strEndRange = "";  // Unending
            } else {
                if (dateStopBy.isBefore(dateStart)) {
                    // Stop before start = no recurrence.
                    rbtnNone.setSelected(true);
                    strEndRange = "";  // Unending
                } else {
                    dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                    strEndRange = dtf.format(dateStopBy);
                }
            } // end if
        } else {
            strEndRange = "";  // Unending
        } // end if

        if (rbtnDay.isSelected()) {
            strRecurSetting = "D";
            strRecurSetting += txtfDayInterval.getText().trim();
            strRecurSetting += "_";
            strRecurSetting += strEndRange;
        } else if (rbtnWeek.isSelected()) {
            strRecurSetting = "W";
            strRecurSetting += txtfWeekInterval.getText().trim();
            strRecurSetting += "_";
            if (chkboxSunday.isSelected()) strRecurSetting += "Su";
            if (chkboxMonday.isSelected()) strRecurSetting += "Mo";
            if (chkboxTuesday.isSelected()) strRecurSetting += "Tu";
            if (chkboxWednesday.isSelected()) strRecurSetting += "We";
            if (chkboxThursday.isSelected()) strRecurSetting += "Th";
            if (chkboxFriday.isSelected()) strRecurSetting += "Fr";
            if (chkboxSaturday.isSelected()) strRecurSetting += "Sa";
            strRecurSetting += "_";
            strRecurSetting += strEndRange;
        } else if (rbtnMonth.isSelected()) {
            strRecurSetting = "M";
            strRecurSetting += txtfMonthInterval.getText().trim();
            strRecurSetting += "_";
            strTmp = (String) comboxMonth.getSelectedItem();
            strRecurSetting += strTmp;
            strRecurSetting += "_";
            strRecurSetting += strEndRange;
        } else if (rbtnYear.isSelected()) {
            strRecurSetting = "Y_";
            strTmp = (String) comboxYear.getSelectedItem();
            strRecurSetting += strTmp;
            strRecurSetting += "_";
            strRecurSetting += strEndRange;
        } else { //  (rbtnNone.isSelected()
            strRecurSetting = "";
        } // end if - which recurrence type
        return strRecurSetting;
    } // end getRecurrenceSetting


    private void initializeComponent() {
        JPanel contentPane = this.getContentPane();
        //-----
        JPanel pnlInterval = new JPanel();
        //-----
        lblStartDate = new JLabel();
        rbtnNone = new JRadioButton();
        JPanel pnlNone = new JPanel();
        //-----
        JLabel jLabel1 = new JLabel();
        rbtnDay = new JRadioButton();
        txtfDayInterval = new JTextField();
        JPanel pnlDay = new JPanel();
        //-----
        JLabel jLabel3 = new JLabel();
        rbtnWeek = new JRadioButton();
        chkboxSunday = new JCheckBox();
        chkboxMonday = new JCheckBox();
        chkboxTuesday = new JCheckBox();
        chkboxWednesday = new JCheckBox();
        chkboxThursday = new JCheckBox();
        chkboxFriday = new JCheckBox();
        chkboxSaturday = new JCheckBox();
        txtfWeekInterval = new JTextField();
        JPanel pnlWeek = new JPanel();
        //-----
        JLabel jLabel5 = new JLabel();
        JLabel jLabel6 = new JLabel();
        rbtnMonth = new JRadioButton();
        txtfMonthInterval = new JTextField();
        comboxMonth = new JComboBox<>();
        JPanel pnlMonth = new JPanel();
        //-----
        rbtnYear = new JRadioButton();
        comboxYear = new JComboBox<>();
        JPanel pnlYear = new JPanel();
        //-----
        pnlEnd = new JPanel();
        //-----
        rbtnForever = new JRadioButton();
        JPanel jPanel15 = new JPanel();
        //-----
        JLabel jLabel8 = new JLabel();
        rbtnStopAfter = new JRadioButton();
        txtfStopAfter = new JTextField();
        JPanel jPanel16 = new JPanel();
        //-----
        lblStopBy = new JLabel();
        rbtnStopBy = new JRadioButton();
        pnlStopBy = new JPanel();
        //-----

        //
        // contentPane
        //
        contentPane.setLayout(null);
        addComponent(contentPane, pnlInterval, 10, 10, 298, 240);
        addComponent(contentPane, pnlEnd, 10, 255, 298, 80);
        //
        // pnlInterval
        //
        pnlInterval.setLayout(null);
        pnlInterval.setBorder(BorderFactory.createEtchedBorder());
        addComponent(pnlInterval, pnlNone, 2, 2, 288, 33);
        addComponent(pnlInterval, pnlDay, 2, 35, 259, 33);
        addComponent(pnlInterval, pnlWeek, 2, 68, 262, 72);
        addComponent(pnlInterval, pnlMonth, 2, 140, 261, 62);
        addComponent(pnlInterval, pnlYear, 2, 202, 289, 32);
        //
        // lblStartDate
        //
        lblStartDate.setHorizontalAlignment(SwingConstants.RIGHT);
        lblStartDate.setHorizontalTextPosition(SwingConstants.RIGHT);
        lblStartDate.setText("Start Date");
        //
        // rbtnNone
        //
        rbtnNone.setText("Do Not Repeat");
        rbtnNone.setSelected(true);
        //
        // pnlNone
        //
        pnlNone.setLayout(null);
        addComponent(pnlNone, lblStartDate, 130, 6, 151, 15);
        addComponent(pnlNone, rbtnNone, 2, 5, 98, 23);
        //
        // jLabel1
        //
        jLabel1.setText("Day Intervals");
        //
        // rbtnDay
        //
        rbtnDay.setText("Repeat at");
        rbtnDay.setSelected(true);
        //
        // txtfDayInterval
        //
        txtfDayInterval.setHorizontalAlignment(JTextField.CENTER);
        txtfDayInterval.setText(" 999");
        //
        // pnlDay
        //
        pnlDay.setLayout(null);
        addComponent(pnlDay, jLabel1, 110, 9, 92, 15);
        addComponent(pnlDay, rbtnDay, 2, 5, 76, 23);
        addComponent(pnlDay, txtfDayInterval, 78, 6, 27, 21);
        //
        // jLabel3
        //
        jLabel3.setText(" Week Intervals, on:");
        //
        // rbtnWeek
        //
        rbtnWeek.setText("Repeat at");
        //
        // chkboxSunday
        //
        chkboxSunday.setHorizontalTextPosition(SwingConstants.CENTER);
        chkboxSunday.setText("Sun");
        chkboxSunday.setVerticalTextPosition(SwingConstants.TOP);
        //
        // chkboxMonday
        //
        chkboxMonday.setHorizontalTextPosition(SwingConstants.CENTER);
        chkboxMonday.setText("Mon");
        chkboxMonday.setVerticalTextPosition(SwingConstants.TOP);
        //
        // chkboxTuesday
        //
        chkboxTuesday.setHorizontalTextPosition(SwingConstants.CENTER);
        chkboxTuesday.setText("Tue");
        chkboxTuesday.setVerticalTextPosition(SwingConstants.TOP);
        //
        // chkboxWednesday
        //
        chkboxWednesday.setHorizontalTextPosition(SwingConstants.CENTER);
        chkboxWednesday.setText("Wed");
        chkboxWednesday.setVerticalTextPosition(SwingConstants.TOP);
        //
        // chkboxThursday
        //
        chkboxThursday.setHorizontalTextPosition(SwingConstants.CENTER);
        chkboxThursday.setText("Thu");
        chkboxThursday.setVerticalTextPosition(SwingConstants.TOP);
        //
        // chkboxFriday
        //
        chkboxFriday.setHorizontalTextPosition(SwingConstants.CENTER);
        chkboxFriday.setText("Fri");
        chkboxFriday.setVerticalTextPosition(SwingConstants.TOP);
        //
        // chkboxSaturday
        //
        chkboxSaturday.setHorizontalTextPosition(SwingConstants.CENTER);
        chkboxSaturday.setText("Sat");
        chkboxSaturday.setVerticalTextPosition(SwingConstants.TOP);
        //
        // txtfWeekInterval
        //
        txtfWeekInterval.setHorizontalAlignment(JTextField.CENTER);
        txtfWeekInterval.setText(" 99");
        //
        // pnlWeek
        //
        pnlWeek.setLayout(null);
        addComponent(pnlWeek, jLabel3, 105, 6, 106, 18);
        addComponent(pnlWeek, rbtnWeek, 2, 5, 76, 23);
        addComponent(pnlWeek, chkboxSunday, 21, 28, 30, 35);
        addComponent(pnlWeek, chkboxMonday, 55, 28, 30, 35);
        addComponent(pnlWeek, chkboxTuesday, 89, 28, 30, 35);
        addComponent(pnlWeek, chkboxWednesday, 123, 28, 30, 35);
        addComponent(pnlWeek, chkboxThursday, 156, 28, 30, 35);
        addComponent(pnlWeek, chkboxFriday, 191, 28, 30, 35);
        addComponent(pnlWeek, chkboxSaturday, 220, 28, 30, 35);
        addComponent(pnlWeek, txtfWeekInterval, 81, 5, 20, 22);
        //
        // jLabel5
        //
        jLabel5.setText(" of the month");
        //
        // jLabel6
        //
        jLabel6.setText(" Month Intervals, on");
        //
        // rbtnMonth
        //
        rbtnMonth.setOpaque(false);
        rbtnMonth.setText("Repeat at");
        //
        // txtfMonthInterval
        //
        txtfMonthInterval.setHorizontalAlignment(JTextField.CENTER);
        txtfMonthInterval.setText("99");
        //
        // comboxMonth
        //
        comboxMonth.addItem("the 31st");
        comboxMonth.addItem("the last Wednesday");
        comboxMonth.addItem("the fourth Wednesday");
        comboxMonth.addItem("the last Weekday");
        comboxMonth.addItem("the last Weekend Day");
        comboxMonth.setOpaque(false);
        //
        // pnlMonth
        //
        pnlMonth.setLayout(null);
        addComponent(pnlMonth, jLabel5, 188, 35, 66, 15);
        addComponent(pnlMonth, jLabel6, 103, 6, 117, 18);
        addComponent(pnlMonth, rbtnMonth, 2, 5, 76, 23);
        addComponent(pnlMonth, txtfMonthInterval, 79, 5, 20, 22);
        addComponent(pnlMonth, comboxMonth, 23, 34, 162, 21);
        //
        // rbtnYear
        //
        rbtnYear.setOpaque(false);
        rbtnYear.setText("Yearly, on");
        rbtnYear.setSelected(true);
        //
        // comboxYear
        //
        comboxYear.addItem("the 31st of January");
        comboxYear.addItem("the last Wednesday in January");
        comboxYear.addItem("Easter");
        comboxYear.addItem("Thanksgiving");
        comboxYear.addItem("the second Weekday in January");
        //
        // pnlYear
        //
        pnlYear.setLayout(null);
        addComponent(pnlYear, rbtnYear, 2, 5, 78, 23);
        addComponent(pnlYear, comboxYear, 78, 5, 211, 21);
        //
        // pnlEnd
        //
        pnlEnd.setLayout(null);
        pnlEnd.setBorder(BorderFactory.createEtchedBorder());
        addComponent(pnlEnd, jPanel15, 2, 2, 196, 24);
        addComponent(pnlEnd, jPanel16, 2, 26, 193, 24);
        addComponent(pnlEnd, pnlStopBy, 2, 50, 262, 24);
        //
        // rbtnForever
        //
        rbtnForever.setText("Repeat Indefinitely");
        rbtnForever.setSelected(true);
        //
        // jPanel15
        //
        jPanel15.setLayout(new BoxLayout(jPanel15, BoxLayout.X_AXIS));
        jPanel15.add(rbtnForever, 0);
        //
        // jLabel8
        //
        jLabel8.setText(" times");
        //
        // rbtnStopAfter
        //
        rbtnStopAfter.setOpaque(false);
        rbtnStopAfter.setText("Stop after");
        //
        // txtfStopAfter
        //
        txtfStopAfter.setHorizontalAlignment(JTextField.CENTER);
        txtfStopAfter.setText(" 999");
        //
        // jPanel16
        //
        jPanel16.setLayout(null);
        addComponent(jPanel16, jLabel8, 105, 3, 34, 15);
        addComponent(jPanel16, rbtnStopAfter, 0, 0, 86, 23);
        addComponent(jPanel16, txtfStopAfter, 73, 0, 27, 21);
        //
        // lblStopBy
        //
        lblStopBy.setText(" <Select a Date>");
        //
        // rbtnStopBy
        //
        rbtnStopBy.setText("Stop by ");
        //
        // pnlStopBy
        //
        pnlStopBy.setLayout(null);
        addComponent(pnlStopBy, lblStopBy, 70, 2, 181, 18);
        addComponent(pnlStopBy, rbtnStopBy, -1, 0, 71, 23);
        //
        // RecurrencePanel
        //
        this.setLocation(new Point(4, 10));
        this.setSize(new Dimension(325, 374));
    } // end initializeComponent - JFrameBuilder generated code.


    //----------------------------------------------------------
    // Method Name: isRecurrenceValid
    //
    // Check the user settings and determine if they are
    //   effective and should be used.
    //----------------------------------------------------------
    public boolean isRecurrenceValid() {
        boolean blnIsIt = true;

        int intInterval;
        String strTmp;

        // Day interval
        if (rbtnDay.isSelected()) {
            strTmp = txtfDayInterval.getText();
            if (strTmp.equals("")) {
                // System.out.println("Day interval is empty");
                blnIsIt = false;  // Day interval not specified
            } else {
                // System.out.println("Day interval: " + strTmp);
                intInterval = Integer.parseInt(strTmp);
                if (intInterval == 0) blnIsIt = false;
            } // end if empty input
        } // end if Day

        // Week interval
        if (rbtnWeek.isSelected()) {
            strTmp = txtfWeekInterval.getText();
            if (strTmp == null) {
                blnIsIt = false;
            } else {
                strTmp = strTmp.trim();
                if (strTmp.equals("")) {
                    // System.out.println("Week interval is empty");
                    blnIsIt = false;
                } else {
                    // System.out.println("Week interval: " + strTmp);
                    intInterval = Integer.parseInt(strTmp);
                    if (intInterval == 0) blnIsIt = false;
                } // end if empty input
            }
        } // end if Week

        // Month interval
        if (rbtnMonth.isSelected()) {
            strTmp = txtfMonthInterval.getText();
            if (strTmp.equals("")) {
                // System.out.println("Month interval is empty");
                blnIsIt = false;
            } else {
                // System.out.println("Month interval: " + strTmp);
                intInterval = Integer.parseInt(strTmp);
                if (intInterval == 0) {
                    blnIsIt = false;
                } else {
                    strTmp = (String) comboxMonth.getSelectedItem();
                    if (strTmp == null) {
                        // System.out.println("Monthly pattern not selected");
                        blnIsIt = false;
                    } // end if no pattern
                } // end if zero interval
            } // end if empty input
        } // end if Month

        // Year interval
        if (rbtnYear.isSelected()) {
            strTmp = (String) comboxYear.getSelectedItem();
            if (strTmp == null) {
                // System.out.println("Yearly pattern not selected");
                blnIsIt = false;
            } // end if no pattern
        } // end if Year

        return blnIsIt;
    } // end isRecurrenceValid


    static boolean isWeekday(LocalDate theDay) {
        boolean result = true;
        if (theDay.getDayOfWeek() == DayOfWeek.SATURDAY) result = false;
        if (theDay.getDayOfWeek() == DayOfWeek.SUNDAY) result = false;

        return result;
    } // end isWeekday

    public void itemStateChanged(ItemEvent e) {
        if (!blnHandleItems) return;

        String strItemName;

        JComponent jc = (JComponent) e.getSource();

        strItemName = jc.getName();

        // The 'day' checkboxes did not get a Name set.
        if (strItemName == null) {
            // Make sure that the 'weekly' recurrence is selected.
            rbtnWeek.setSelected(true);

            recalcEnd();
            return;
        }

        if (e.getStateChange() == ItemEvent.DESELECTED) return;

        // System.out.println("Name = " + strItemName);

        // end if
        pnlEnd.setVisible(!strItemName.equals("rbtnNone"));

        recalcEnd();
    } // end itemStateChanged


    //-------------------------------------------------------------
    // Method Name: recalcEnd
    //
    // This method is called when various interface interactions
    //   might cause a different value to be displayed in either
    //   the 'Stop After' (how many times) or 'Stop By'
    //   (a specific date) fields.
    //-------------------------------------------------------------
    private void recalcEnd() {
        if (rbtnForever.isSelected()) {
            txtfStopAfter.setText("");
            dateStopBy = null;
            intStopAfter = 0;
            btnStopBy.setText(" <Select a Date>");
            return; // Do not End
        } // end if

        if (rbtnStopAfter.isSelected()) {
            intStopAfter = getIntFromTextField(txtfStopAfter);
            if (intStopAfter == 0) return;
        } // end if

        if (rbtnStopBy.isSelected()) {
            if (dateStopBy == null) {
                // System.out.println("Stop By is null");
                return;  // Stop By not specified
            } else {
                System.out.print("");
                // System.out.println("Stop By: " + dateStopBy);
            }
        } // end if

        if (!isRecurrenceValid()) return;

        // If we've made it past the check above, we can proceed
        //   as though the info we need to work with is available.

        if (rbtnDay.isSelected()) {  // Day interval
            recalcEndByDay(getIntFromTextField(txtfDayInterval));
        } else if (rbtnWeek.isSelected()) {  // Week interval
            recalcEndByWeek(getIntFromTextField(txtfWeekInterval));
        } else if (rbtnMonth.isSelected()) { // Month interval
            String strTmp = (String) comboxMonth.getSelectedItem();
            // System.out.println("Monthly pattern: " + strTmp);
            recalcEndByMonth(getIntFromTextField(txtfMonthInterval), strTmp);
        } else if (rbtnYear.isSelected()) { // Year interval
            String strTmp = (String) comboxYear.getSelectedItem();
            // System.out.println("Yearly pattern: " + strTmp);
            recalcEndByMonth(12, strTmp);
        } // end if Year

        // Update the two 'Stop' values.
        dtf = DateTimeFormatter.ofPattern("EEE  d MMM yyyy");
        btnStopBy.setText(dtf.format(dateStopBy));

        txtfStopAfter.setText(String.valueOf(intStopAfter));
    } // end recalcEnd


    //------------------------------------------------------------
    // Method Name: recalcEndByDay
    //
    // This is the simplest of the end recalculators.  We
    //   simply have to increase the dateStart by the specified
    //   interval, until we have reached either the required
    //   number of occurrences or the final date.
    //------------------------------------------------------------
    private void recalcEndByDay(int interval) {
        LocalDate tmpDate;

        // Calculate the dateStopBy (because the StopAfter count has been explicitly set)
        if (rbtnStopAfter.isSelected()) {
            tmpDate = dateStart;
            for (int i = 0; i < intStopAfter; i++) {
                tmpDate = tmpDate.plusDays(interval);
            } // end for
            dateStopBy = tmpDate;
        } // end if Stop After

        // Calculate the intStopAfter (because the StopBy date has been explicitly set)
        if (rbtnStopBy.isSelected()) {
            tmpDate = dateStart;
            intStopAfter = 1;
            while (tmpDate.isBefore(dateStopBy)) {
                tmpDate = tmpDate.plusDays(interval);
                if (tmpDate.isBefore(dateStopBy)) intStopAfter++;
            } // end while
        } // end if Stop After
    } // end recalcEndByDay

    //------------------------------------------------------------
    // Method Name: recalcEndByMonth
    //
    // The panel allows for the user to explicitly set either the
    //   date by which to stop, or the number of times that the
    //   event should occur over time.  Only one of these may be set,
    //   and the value that is set will be used to determine the value
    //   of the other.  This method calculates either the dateStopBy
    //   or the intStopAfter value.
    //------------------------------------------------------------
    private void recalcEndByMonth(int intInterval, String strMonthPattern) {
        int intAfterMonths;

        // Calculate the dateStopBy (because the StopAfter count has been explicitly set)
        if (rbtnStopAfter.isSelected()) {
            intAfterMonths = intInterval * (intStopAfter - 1);
            dateStopBy = getEndDateAfterMonths(intAfterMonths, strMonthPattern);
        } // end if

        // Calculate the intStopAfter (because the StopBy date has been explicitly set)
        if (rbtnStopBy.isSelected()) {
            intStopAfter = 0;  // Initialization

            // From an application-operational perspective, we should not be able to arrive
            // at this point without having set the 'dateStopBy'.  If an alternative path was
            // taken (for example by a test class), then you might see a null exception below.
            // If so - that's on you and your test code; I'm not going to handle it here.

            // The dateStopBy is not the same as the event end, so it is possible that the
            // user could have set it to a value before the event even started.  In that
            // case the recurrence is a bit moot; our result was 'calculated' as soon as it
            // was initialized, above.
            if (dateStopBy.isBefore(dateStart)) return;

            if (dateStopBy.isEqual(dateStart)) { // Also possible, edge case.
                intStopAfter = 1;
                return;
            }

            LocalDate tmpDate = dateStart;
            while (dateStopBy.isAfter(tmpDate)) {
                intStopAfter++;
                intAfterMonths = intInterval * (intStopAfter - 1);
                tmpDate = getEndDateAfterMonths(intAfterMonths, strMonthPattern);
            } // end while

            // Correction for overshoot.
            if(tmpDate.isAfter(dateStopBy)) {
                intStopAfter--;
            }
        } // end if recalculating due to StopBy

    } // end recalcEndByMonth


    //------------------------------------------------------------
    // Method Name: recalcEndByWeek
    //
    //------------------------------------------------------------
    private void recalcEndByWeek(int interval) {
        // Initialize the calendar to the start date.
        LocalDate tmpDate = dateStart;

        // Back up one day, so we will include
        //   the start as we scan forward.
        tmpDate = tmpDate.minusDays(1);

        // Calculate the dateStopBy
        if (rbtnStopAfter.isSelected()) {
            int intTmp = intStopAfter;

            while (intTmp > 0) {
                tmpDate = tmpDate.plusDays(1);
                switch (tmpDate.getDayOfWeek()) {
                    case SUNDAY:
                        if (chkboxSunday.isSelected()) intTmp--;
                        break;
                    case MONDAY:
                        if (chkboxMonday.isSelected()) intTmp--;
                        break;
                    case TUESDAY:
                        if (chkboxTuesday.isSelected()) intTmp--;
                        break;
                    case WEDNESDAY:
                        if (chkboxWednesday.isSelected()) intTmp--;
                        break;
                    case THURSDAY:
                        if (chkboxThursday.isSelected()) intTmp--;
                        break;
                    case FRIDAY:
                        if (chkboxFriday.isSelected()) intTmp--;
                        break;
                    case SATURDAY:
                        if (chkboxSaturday.isSelected()) intTmp--;

                        // Jump the (rest of the) interval
//                        if (intTmp > 0) calTmp.setNotes(Calendar.DATE, 7 * (interval - 1));
                        if (intTmp > 0) tmpDate = tmpDate.plusWeeks(interval - 1);
                        break;
                } // end switch

            } // end while still more to go

//            dateStopBy = calTmp.getTime();
            dateStopBy = tmpDate;
        } // end if Stop After

        //----------------------------------------------------------

        // Calculate the intStopAfter
        if (rbtnStopBy.isSelected()) {
            tmpDate = dateStart;
            intStopAfter = 0;

            while (tmpDate.isBefore(dateStopBy)) {
                // Adjust the count for one week
                tmpDate = tmpDate.plusDays(1);
                switch (tmpDate.getDayOfWeek()) {
                    case SUNDAY:
                        if (chkboxSunday.isSelected()) intStopAfter++;
                        break;
                    case MONDAY:
                        if (chkboxMonday.isSelected()) intStopAfter++;
                        break;
                    case TUESDAY:
                        if (chkboxTuesday.isSelected()) intStopAfter++;
                        break;
                    case WEDNESDAY:
                        if (chkboxWednesday.isSelected()) intStopAfter++;
                        break;
                    case THURSDAY:
                        if (chkboxThursday.isSelected()) intStopAfter++;
                        break;
                    case FRIDAY:
                        if (chkboxFriday.isSelected()) intStopAfter++;
                        break;
                    case SATURDAY:
                        if (chkboxSaturday.isSelected()) intStopAfter++;

                        // Jump the (rest of the) interval
//                        if (calTmp.getTime().before(dateStopBy)) calTmp.setNotes(Calendar.DATE, 7 * (interval - 1));
                        if (tmpDate.isBefore(dateStopBy)) tmpDate = tmpDate.plusWeeks(interval - 1);
                        break;
                } // end switch

            } // end while

            if (intStopAfter == 0) intStopAfter = 1;
        } // end if Stop After

    } // end recalcEndByWeek


    /**
     * This method is called from within the constructor.
     * It continues the work of the JFrameBuilder generated code,
     * adding additional customizations that JFB does not
     * provide.
     */
    private void reinitializeComponent() {
        lblStartDate.setFont(Font.decode("Dialog-bold-12"));

        // Replace lblStopBy with btnStopBy.
        Rectangle rectTmp; // For holding original size and location.
        rectTmp = lblStopBy.getBounds();
        pnlStopBy.remove(lblStopBy);
        btnStopBy = new LabelButton("<Select a Date>");
        btnStopBy.setName("btnStopBy");
        btnStopBy.setFont(Font.decode("Dialog-bold-11"));
        btnStopBy.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                rbtnStopBy.requestFocus();
                rbtnStopBy.setSelected(true);
            }

            public void mouseClicked(MouseEvent e) {
                setStopBy();
            }
        });

        addComponent(pnlStopBy, btnStopBy, rectTmp.x, rectTmp.y, rectTmp.width, rectTmp.height);


        // Add Listeners for the Recurrence radio buttons.
        rbtnNone.addItemListener(this);
        rbtnDay.addItemListener(this);
        rbtnWeek.addItemListener(this);
        rbtnMonth.addItemListener(this);
        rbtnYear.addItemListener(this);

        // Group the radio buttons, for mutual exclusivity.
        ButtonGroup bgRecur = new ButtonGroup();
        bgRecur.add(rbtnNone);
        bgRecur.add(rbtnDay);
        bgRecur.add(rbtnWeek);
        bgRecur.add(rbtnMonth);
        bgRecur.add(rbtnYear);

        // Add Listeners for the 'when to End' radio buttons.
        rbtnForever.addItemListener(this);
        rbtnStopAfter.addItemListener(this);
        rbtnStopBy.addItemListener(this);

        // Group the radio buttons, for mutual exclusivity.
        ButtonGroup bgUntil = new ButtonGroup();
        bgUntil.add(rbtnForever);
        bgUntil.add(rbtnStopAfter);
        bgUntil.add(rbtnStopBy);

        // Assign Names to critical components
        rbtnNone.setName("rbtnNone");
        rbtnDay.setName("rbtnDay");
        rbtnWeek.setName("rbtnWeek");
        rbtnMonth.setName("rbtnMonth");
        rbtnYear.setName("rbtnYear");
        rbtnForever.setName("rbtnForever");
        rbtnStopAfter.setName("rbtnStopAfter");
        rbtnStopBy.setName("rbtnStopBy");
        txtfDayInterval.setName("txtfDayInterval");
        txtfWeekInterval.setName("txtfWeekInterval");
        txtfMonthInterval.setName("txtfMonthInterval");
        txtfStopAfter.setName("txtfStopAfter");
        comboxMonth.setName("comboxMonth");
        comboxYear.setName("comboxYear");


        // ActionListeners and ActionCommands
        txtfDayInterval.addActionListener(this);
        txtfWeekInterval.addActionListener(this);
        txtfMonthInterval.addActionListener(this);
        txtfStopAfter.addActionListener(this);
        comboxMonth.addActionListener(this);
        comboxYear.addActionListener(this);
        //------------------------------------------
        txtfDayInterval.setActionCommand("txtfDayInterval");
        txtfWeekInterval.setActionCommand("txtfWeekInterval");
        txtfMonthInterval.setActionCommand("txtfMonthInterval");
        txtfStopAfter.setActionCommand("txtfStopAfter");
        comboxMonth.setActionCommand("comboxMonth");
        comboxYear.setActionCommand("comboxYear");

        // FocusListeners for the text fields and combo boxes.
        txtfDayInterval.addFocusListener(this);
        txtfWeekInterval.addFocusListener(this);
        txtfMonthInterval.addFocusListener(this);
        txtfStopAfter.addFocusListener(this);
        comboxMonth.addFocusListener(this);
        comboxYear.addFocusListener(this);

        // Force the text fields to digits only -
        KeyAdapter kaDigitsOnly = new KeyAdapter() {
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();

                if (!((c == KeyEvent.VK_BACK_SPACE) || (c == KeyEvent.VK_DELETE)
                        || (c == KeyEvent.VK_ENTER) || (c == KeyEvent.VK_TAB)
                        || (Character.isDigit(c)))) {
                    e.consume();
                }
            }
        };
        txtfDayInterval.addKeyListener(kaDigitsOnly);
        txtfWeekInterval.addKeyListener(kaDigitsOnly);
        txtfMonthInterval.addKeyListener(kaDigitsOnly);
        txtfStopAfter.addKeyListener(kaDigitsOnly);

        // Establish set lengths for each text field.
        AbstractDocument doc;
        doc = (AbstractDocument) txtfDayInterval.getDocument();
        doc.setDocumentFilter(new FixedSizeDocumentFilter(3));
        doc = (AbstractDocument) txtfWeekInterval.getDocument();
        doc.setDocumentFilter(new FixedSizeDocumentFilter(2));
        doc = (AbstractDocument) txtfMonthInterval.getDocument();
        doc.setDocumentFilter(new FixedSizeDocumentFilter(2));
        doc = (AbstractDocument) txtfStopAfter.getDocument();
        doc.setDocumentFilter(new FixedSizeDocumentFilter(3));

        // Add Listeners for the individual Day checkboxes
        chkboxSunday.addItemListener(this);
        chkboxMonday.addItemListener(this);
        chkboxTuesday.addItemListener(this);
        chkboxWednesday.addItemListener(this);
        chkboxThursday.addItemListener(this);
        chkboxFriday.addItemListener(this);
        chkboxSaturday.addItemListener(this);

    } // end reinitializeComponent

    // Used by tests; no dialog needed to choose a date
    void setStopBy(LocalDate theStopDate) {
        rbtnForever.setSelected(false);
        rbtnStopAfter.setSelected(false);
        rbtnStopBy.setSelected(true);
        dateStopBy = theStopDate;
        recalcEnd();
    }

    // Set the 'Stop By' date
    private void setStopBy() {
        // Make a dialog window to choose a date from a Year view.
        YearView yvDateChooser = new YearView();
        if (dateStopBy == null) dateStopBy = dateStart;
        yvDateChooser.setChoice(dateStopBy);

        int doit = optionPane.showConfirmDialog(
                JOptionPane.getFrameForComponent(this),
                yvDateChooser,
                "Select a Date to stop by",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (doit == -1) return; // The X on the dialog
        if (doit == JOptionPane.CANCEL_OPTION) return;

        dateStopBy = yvDateChooser.getChoice();
        recalcEnd();
    } // end setStopBy

    // Set the interface fields per the input data.  The input string should have already
    // been validated by the UI and validation only needs to occur again after user
    // changes have been entered.  Test methods are responsible for generating
    // correctly formatted input for usage by this method.
    void showTheData(String strRecur, LocalDate firstEventDate) {
        MemoryBank.debug("Initial recurrence string: " + strRecur);
        dateStart = firstEventDate;

        // Initialize the interface; clear previous settings.
        //-------------------------------------------------------
        blnHandleItems = false;

        // The default is 'Do not repeat' - No recurrence.
        rbtnNone.setSelected(true);
        txtfDayInterval.setText("1");
        txtfWeekInterval.setText("1");
        chkboxSunday.setSelected(false);
        chkboxMonday.setSelected(false);
        chkboxTuesday.setSelected(false);
        chkboxWednesday.setSelected(false);
        chkboxThursday.setSelected(false);
        chkboxFriday.setSelected(false);
        chkboxSaturday.setSelected(false);
        chkboxSunday.setEnabled(true);
        chkboxMonday.setEnabled(true);
        chkboxTuesday.setEnabled(true);
        chkboxWednesday.setEnabled(true);
        chkboxThursday.setEnabled(true);
        chkboxFriday.setEnabled(true);
        chkboxSaturday.setEnabled(true);
        txtfMonthInterval.setText("1");
        rbtnForever.setSelected(true);
        txtfStopAfter.setText("");
        pnlEnd.setVisible(false);
        btnStopBy.setText(" <Select a Date>");

        // Preselect the days of the week
        shuffleDays();  // this did a calTemp set to dateStart.

        // Display the date we're working forward from.
        dtf = DateTimeFormatter.ofPattern("EEE  d MMM yyyy");
        lblStartDate.setText(dtf.format(dateStart));

        //--------------------------------------------------------
        // Reload the combo boxes with dateStart-specific values
        //--------------------------------------------------------
        comboxMonth.removeAllItems();
        comboxYear.removeAllItems();

        // Get the month name -
        dtf = DateTimeFormatter.ofPattern("MMMM");
        String strMonth = dtf.format(dateStart);

        // Get the numeric (as 'nice' text) within the month -
        //--------------------------------------------------------
        int intDate = dateStart.getDayOfMonth();
        String strDate = String.valueOf(intDate);
        if ((intDate == 1) || (intDate == 21) || (intDate == 31)) {
            strDate += "st";
        } else if ((intDate == 2) || (intDate == 22)) {
            strDate += "nd";
        } else if ((intDate == 3) || (intDate == 23)) {
            strDate += "rd";
        } else {
            strDate += "th";
        } // end if
        comboxMonth.addItem("the " + strDate);
        comboxYear.addItem("the " + strDate + " of " + strMonth);

        // Next choice(s): The 'n'th (some)day of the month.
        //------------------------------------------------------
        // Example:  The 3rd Friday, or the 2nd weekend day.
        // First, figure out which week we're in; looking for:
        //     First, Second, Third, Fourth, or Last.
        // Sometimes the 4th IS the last and sometimes there
        // is a 5th occurrence and so the 4th is just the 4th.
        // The 5th is always the last, so we don't offer the 5th
        // as a numeric choice.  If the month we're working in
        // only has 4 Fridays and our dateStart is the 4th one,
        // we must offer both a 'Fourth' choice as well as a 'Last'
        // choice.  If there were five and our dateStart is the 5th
        // Friday, we only need to offer the 'Last' choice.  If
        // there were five and ours were the 4th, we will only
        // offer the 'Fourth' choice.  The choices offerred
        // are not dependent on previous choices that were made;
        // only on the possible variations of the description for
        // dateStart.
        int intMonth = dateStart.getMonthValue() - 1;
        int intWeeksBefore = 0;
        // First, how many weeks are BEFORE this one?
        LocalDate tmpDate = dateStart;
        do {
            tmpDate = tmpDate.minusWeeks(1);
            if (tmpDate.getMonthValue() - 1 == intMonth) intWeeksBefore++;
        } while (tmpDate.getMonthValue() - 1 == intMonth);

        // Get the day name -
        dtf = DateTimeFormatter.ofPattern("EEEE");
        String strDay = dtf.format(dateStart);

        boolean blnLastWeek = false;

        tmpDate = dateStart;
        switch (intWeeksBefore) {
            case 0: // This is the 1st week
                strDate = "the first " + strDay;
                comboxMonth.addItem(strDate);
                comboxYear.addItem(strDate + " in " + strMonth);

                // Now, is this the 1st-4th weekday or 1st or 2nd weekend day?
                if (strDay.equals("Sunday") || (strDay.equals("Saturday"))) {
                    // As a weekend day (in the first week), we can only be the
                    //   first or the second.  If we scan back to the beginning
                    //   and find either a Sat or Sun along the way, then we're
                    //   the second; otherwise first.
                    boolean blnFirst = true;
                    do {
                        tmpDate = tmpDate.minusDays(1);
                        if (tmpDate.getMonthValue() - 1 == intMonth) {
                            if (tmpDate.getDayOfWeek() == DayOfWeek.SATURDAY) blnFirst = false;
                            if (tmpDate.getDayOfWeek() == DayOfWeek.SUNDAY) blnFirst = false;
                        } // end if
                    } while (tmpDate.getMonthValue() - 1 == intMonth);

                    if (blnFirst) {
                        strDate = "the first weekend day";
                        comboxMonth.addItem(strDate);
                        comboxYear.addItem(strDate + " in " + strMonth);
                    } else {
                        strDate = "the second weekend day";
                        comboxMonth.addItem(strDate);
                        comboxYear.addItem(strDate + " in " + strMonth);
                    } // end if
                } else {
                    // Is there a 'weekend-day' offset, and if so, how much?
                    int intOffset = 0;
                    do {
                        tmpDate = tmpDate.minusDays(1);
                        if (tmpDate.getMonthValue() - 1 == intMonth) {
                            if (tmpDate.getDayOfWeek() == DayOfWeek.SATURDAY) intOffset++;
                            if (tmpDate.getDayOfWeek() == DayOfWeek.SUNDAY) intOffset++;
                        } // end if
                    } while (tmpDate.getMonthValue() - 1 == intMonth);
                    intDate -= intOffset;
                    switch (intDate) {
                        case 1:
                            strDate = "the first weekday";
                            comboxMonth.addItem(strDate);
                            comboxYear.addItem(strDate + " in " + strMonth);
                            break;
                        case 2:
                            strDate = "the second weekday";
                            comboxMonth.addItem(strDate);
                            comboxYear.addItem(strDate + " in " + strMonth);
                            break;
                        case 3:
                            strDate = "the third weekday";
                            comboxMonth.addItem(strDate);
                            comboxYear.addItem(strDate + " in " + strMonth);
                            break;
                        case 4:
                            strDate = "the fourth weekday";
                            comboxMonth.addItem(strDate);
                            comboxYear.addItem(strDate + " in " + strMonth);
                            break;
                        default: // do nothing for weekdays 5,6,7.
                    } // end switch

                }
                break;
            case 1: // This is the 2nd week
                strDate = "the second " + strDay;
                comboxMonth.addItem(strDate);
                comboxYear.addItem(strDate + " in " + strMonth);

                // Now, is this the 3rd or 4th weekend day?
                if (strDay.equals("Sunday") || (strDay.equals("Saturday"))) {
                    int intOffset = 0;
                    do {
                        tmpDate = tmpDate.minusDays(1);
                        if (tmpDate.getMonthValue() - 1 == intMonth) {
                            if (tmpDate.getDayOfWeek() == DayOfWeek.SATURDAY) intOffset++;
                            if (tmpDate.getDayOfWeek() == DayOfWeek.SUNDAY) intOffset++;
                        } // end if
                    } while (tmpDate.getMonthValue() - 1 == intMonth);

                    if (intOffset == 2) { // Two before this one means it's 3rd.
                        strDate = "the third weekend day";
                        comboxMonth.addItem(strDate);
                        comboxYear.addItem(strDate + " in " + strMonth);
                    } else {
                        strDate = "the fourth weekend day";
                        comboxMonth.addItem(strDate);
                        comboxYear.addItem(strDate + " in " + strMonth);
                    }
                } // end if a weekend day
                break;
            case 2: // This is the 3rd week
                strDate = "the third " + strDay;
                comboxMonth.addItem(strDate);
                comboxYear.addItem(strDate + " in " + strMonth);
                break;
            case 3: // This is the 4th (and also last? week)
                strDate = "the fourth " + strDay;
                comboxMonth.addItem(strDate);
                comboxYear.addItem(strDate + " in " + strMonth);

                // Check to see if there is another week this month.
                tmpDate = tmpDate.plusWeeks(1);
                if (tmpDate.getMonthValue() - 1 != intMonth) blnLastWeek = true;

                break;
            case 4: // This is the last (5th) week.
                blnLastWeek = true;
                // We handle this case separately so that the variation on
                //   case 3 above can also participate.  Originally written as
                //   a case fallthru but the -Xlint compiler whined too much.
        } // end switch

        if (blnLastWeek) {
            strDate = "the last " + strDay;
            comboxMonth.addItem(strDate);
            comboxYear.addItem(strDate + " in " + strMonth);

            // Is this a weekday or weekend day?
            boolean blnWeekday = true;
            if (tmpDate.getDayOfWeek() == DayOfWeek.SATURDAY) blnWeekday = false;
            if (tmpDate.getDayOfWeek() == DayOfWeek.SUNDAY) blnWeekday = false;

            // Is this the last day (of its kind) this month?
            boolean blnLast = true;
            if (blnWeekday) {
                tmpDate = dateStart;
                do {
                    tmpDate = tmpDate.plusDays(1);
                    if (tmpDate.getMonthValue() - 1 == intMonth) {
                        if (isWeekday(tmpDate)) blnLast = false;
                    } // end if
                } while (tmpDate.getMonthValue() - 1 == intMonth);

                if (blnLast) {
                    strDate = "the last weekday";
                    comboxMonth.addItem(strDate);
                    comboxYear.addItem(strDate + " in " + strMonth);
                }

            } else {
                do {
                    tmpDate = tmpDate.plusDays(1);
                    if (tmpDate.getMonthValue() - 1 == intMonth) {
                        if (!isWeekday(tmpDate)) blnLast = false;
                    } // end if
                } while (tmpDate.getMonthValue() - 1 == intMonth);

                if (blnLast) {
                    strDate = "the last weekend day";
                    comboxMonth.addItem(strDate);
                    comboxYear.addItem(strDate + " in " + strMonth);
                }
            } // end if

            // Is this the last day of the month, period?
            tmpDate = dateStart.plusDays(1);
            if (tmpDate.getMonthValue() - 1 != intMonth) {
                strDate = "the last day";
                comboxMonth.addItem(strDate);
                comboxYear.addItem(strDate + " of " + strMonth);
            }
        } // end if last week (containing this day) in the month

        // If we want to recognize variable holidays such
        //   as Thanksgiving and Easter, here is where it
        //   should be done.
        // For now - I'm moving on.  dlc 11/28/2006

        comboxMonth.setSelectedItem(null);
        comboxYear.setSelectedItem(null);

        blnHandleItems = true;

        // Now, make the selections match the initial data.
        //------------------------------------------------------
        int intUnderscore1;
        int intUnderscore2 = 0;
        String strDescription;
        if (strRecur.trim().equals("")) {
            return;
            // No further action required.
        } else if (strRecur.startsWith("D")) {
            rbtnDay.setSelected(true);
            intUnderscore1 = strRecur.indexOf('_');
            txtfDayInterval.setText(strRecur.substring(1, intUnderscore1));
            intUnderscore2 = strRecur.lastIndexOf('_');
        } else if (strRecur.startsWith("W")) {
            rbtnWeek.setSelected(true);
            intUnderscore1 = strRecur.indexOf('_');
            txtfWeekInterval.setText(strRecur.substring(1, intUnderscore1));
            if (strRecur.contains("Su")) chkboxSunday.setSelected(true);
            if (strRecur.contains("Mo")) chkboxMonday.setSelected(true);
            if (strRecur.contains("Tu")) chkboxTuesday.setSelected(true);
            if (strRecur.contains("We")) chkboxWednesday.setSelected(true);
            if (strRecur.contains("Th")) chkboxThursday.setSelected(true);
            if (strRecur.contains("Fr")) chkboxFriday.setSelected(true);
            if (strRecur.contains("Sa")) chkboxSaturday.setSelected(true);
            intUnderscore2 = strRecur.lastIndexOf('_');
        } else if (strRecur.startsWith("M")) {
            rbtnMonth.setSelected(true);
            intUnderscore1 = strRecur.indexOf('_');
            txtfMonthInterval.setText(strRecur.substring(1, intUnderscore1));
            intUnderscore2 = strRecur.lastIndexOf('_');
            strDescription = strRecur.substring(intUnderscore1 + 1, intUnderscore2);
            comboxMonth.setSelectedItem(strDescription);
        } else if (strRecur.startsWith("Y")) {
            rbtnYear.setSelected(true);
            intUnderscore1 = strRecur.indexOf('_');
            intUnderscore2 = strRecur.lastIndexOf('_');
            strDescription = strRecur.substring(intUnderscore1 + 1, intUnderscore2);
            comboxYear.setSelectedItem(strDescription);
        } // end if/elses

        if (!strRecur.endsWith("_")) {
            String strRecurEnd;
            strRecurEnd = strRecur.substring(intUnderscore2 + 1);
            if (strRecurEnd.length() < 4) { // Stop After
                txtfStopAfter.setText(strRecurEnd);
                rbtnStopAfter.setSelected(true);
            } else {                       // Stop By
//                sdf.applyPattern("yyyyMMdd");
                dtf = DateTimeFormatter.ofPattern("yyyyMMdd");
                try {
                    dateStopBy = LocalDate.parse(strRecurEnd, dtf);
                    rbtnStopBy.setSelected(true);
                } catch (Exception pe) {
                    System.out.println("Exception: " + pe.getMessage());
                }
            }
        } // end if there is an end recurrence range

    } // end showTheData


    private void shuffleDays() {
        // Interpret the dateStart Day
        switch (dateStart.getDayOfWeek()) {
            case SUNDAY:
                chkboxSunday.setSelected(true);
                chkboxSunday.setEnabled(false);

                chkboxSunday.setBackground(EventEditorPanel.backColor);
                chkboxMonday.setBackground(EventEditorPanel.futureColor);
                chkboxTuesday.setBackground(EventEditorPanel.futureColor);
                chkboxWednesday.setBackground(EventEditorPanel.futureColor);
                chkboxThursday.setBackground(EventEditorPanel.futureColor);
                chkboxFriday.setBackground(EventEditorPanel.futureColor);
                chkboxSaturday.setBackground(EventEditorPanel.futureColor);
                break;
            case MONDAY:
                chkboxMonday.setSelected(true);
                chkboxMonday.setEnabled(false);

                chkboxSunday.setBackground(EventEditorPanel.pastColor);
                chkboxMonday.setBackground(EventEditorPanel.backColor);
                chkboxTuesday.setBackground(EventEditorPanel.futureColor);
                chkboxWednesday.setBackground(EventEditorPanel.futureColor);
                chkboxThursday.setBackground(EventEditorPanel.futureColor);
                chkboxFriday.setBackground(EventEditorPanel.futureColor);
                chkboxSaturday.setBackground(EventEditorPanel.futureColor);
                break;
            case TUESDAY:
                chkboxTuesday.setSelected(true);
                chkboxTuesday.setEnabled(false);

                chkboxSunday.setBackground(EventEditorPanel.pastColor);
                chkboxMonday.setBackground(EventEditorPanel.pastColor);
                chkboxTuesday.setBackground(EventEditorPanel.backColor);
                chkboxWednesday.setBackground(EventEditorPanel.futureColor);
                chkboxThursday.setBackground(EventEditorPanel.futureColor);
                chkboxFriday.setBackground(EventEditorPanel.futureColor);
                chkboxSaturday.setBackground(EventEditorPanel.futureColor);
                break;
            case WEDNESDAY:
                chkboxWednesday.setSelected(true);
                chkboxWednesday.setEnabled(false);

                chkboxSunday.setBackground(EventEditorPanel.pastColor);
                chkboxMonday.setBackground(EventEditorPanel.pastColor);
                chkboxTuesday.setBackground(EventEditorPanel.pastColor);
                chkboxWednesday.setBackground(EventEditorPanel.backColor);
                chkboxThursday.setBackground(EventEditorPanel.futureColor);
                chkboxFriday.setBackground(EventEditorPanel.futureColor);
                chkboxSaturday.setBackground(EventEditorPanel.futureColor);
                break;
            case THURSDAY:
                chkboxThursday.setSelected(true);
                chkboxThursday.setEnabled(false);

                chkboxSunday.setBackground(EventEditorPanel.pastColor);
                chkboxMonday.setBackground(EventEditorPanel.pastColor);
                chkboxTuesday.setBackground(EventEditorPanel.pastColor);
                chkboxWednesday.setBackground(EventEditorPanel.pastColor);
                chkboxThursday.setBackground(EventEditorPanel.backColor);
                chkboxFriday.setBackground(EventEditorPanel.futureColor);
                chkboxSaturday.setBackground(EventEditorPanel.futureColor);
                break;
            case FRIDAY:
                chkboxFriday.setSelected(true);
                chkboxFriday.setEnabled(false);

                chkboxSunday.setBackground(EventEditorPanel.pastColor);
                chkboxMonday.setBackground(EventEditorPanel.pastColor);
                chkboxTuesday.setBackground(EventEditorPanel.pastColor);
                chkboxWednesday.setBackground(EventEditorPanel.pastColor);
                chkboxThursday.setBackground(EventEditorPanel.pastColor);
                chkboxFriday.setBackground(EventEditorPanel.backColor);
                chkboxSaturday.setBackground(EventEditorPanel.futureColor);
                break;
            case SATURDAY:
                chkboxSaturday.setSelected(true);
                chkboxSaturday.setEnabled(false);

                chkboxSunday.setBackground(EventEditorPanel.pastColor);
                chkboxMonday.setBackground(EventEditorPanel.pastColor);
                chkboxTuesday.setBackground(EventEditorPanel.pastColor);
                chkboxWednesday.setBackground(EventEditorPanel.pastColor);
                chkboxThursday.setBackground(EventEditorPanel.pastColor);
                chkboxFriday.setBackground(EventEditorPanel.pastColor);
                chkboxSaturday.setBackground(EventEditorPanel.backColor);
        } // end switch
    } // end shuffleDays

} // end class RecurrencePanel
