/* ***************************************************************************
 * File:    YearView.java
 * Author:  D. Lee Chastain
 *
 ****************************************************************************/
/**
 User interface to display a full calendar year in Month/Date
 format, with capability of selection, and giving the user control
 over which year is displayed.

 Note 12/29/2006: added the capability to UNselect.
 */

//                                                                       
// Point of historical accuracy - for the US, the Gregorian change
//   has been set to September, 1752.  See also - the notes associated
//   with GregorianCalendar.java.  If a different Calendar is 
//   desired, can rewrite to allow it to be 'set'.
//                                                                       

import java.awt.*;
import java.awt.event.*;
import java.text.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.LineBorder;

public class YearView extends JPanel implements ActionListener {
    private static final long serialVersionUID = -4542122215704753611L;

    // Required for use by 'static' section -
    private static String[] monthNames;
    private static String[] weekNames;

    // Variables needed by more than one method -
    private Date choice;            // constructor, event handling, numerous
    private Date choice2;           // when 2 choices are allowed
    private Date initial;           // constructor, event handling
    private DayLabel dl;            // recalc, event handling
    private DayLabel today;         // actionPerformed, recalc
    private JLabel titleLabel;       // constructor, event handling
    private JLabel choiceLabel;      // constructor, highlight
    private JPanel yearPanel;
    private int Year;               // numerous
    private YearBox yearBox;        // BottomPanel constructor
    private static SimpleDateFormat sdf;
    private AppTree parent;
    private static Color hasDataColor = Color.blue;
    private static Color noDataColor = Color.black;
    private static Font hasDataFont = Font.decode("Dialog-bold-16");
    private static Font noDataFont = Font.decode("Dialog-plain-14");
    private boolean hasDataArray[][];
    private JDialog jdTheDialog;
    private int intNumSelections;
    private int intSelectionCount;
    private JButton todayButton;


    GregorianCalendar cal;

    private static final int borderWidth = 2;
    private static LineBorder theBorder;

    static {
        theBorder = new LineBorder(Color.black, borderWidth);

        // Initialize month names.
        monthNames = new String[]{"January", "February", "March",
                "April", "May", "June", "July", "August", "September",
                "October", "November", "December"};

        // Initialize day of week names.
        weekNames = new String[]{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

        sdf = new SimpleDateFormat();
        sdf.applyPattern("EEEE, MMMM d, yyyy");
    } // end static

    YearView(AppTree l) {
        super(new BorderLayout());
        parent = l;
        initial = new Date();
        jdTheDialog = null;
        choice2 = null;
        intSelectionCount = 0;

        setBorder(theBorder);

        cal = (GregorianCalendar) Calendar.getInstance();
        // Note: getInstance at this time returns a Calendar that
        //   is actually a GregorianCalendar, but since the return
        //   type is Calendar, it must be cast in order to assign.

        cal.setGregorianChange(new GregorianCalendar(1752,
                Calendar.SEPTEMBER, 14).getTime());

        reset();
        Year = cal.get(Calendar.YEAR);

        MouseAdapter ma = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                LabelButton source = (LabelButton) e.getSource();
                String s = source.getText();

                dl.reset();     // turn off current choice highlight

                if (s.equals("-"))
                    if (Year > 1) Year--;
                if (s.equals("+"))
                    if (Year < 9999) Year++;

//	int thisYear = Calendar.getInstance().get(Calendar.YEAR);
                yearBox.select(String.valueOf(Year));
//	if(Year == thisYear) reset();
//	else choice = null;  // will be set again in recalc...
                titleLabel.setText("Year " + Year);

                recalc(Year);
            } // end mouseClicked
        };// end of new MouseAdapter

        LabelButton prev = new LabelButton("-");
        prev.addMouseListener(ma);
        prev.setPreferredSize(new Dimension(28, 28));
        prev.setFont(Font.decode("Dialog-bold-14"));

