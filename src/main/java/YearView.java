/*
 * User interface to display a full calendar year in Month/Date
 * format, with capability of selection, and giving the user control
 * over which year is displayed.
 * <p>
 * Note 12/29/2006: added the capability to UNselect.
 */

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

public class YearView extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    // Required for use by 'static' section -
    private static String[] monthNames;
    private static String[] weekdayNames;

    // Variables needed by more than one method -
    private LocalDate theChoice;    // constructor, event handling, numerous
    private LocalDate choice2;      // when 2 choices are allowed
    private DayLabel activeDayLabel;            // recalc, event handling
    private JLabel titleLabel;       // constructor, event handling
    private JLabel choiceLabel;      // constructor, highlight
    private JPanel yearPanel;
    private int theYear;            // numerous
    private YearBox yearBox;        // BottomPanel constructor
    private static DateTimeFormatter dtf;
    private AppTreePanel parent = null;
    private static Color hasDataColor = Color.blue;
    private static Color noDataColor = Color.black;
    private static Font hasDataFont = Font.decode("Dialog-bold-16");
    private static Font noDataFont = Font.decode("Dialog-plain-14");
    private boolean[][] hasDataArray;
    private JDialog jdTheDialog;
    private int intNumSelections;
    private int intSelectionCount;
    private JButton todayButton;

    private static final int borderWidth = 2;
    private static LineBorder theBorder;

    static {
        theBorder = new LineBorder(Color.black, borderWidth);

        // Initialize month names.
        monthNames = new String[]{"January", "February", "March",
                "April", "May", "June", "July", "August", "September",
                "October", "November", "December"};

        // Initialize day of week names.
        weekdayNames = new String[]{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

        dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
    } // end static

    YearView() {
        this(LocalDate.now());
    }

    YearView(LocalDate initial) {
        //  @SuppressWarnings("MagicConstant")
        super(new BorderLayout());
        jdTheDialog = null;
        choice2 = null;
        intSelectionCount = 0;

        setBorder(theBorder);

        theChoice = initial;
        theYear = initial.getYear();

        MouseAdapter alterButtonHandler = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                LabelButton source = (LabelButton) e.getSource();
                String s = source.getText();

                if (s.equals("-"))
                    if (theYear > 1) theYear--;
                if (s.equals("+"))
                    if (theYear < 9999) theYear++;

                yearBox.select(String.valueOf(theYear));
                titleLabel.setText("Year " + theYear);

                recalc(theYear);
            } // end mouseClicked
        };// end of new MouseAdapter

        LabelButton prev = new LabelButton("-");
        prev.addMouseListener(alterButtonHandler);
        prev.setPreferredSize(new Dimension(28, 28));
        prev.setFont(Font.decode("Dialog-bold-14"));

        LabelButton next = new LabelButton("+");
        next.addMouseListener(alterButtonHandler);
        next.setPreferredSize(new Dimension(28, 28));
        next.setFont(Font.decode("Dialog-bold-14"));

        JPanel p0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p0.add(prev);
        p0.add(next);
        p0.add(new Spacer(25, 1));

        titleLabel = new JLabel("Year " + theYear);
        titleLabel.setFont(Font.decode("Serif-bold-20"));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setBackground(Color.lightGray);

        yearBox = new YearBox(String.valueOf(initial.getYear()));

        JPanel head1 = new JPanel(new BorderLayout());
        head1.add(p0, "West");
        head1.add(titleLabel, "Center");
        head1.add(yearBox, "East");
        add(head1, BorderLayout.NORTH);

        choiceLabel = new JLabel();
        choiceLabel.setFont(Font.decode("Dialog-bold-18"));
        choiceLabel.setForeground(Color.red);
        choiceLabel.setHorizontalAlignment(JLabel.RIGHT);

        LocalDate tmpLocalDate = LocalDate.of(theYear, 1, 1);
        yearPanel = new JPanel(new GridLayout(3, 4));
        for (int i = 0; i < 12; i++) {
            yearPanel.add(new MonthCanvas(tmpLocalDate));
            tmpLocalDate = tmpLocalDate.plusMonths(1);
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

        recalc(theYear);
    } // end constructor


    // Handler for the 'Today' button.
    public void actionPerformed(ActionEvent e) {
        setChoice(LocalDate.now());
    } // end actionPerformed


    public LocalDate getChoice() {
        return theChoice;
    }

    // When the YearView is used as a Date input dialog,
    //   sometimes two dates are needed.  This method
    //   is provided so the calling context can retrieve
    //   the second choice.
    LocalDate getChoice2() {
        return choice2;
    }

    // This is my own conversion, to numbers that matched these
    // that were being returned by Calendar queries, now deprecated.
    // I put this in place as a temporary remediation along the way
    // to updating the app to new Java 8 date/time classes.
    private int getDayOfWeekInt(LocalDate tmpDate) {
        switch (tmpDate.getDayOfWeek()) {
            case SUNDAY:
                return 1;
            case MONDAY:
                return 2;
            case TUESDAY:
                return 3;
            case WEDNESDAY:
                return 4;
            case THURSDAY:
                return 5;
            case FRIDAY:
                return 6;
            case SATURDAY:
                return 7;
        }
        return -1;
    }

    public void recalc(int year) {
        // Look for new day data, for color/font setting.
        hasDataArray = AppUtil.findDataDays(year);

        LocalDate tmpLocalDate = LocalDate.of(year, 1, 1);
        for (int i = 0; i < 12; i++) {
            MonthCanvas mc = (MonthCanvas) yearPanel.getComponent(i);
            mc.recalc(tmpLocalDate);
            tmpLocalDate = tmpLocalDate.plusMonths(1);
        } // end for i

        if (theChoice == null) {
            choiceLabel.setText(" ");
        } else {
            choiceLabel.setText(dtf.format(theChoice) + " ");
        }
    } // end recalc

    // Called by an external controlling context.
    public void setChoice(LocalDate theNewChoice) {
        if (activeDayLabel != null) activeDayLabel.reset(); // turn off any previous selection.
        if (theNewChoice == null) {
            theChoice = LocalDate.now();
            intSelectionCount = 0;
        } else {
            theChoice = theNewChoice;
            intSelectionCount = 1;
        }
        theYear = theChoice.getYear();
        titleLabel.setText("Year " + theYear);
        yearBox.select(String.valueOf(theYear));
        recalc(theYear);

        if (theNewChoice == null) {
            activeDayLabel.reset();
            theChoice = null;
        } // end if setting no selection
    } // end setChoice


    public void setDialog(JDialog jd, int numSelections) {
        jdTheDialog = jd;
        intNumSelections = numSelections;
        todayButton.setVisible(true);
    } // end setDialog

    void setParent(AppTreePanel atp) {
        parent = atp;
    }

    public class MonthCanvas extends JPanel {
        private static final long serialVersionUID = 1L;

        JPanel monthHeader; // Row 1 = Month Name, Row 2 = (short) Weekday names
        JPanel monthMatrix; // 7 columns by 6 rows (not all used, of course)
        int whichOf12;      // which month we're currently working on (zero-based)
        LocalDate myDate;   // An intial date

        MonthCanvas(LocalDate monthLocalDate) { // constructor
            setLayout(new BorderLayout());
            setBackground(Color.lightGray);
            whichOf12 = monthLocalDate.getMonthValue() - 1;
            JLabel tempLabel;

            // Add the month label to the month header.
            monthHeader = new JPanel();
            monthHeader.setLayout(new GridLayout(2, 1));
            tempLabel = new JLabel(monthNames[whichOf12], JLabel.CENTER);
            monthHeader.setBackground(Color.black);
            tempLabel.setBackground(Color.blue);
            tempLabel.setForeground(Color.white);
            tempLabel.setOpaque(true);
            tempLabel.addMouseListener(new MouseAdapter() {
                public void mousePressed(MouseEvent e) {
                    setChoice(myDate);
                    // System.out.println("Choice is: " + choice);
                    if (parent == null) return;
                    if (e.getClickCount() == 2) parent.showMonth();
                } // end mousePressed
            });//end addMouseListener
            monthHeader.add(tempLabel);

            // Add the day of the week labels to the month header.
            JPanel weekdayNameHeader = new JPanel();
            weekdayNameHeader.setLayout(new GridLayout(1, weekdayNames.length));
            weekdayNameHeader.setBackground(Color.gray);
            for (String dayName : weekdayNames) {
                tempLabel = new JLabel(dayName, JLabel.CENTER);
                tempLabel.setForeground(Color.white);
                tempLabel.setBackground(Color.gray);
                tempLabel.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        setChoice(myDate);
                        // System.out.println("Choice is: " + choice);
                        if (parent == null) return;
                        if (e.getClickCount() == 2) parent.showWeek();
                    } // end mousePressed
                });//end addMouseListener
                weekdayNameHeader.add(tempLabel);
            } // end for i
            monthHeader.add(weekdayNameHeader);

            // Add the month header to the canvas.
            add(monthHeader, BorderLayout.NORTH);

            // Create a grid for all the possible days in a month.
            // The max number of days in any given month is 31 but mapping that onto
            // a matrix of 7 columns (weekdays), you need at least 5 rows.  Then consider
            // that in the first row, you could have up to 6 'blank' days, if the first
            // of the month falls on a Saturday.  That gives us 31+6=37 slots to consider
            // and potentially fill, but that exceeds a 7-by-5 matrix by two slots, so the
            // matrix must be 7x6 which has 42 slots but there's no need to evaluate the
            // ones above the 37th.  Now the numbers are not so magical, once you get the
            // explanation.  But we process it as 1-37 vs 0-36, just for better readability.
            // Adjustment to the layout index is made when we access the zero-based matrix.
            monthMatrix = new JPanel();
            monthMatrix.setLayout(new GridLayout(6, weekdayNames.length));

            // Add day labels to all the possible slots; not all will be used.
            for (int i = 1; i <= 37; i++) {
                monthMatrix.add(new DayLabel());
            } // end for i

            // Add the month matrix to the canvas.
            add(monthMatrix, BorderLayout.CENTER);

            // Draw a border around our canvas.  Much easier than how it was done in MonthView.
            setBorder(LineBorder.createBlackLineBorder());
        } // end constructor

        // This recalc may look very similar to the one in MonthView, but there are significant
        // differences that prevent reuse without major refactoring - the drawing of a month-day
        // requires both a 'box' and a number inside it.  Here, only a numeric label is needed.
        // The sizes are very different, so that here the day names are shortened to two chars.
        // You may want to evaluate the utility of having an abstract common parent class that
        // requires a MonthCanvas and recalc, but currently I don't see enough need for it.
        public void recalc(LocalDate thisMonth) {
            DayLabel tmpDayLabel;

            // The expectation is that 'thisMonth' is a date on the first of the month we need to render.
            LocalDate tmpLocalDate = thisMonth;
            // But you can be disappointed when you have expectations; this ensures it.
            tmpLocalDate = tmpLocalDate.withDayOfMonth(1);

            int dayOfWeek = getDayOfWeekInt(tmpLocalDate); // TODO maybe - refactor so we can just use tmpLocalDate.getDayOfWeek()

            boolean rollover = false;

            // Cycle thru all the (possible) days in our month layout.
            for (int i = 1; i <= 37; i++) {

                // 'blank' days in the first row, before the 1st of the month.
                if (i < dayOfWeek) {
                    ((DayLabel) monthMatrix.getComponent(i - 1)).setText("");
                    continue;
                } // end if

                // 'blank' days in the last row (or two).
                if (rollover) { // blank out trailing days
                    ((DayLabel) monthMatrix.getComponent(i - 1)).setText("");
                    continue;
                } // end if

                // Label this day
                tmpDayLabel = (DayLabel) monthMatrix.getComponent(i - 1);
                tmpDayLabel.setDay(tmpLocalDate);

                // If this day is the current choice then highlight it.
                if (null != theChoice && theChoice.isEqual(tmpLocalDate)) {
                    // If the choice is this day -
                    activeDayLabel = tmpDayLabel;
                    activeDayLabel.highlight();

                    // today is only used in the handler for the 'Today'
                    // button.  Although we set it here to the value of
                    // the current choice which may not really be 'today',
                    // that handler will detect that and call this method
                    // again with the correct choice.
                } // end if

                // Rollover test - if true, then we just processed the last visible day of this month.
                tmpLocalDate = tmpLocalDate.plusDays(1);
                if (thisMonth.getMonth() != tmpLocalDate.getMonth()) rollover = true;
            } // end for i
        } // end recalc

        public Dimension getPreferredSize() {
            return new Dimension(160, 130);
        } // end getPreferredSize
    } // end MonthCanvas

    class DayLabel extends JLabel implements MouseListener {
        private static final long serialVersionUID = 1L;

        int day;
        int month;
        LocalDate myDate;
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
            choiceLabel.setText(dtf.format(theChoice) + " ");
        } // end highlight

        public void reset() {
            setFont(offFont);
            setForeground(offColor);
            choiceLabel.setText(" ");
        } // end reset

        public void setDay(LocalDate aLocalDate) {
            myDate = aLocalDate;
            day = myDate.getDayOfMonth();    // these may be wrong...
            month = myDate.getMonthValue() - 1;

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
        } // end setDay

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
                if (activeDayLabel == this) {
                    activeDayLabel.reset();
                    theChoice = null;
                    if (intSelectionCount > 0) intSelectionCount--;
                } // end if
            } else {
                choice2 = theChoice;  // TODO need to verify this is working...
                theChoice = myDate;
                intSelectionCount++;
                // System.out.println("Choice is: " + choice);

                // A left-click on ANY DayLabel will turn off the previous one.
                activeDayLabel.reset();

                activeDayLabel = this;
                activeDayLabel.highlight();

                // If the YearView is acting as a choice selection dialog -
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

        YearBox(String init) {
            super(new FlowLayout(FlowLayout.LEFT, 0, 0));
            y1 = new JComboBox<>();
            y2 = new JComboBox<>();

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

            for (String num : nums) {
                y1.addItem(num);
                y2.addItem(num);
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
            StringBuilder initBuilder = new StringBuilder(init);
            while (initBuilder.length() < 4) initBuilder.insert(0, "0");
            init = initBuilder.toString();

            y1.setSelectedItem(init.substring(0, 2));
            y2.setSelectedItem(init.substring(2, 4));
        } // end select

        // the ItemListener method of the YearBox
        public void itemStateChanged(ItemEvent ie) {
            int initialYear = theYear;

            String year = Objects.requireNonNull(y1.getSelectedItem()).toString() +
                    Objects.requireNonNull(y2.getSelectedItem()).toString();

            theYear = Integer.parseInt(year);

            if (initialYear == theYear) return; // selection does not represent a change.
            // This happens every time the control is set externally - either by
            //  a +/- key or the Today button.  By the time we arrive here to handle
            //  the change, it's already where we want it to be.
            // One other case - when we actually do handle this control changing,
            //  it will be one of the two comboboxes.  The other combobox will
            //  not need to handle the same event unless it is a rollover.

            System.out.println("Year = " + year);

            //int thisYear = Calendar.getInstance().get(Calendar.YEAR);
            if (theYear == 0) { // reset to current, if not valid
                theYear = LocalDate.now().getYear();
                select(String.valueOf(theYear));
            } // end if

            activeDayLabel.reset();     // turn off current choice highlight
            titleLabel.setText("Year " + theYear);
            recalc(theYear);

        } // end itemStateChanged
    } // end class

} // end class YearView