        LabelButton next = new LabelButton("+");
        next.addMouseListener(ma);
        next.setPreferredSize(new Dimension(28, 28));
        next.setFont(Font.decode("Dialog-bold-14"));

        JPanel p0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p0.add(prev);
        p0.add(next);
        p0.add(new Spacer(25, 1));

        titleLabel = new JLabel("Year " + Year);
        titleLabel.setFont(Font.decode("Serif-bold-20"));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setBackground(Color.lightGray);

        yearBox = new YearBox(String.valueOf(cal.get(Calendar.YEAR)));

        JPanel head1 = new JPanel(new BorderLayout());
        head1.add(p0, "West");
        head1.add(titleLabel, "Center");
        head1.add(yearBox, "East");
        add(head1, BorderLayout.NORTH);

        choiceLabel = new JLabel();
        choiceLabel.setFont(Font.decode("Dialog-bold-18"));
        choiceLabel.setForeground(Color.red);
        choiceLabel.setHorizontalAlignment(JLabel.RIGHT);

        yearPanel = new JPanel(new GridLayout(3, 4));
        for (int i = 0; i < 12; i++) {
            cal.set(Year, i, 1);    // month is zero-based.
            yearPanel.add(new MonthCanvas(cal));
        } // end for i

        add(yearPanel, BorderLayout.CENTER);

        JPanel bottomPanel = new JPanel(new BorderLayout());
        // Using a BorderLayout so that the choiceLabel can
        //   take the 'center' and thereby expand/contract as needed.
        todayButton = new JButton("Today");
        todayButton.setFont(Font.decode("DialogInput-bold-12"));
        todayButton.addActionListener(YearView.this);

        // This button now only needed when working as a dialog.
        todayButton.setVisible(false);

        bottomPanel.add(todayButton, "West");
        bottomPanel.add(choiceLabel, "Center");
        add(bottomPanel, BorderLayout.SOUTH);

        recalc(Year);
    } // end constructor


    // Handler for the 'Today' button.
    public void actionPerformed(ActionEvent e) {
        boolean doRecalc = true;

        dl.reset();     // turn off current choice highlight

        // Compare the currently displayed Year with 'now'.
        int thisYear = Year;
        reset(); // Set cal to 'today'
        Year = cal.get(Calendar.YEAR);
        if (Year == thisYear) doRecalc = false;

        titleLabel.setText("Year " + Year);
        yearBox.select(String.valueOf(Year));

        if (!doRecalc) {
            int day = cal.get(Calendar.DAY_OF_MONTH);
            int month = cal.get(Calendar.MONTH);

            // today will be wrong at this point in the code if
            // this interface has been up past midnight.
            // The fix:  recalc is the only good place to assign the
            // 'today' DayLabel object; it needs to be called
            // even though we may already be on the right Year.
            if (day != today.day) doRecalc = true;
            if (month != today.month) doRecalc = true;
        } // end if

        if (doRecalc) recalc(Year);
        dl = today;
        dl.highlight(); // highlight today
    } // end actionPerformed


    // debug method...
    protected static void calPrint(Calendar cal) {
        System.out.print("Date:  " + cal.get(Calendar.DAY_OF_MONTH));
        System.out.print("\tMonth: " + cal.get(Calendar.MONTH));
        System.out.print("\tYear: " + cal.get(Calendar.YEAR));
        System.out.print("\n");
    } // end calPrint

    public Date getChoice() {
        return choice;
    }

    // When the YearView is used as a Date input dialog,
    //   sometimes two dates are needed.  This method
    //   is provided so the calling context can retrieve
    //   the second choice.
    public Date getChoice2() {
        return choice2;
    }


    public void recalc(int year) {
        // Look for new day data, for color/font setting.
        hasDataArray = AppUtil.findDataDays(year);

        for (int i = 0; i < 12; i++) {
            MonthCanvas mc = (MonthCanvas) yearPanel.getComponent(i);
            cal.set(year, i, 1);
            mc.recalc(cal);
        } // end for i

        if (choice == null) {
            choiceLabel.setText(" ");
        } else {
            choiceLabel.setText(sdf.format(choice) + " ");
        }
    } // end recalc

    // Reset cal and choice to current day/time, sdf to cal
    public void reset() {
        initial = new Date(); // in case this interface has been up past midnight.
        cal.setTime(initial);

        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);

        sdf.setCalendar(cal);
        choice = cal.getTime();
    } // end reset

    // Called by an external controlling context.
    public void setChoice(Date d) {
        DayLabel tmp = today; // preserve the value of 'today'.
        if (dl != null) dl.reset(); // turn off any previous selection.
        if (d == null) {
            cal.setTime(new Date());
            intSelectionCount = 0;
        } else {
            cal.setTime(d);
            intSelectionCount = 1;
        }
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        choice = cal.getTime();
        Year = cal.get(Calendar.YEAR);
        titleLabel.setText("Year " + Year);
        yearBox.select(String.valueOf(Year));
        recalc(Year);
        today = tmp;

        if (d == null) {
            dl.reset();
            choice = null;
        } // end if setting no selection
    } // end setChoice


    public void setDialog(JDialog jd, int numSelections) {
        jdTheDialog = jd;
        intNumSelections = numSelections;
        todayButton.setVisible(true);
    } // end setDialog


    public class MonthCanvas extends JPanel {
        private static final long serialVersionUID = -8559208397000245694L;

        JPanel p1;
        JPanel p2;
        int whichOf12;
        Date myDate;

        MonthCanvas(GregorianCalendar cal) { // constructor
            setLayout(new BorderLayout());
            setBackground(Color.lightGray);
            whichOf12 = cal.get(Calendar.MONTH);
            JLabel l;

            // Add the month label.
            p1 = new JPanel();
            p1.setLayout(new GridLayout(2, 1));
            l = new JLabel(monthNames[whichOf12], JLabel.CENTER);
            p1.setBackground(Color.black);
            l.setBackground(Color.blue);
            l.setForeground(Color.white);
            l.setOpaque(true);
            l.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    setChoice(myDate);
                    // System.out.println("Choice is: " + choice);
                    if (parent == null) return;
                    if (e.getClickCount() == 2) parent.showMonth();
                } // end mousePressed
            });//end addMouseListener
            p1.add(l);

            // Add the day of the week labels.
            JPanel q = new JPanel();
            q.setLayout(new GridLayout(1, weekNames.length));
            q.setBackground(Color.gray);
            for (String weekName : weekNames) {
                l = new JLabel(weekName, JLabel.CENTER);
                l.setForeground(Color.white);
                l.setBackground(Color.gray);
                l.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        setChoice(myDate);
                        // System.out.println("Choice is: " + choice);
                        if (parent == null) return;
                        if (e.getClickCount() == 2) parent.showWeek();
                    } // end mousePressed
                });//end addMouseListener
                q.add(l);
            } // end for i
            p1.add(q);
            add(p1, BorderLayout.NORTH);

            // Create a grid for the days (as many rows as needed, 7 columns).
            p2 = new JPanel();
            p2.setLayout(new GridLayout(0, weekNames.length));

            // Add the days.  Enough room for 31 days across 6 rows.
            for (int i = 1; i <= 37; i++) {
                p2.add(new DayLabel());
            } // end for i

            add(p2, BorderLayout.CENTER);

            setBorder(LineBorder.createBlackLineBorder());
        } // end constructor

        public void recalc(GregorianCalendar cal) {
            DayLabel tmp;
            myDate = cal.getTime();

            // Get the day of the week of the first day.
            cal.set(Calendar.DAY_OF_MONTH, 1);
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
            int month = cal.get(Calendar.MONTH);

            // Clone this calendar
            GregorianCalendar tmpCal = (GregorianCalendar) cal.clone();

            boolean rollover = false;

            // Cycle thru all the days.
            for (int i = 1; i <= 37; i++) {

                //---------------------------------------------------
                // 'blank' days in the first row, before the 1st of the month.
                if (i < dayOfWeek) {
                    ((DayLabel) p2.getComponent(i - 1)).setText("");
                    continue;
                } // end if

                // 'blank' days in the last row (or two).
                if (rollover) { // blank out trailing days
                    ((DayLabel) p2.getComponent(i - 1)).setText("");
                    continue;
                } // end if

                // leave the declaration inside the loop;
                //    we want a new instance each time.
                GregorianCalendar c = (GregorianCalendar) tmpCal.clone();
                tmpCal.add(Calendar.DATE, 1);
                if (month != tmpCal.get(Calendar.MONTH)) rollover = true;

                // Set the date - source cal is 'c'
                tmp = (DayLabel) p2.getComponent(i - 1);
                tmp.setCal(c);
                //---------------------------------------------------

                // Highlight the current choice
                if (choice != null) {  // Take the first available day (Jan 1)
//          choice = new Date();
//          c.set(Calendar.MINUTE, 0);
//          c.set(Calendar.SECOND, 0);
//          choice = c.getTime();
//          dl = tmp;
//          dl.highlight();
//        } else {
                    if (choice.equals(c.getTime())) {
                        // If the choice is this day -
                        dl = tmp;
                        dl.highlight();

                        today = dl;
                        // today is only used in the handler for the 'Today'
                        // button.  Although we set it here to the value of
                        // the current choice which may not really be 'today',
                        // that handler will detect that and call this method
                        // again with the correct choice.
                    } // end if
                } // end if - if choice not set
            } // end for i
        } // end recalc

        public Dimension getPreferredSize() {
            return new Dimension(160, 130);
        } // end getPreferredSize
    } // end MonthCanvas

    class DayLabel extends JLabel implements MouseListener {
        private static final long serialVersionUID = -315010085213996418L;

        int day;
        int month;
        GregorianCalendar cal;
        Color offColor;
        Font offFont;

        DayLabel() {
            super("", CENTER);
            setBackground(Color.lightGray);
            setFont(Font.decode("Dialog-plain-14"));
            setText("");
            addMouseListener(this);
        } // end constructor

        public void highlight() {
            setForeground(Color.red);
            setFont(Font.decode("Dialog-bold-18"));
            choiceLabel.setText(sdf.format(choice) + " ");
        } // end highlight

        public void reset() {
            setFont(offFont);
            setForeground(offColor);
            choiceLabel.setText(" ");
        } // end reset

        public void setCal(GregorianCalendar c) {
            cal = c;
            day = cal.get(Calendar.DAY_OF_MONTH);
            month = cal.get(Calendar.MONTH);

            if (hasDataArray[month][day - 1]) {
                offFont = hasDataFont;
                offColor = hasDataColor;
            } else {
                offFont = noDataFont;
                offColor = noDataColor;
            } // end if
            setFont(offFont);
            setForeground(offColor);
            setText(String.valueOf(day));
        } // end setCal

        //---------------------------------------------
        // Mouse Listener methods
        //---------------------------------------------
        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            if (getText().equals("")) return; // ignore blank days

            boolean rightClick = false;
            int m = e.getModifiers();
            if ((m & InputEvent.BUTTON3_MASK) != 0) rightClick = true;

            if (rightClick) {
                // Turn 'off' this DayLabel if it is right-clicked.
                if (dl == this) {
                    dl.reset();
                    choice = null;
                    if (intSelectionCount > 0) intSelectionCount--;
                } // end if
            } else {
                choice2 = choice;
                choice = cal.getTime();
                intSelectionCount++;
                // System.out.println("Choice is: " + choice);
                // System.out.println("cal is: " + cal.getTime());

                // A left-click on ANY DayLabel will turn off the previous one.
                dl.reset();

                dl = this;
                dl.highlight();

                if (jdTheDialog != null) {
                    if (intSelectionCount >= intNumSelections) {
                        System.out.println("intSelectionCount = " + intSelectionCount);
                        jdTheDialog.setVisible(false);
                    }
                } // end if there is a dialog
                if (parent == null) return;
                if (e.getClickCount() == 2) parent.showDay();
            } // end if/else
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
        }

    } // end DayLabel

    class YearBox extends JPanel implements ItemListener {
        private static final long serialVersionUID = 1555749356147914633L;

        JComboBox<String> y1;
        JComboBox<String> y2;

        public YearBox(String init) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            y1 = new JComboBox<String>();
            y2 = new JComboBox<String>();

            String[] nums = {
                    "00", "01", "02", "03", "04", "05", "06", "07", "08", "09",
                    "10", "11", "12", "13", "14", "15", "16", "17", "18", "19",
                    "20", "21", "22", "23", "24", "25", "26", "27", "28", "29",
                    "30", "31", "32", "33", "34", "35", "36", "37", "38", "39",
                    "40", "41", "42", "43", "44", "45", "46", "47", "48", "49",
                    "50", "51", "52", "53", "54", "55", "56", "57", "58", "59",
                    "60", "61", "62", "63", "64", "65", "66", "67", "68", "69",
                    "70", "71", "72", "73", "74", "75", "76", "77", "78", "79",
                    "80", "81", "82", "83", "84", "85", "86", "87", "88", "89",
                    "90", "91", "92", "93", "94", "95", "96", "97", "98", "99"};

            for (int i = 0; i < nums.length; i++) {
                y1.addItem(nums[i]);
                y2.addItem(nums[i]);
            } // end for i

            select(init);

            y1.setFont(Font.decode("DialogInput-bold-12"));
            y2.setFont(Font.decode("DialogInput-bold-12"));

            add(y1);
            add(y2);

            y1.addItemListener(this);
            y2.addItemListener(this);
        } // end constructor

        public void select(String init) {
            while (init.length() < 4) init = "0" + init;

            y1.setSelectedItem(init.substring(0, 2));
            y2.setSelectedItem(init.substring(2, 4));
        } // end select

        // the ItemListener method
        public void itemStateChanged(ItemEvent ie) {
            int initialYear = Year;

            String year = y1.getSelectedItem().toString() +
                    y2.getSelectedItem().toString();

            Year = Integer.parseInt(year);

            if (initialYear == Year) return; // selection does not represent a change.
            // This happens every time the control is set externally - either by
            //  a +/- key or the Today button.  By the time we arrive here to handle
            //  the change, it's already where we want it to be.
            // One other case - when we actually do handle this control changing,
            //  it will be one of the two comboboxes.  The other combobox will
            //  not need to handle the same event unless it is a rollover.

            System.out.println("Year = " + year);

            int thisYear = Calendar.getInstance().get(Calendar.YEAR);
            if (Year == 0) { // reset to current, if not valid
                Year = thisYear;
                select(String.valueOf(Year));
            } // end if

            dl.reset();     // turn off current choice highlight
            titleLabel.setText("Year " + Year);
//      choice = null;  // will be set again in recalc...
            recalc(Year);

            if (Year == thisYear) {
                dl.reset();     // turn off highlight of the 1st.
                reset();  // to get back to today -
                dl = today;
                dl.highlight();
            } // end if
        } // end itemStateChanged
    } // end class

    public static void main(String[] args) {
        JFrame yvFrame = new JFrame("Year View Test");
        yvFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        MemoryBank.debug = true;

        YearView dcy = new YearView(null);

        yvFrame.getContentPane().add(dcy, "Center");
        yvFrame.pack();
        yvFrame.setVisible(true);
    } // end main
} // end class YearView

