//
// Description:  A control to select a date from a column 
//    that displays three months at a time.  it allows the user 
//    to advance or regress the month shown.
//                                                                       
// Modification History:
// ---------------------
// 07/05/2005 Changed reference to images location.
// 10/07/2004 Removed the inner class AlterButton.

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.time.LocalDate;
import java.time.Month;

public class ThreeMonthColumn extends JPanel {
    private static final long serialVersionUID = 1L;

    private static final int BOTH = 1000;
    private static final int DOWN = 1001;
    private static final int UP = 1002;

    // Required for use by 'static' section -
    private static String[] monthNames;
    private static String[] weekNames;

    // Variables needed by more than one method -
    private LocalDate theChoice;            // constructor, event handling, numerous
    private DayLabel dayLabel;      // recalc, event handling
    private MonthColumn monthColumn;

    private LocalDate baseDate;
    private DateSelection twimc; // To Whom It May Concern - the subscriber.

    static {
        // Initialize month names.
        monthNames = new String[]{"January", "February", "March",
                "April", "May", "June", "July", "August", "September",
                "October", "November", "December"};

        // Initialize day of week names.
        weekNames = new String[]{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};
    } // end static

    public ThreeMonthColumn() {
        super(new BorderLayout());

        baseDate = LocalDate.now();

        // The container for the 3 months in one column.
        monthColumn = new MonthColumn();

        add(monthColumn, BorderLayout.CENTER);

        twimc = null; // To Whom It May Concern - the subscriber.
    } // end constructor


    // Set the current choice.
    // Called internally after a up/down click, and from external.
    public void setChoice(LocalDate ld) {
        theChoice = ld;
        if (ld == null) {
            if (dayLabel != null) {
                dayLabel.reset();   // turn off current highlight
                dayLabel = null;
            } // end if
            return;
        } // end if

        int theYear = baseDate.getYear();
        Month theMonth = baseDate.getMonth();
        if (theYear != ld.getYear() || theMonth != ld.getMonth()) {
            baseDate = ld;
            monthColumn.recalc();
        }

        // Turn on the highlight for the DayLabel matching ld -
        monthColumn.showChoice(theChoice);
    } // end setChoice

    void setSubscriber(DateSelection ds) {
        twimc = ds;
    }

    class MonthColumn extends JPanel {
        private static final long serialVersionUID = 1L;

        MonthCanvas topMonthCanvas;
        MonthCanvas middleMonthCanvas;
        MonthCanvas bottomMonthCanvas;

        MonthColumn() {
            super(new GridLayout(3, 1));

            // Create the three month views
            topMonthCanvas = new MonthCanvas();
            topMonthCanvas.showAlterButtons(UP);
            middleMonthCanvas = new MonthCanvas();
            bottomMonthCanvas = new MonthCanvas();
            bottomMonthCanvas.showAlterButtons(DOWN);

            // Add them to the panel
            add(topMonthCanvas);
            add(middleMonthCanvas);
            add(bottomMonthCanvas);

            recalc();
        } // end constructor

        public void recalc() {
            // 'baseDate' is external to this inner class, and should always be
            // set correctly prior to calling recalc.
            topMonthCanvas.recalc(baseDate.minusMonths(1));
            middleMonthCanvas.recalc(baseDate);
            bottomMonthCanvas.recalc(baseDate.plusMonths(1));
        } // end recalc

        void showChoice(LocalDate ld) {
            if (ld == null) return;

            int theYear = ld.getYear();
            int theMonth = ld.getMonthValue();
            int theDay = ld.getDayOfMonth();

            //---------------------------------------------------------------
            // Now use the Year and Month to acquire the correct MonthCanvas.
            //---------------------------------------------------------------
            MonthCanvas mc = null;
            LocalDate searchDate = baseDate.minusMonths(1);
            if (theYear == searchDate.getYear())
                if (theMonth == searchDate.getMonthValue())
                    mc = (MonthCanvas) getComponent(0);

            if (theYear == baseDate.getYear())
                if (theMonth == baseDate.getMonthValue())
                    mc = (MonthCanvas) getComponent(1);

            searchDate = baseDate.plusMonths(1);
            if (theYear == searchDate.getYear())
                if (theMonth == searchDate.getMonthValue())
                    mc = (MonthCanvas) getComponent(2);
            //---------------------------------------------------------------

            if (mc == null) return;
            // System.out.println("Highlighting " + theDay + " of " + mc.monthLabel.getText());
            mc.showDay(theDay);
        } // end showChoice
    } // end class MonthColumn

    // A MonthCanvas is a component that represents a given month of
    //  a given year.  The Month name and Year is
    //  displayed in a heading line, above the labels for the days
    //  of the week.  Below the two-character day labels is the
    //  7 columns by n rows grid of individual numeric days.
    public class MonthCanvas extends JPanel {
        private static final long serialVersionUID = 1L;

        JPanel alterButtonPanel;  // Container for the AlterButtons
        JPanel headerPanel;       // Container for the two Header lines - Month name and Day Names.
        JPanel monthGridPanel;    // Contains the month grid of days.
        JLabel monthLabel;
        LabelButton upAb;
        LabelButton downAb;

        MonthCanvas() {
            // In this constructor we don't set any dates;
            // Here, all we do is build the Panel.
            // The Month/Year label is at the CENTER of a BorderLayout JPanel.
            // In the EAST of that same JPanel is another JPanel
            // that contains the AlterButtons in their own FlowLayout.
            // Of the two alterbuttons, zero or one will be visible.

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createLineBorder(Color.black, 2));
            setBackground(Color.lightGray);

            MouseAdapter ma = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    LabelButton source = (LabelButton) e.getSource();

                    if (dayLabel != null) dayLabel.reset();   // turn off current highlight

                    if (source == downAb) baseDate = baseDate.plusMonths(1);
                    if (source == upAb) baseDate = baseDate.minusMonths(1);

                    monthColumn.recalc();

                    // Re-highlight the current choice, if possible.
                    if (theChoice != null) monthColumn.showChoice(theChoice);
                } // end mouseClicked
            };// end of new MouseAdapter

            // Create the Alter Buttons
            char c = java.io.File.separatorChar;
            String iString = MemoryBank.logHome + c + "images" + c;

            downAb = new LabelButton("", LabelButton.DOWN);
            downAb.addMouseListener(ma);
            downAb.setPreferredSize(new Dimension(28, 28));

            upAb = new LabelButton("", LabelButton.UP);
            upAb.addMouseListener(ma);
            upAb.setPreferredSize(new Dimension(28, 28));

            alterButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            alterButtonPanel.add(downAb);
            alterButtonPanel.add(upAb);
            downAb.setVisible(false); // Default is off.
            upAb.setVisible(false);   // Default is off.

            // Add the month label.
            headerPanel = new JPanel(new BorderLayout());
            headerPanel.setBackground(Color.black);
            monthLabel = new JLabel("   ", JLabel.CENTER);
            monthLabel.setFont(Font.decode("Serif-bold-18"));
            monthLabel.setBackground(Color.blue);
            monthLabel.setOpaque(true);
            monthLabel.setForeground(Color.white);

            JPanel head1 = new JPanel(new BorderLayout());
            head1.setBackground(Color.blue);
            head1.add(alterButtonPanel, "East");
            head1.add(monthLabel, "Center");
            headerPanel.add(head1, "North");

            // Add the day of the week labels.
            JPanel head2 = new JPanel();
            head2.setLayout(new GridLayout(1, weekNames.length));
            head2.setBackground(Color.gray);
            for (String weekName : weekNames) {
                JLabel l = new JLabel(weekName, JLabel.CENTER);
                l.setForeground(Color.white);
                l.setBackground(Color.gray);
                head2.add(l);
            } // end for i
            headerPanel.add(head2, "South");

            // Create a grid for the days (up to 6 rows, 7 columns).
            monthGridPanel = new JPanel();
            monthGridPanel.setLayout(new GridLayout(0, weekNames.length));

            add(headerPanel, BorderLayout.NORTH);
            add(monthGridPanel, BorderLayout.CENTER);
        } // end constructor

        void showAlterButtons(int which) {
            switch (which) {
                case BOTH:
                    downAb.setVisible(true);
                    upAb.setVisible(true);
                    break;
                case DOWN:
                    downAb.setVisible(true);
                    break;
                case UP:
                    upAb.setVisible(true);
                    break;
            } // end switch
        } // end showAlterButtons

        public void recalc(LocalDate theBaseDate) {
            DayLabel tmpDayLabel;
            boolean firstTime = false;  // first time?
            if (monthGridPanel.getComponentCount() == 0) firstTime = true;

            // Generate new title with month and year.
            int theMonth = theBaseDate.getMonthValue() - 1;
            int theYear = theBaseDate.getYear();
            String labelText = monthNames[theMonth] + " " + theYear;
            monthLabel.setText(labelText);

            // Get the day of the week of the first day.
            LocalDate tmpDate = theBaseDate.withDayOfMonth(1);
            int dayOfWeek = AppUtil.getDayOfWeekInt(tmpDate);
            int month = tmpDate.getMonthValue() - 1;

            boolean rollover = false;

            // Add the days.  Enough room for 31 days across 6 rows.
            for (int i = 1; i <= 37; i++) {
                // 'blank' days in the first week, before the 1st.
                if (i < dayOfWeek) {
                    if (firstTime) monthGridPanel.add(new DayLabel());
                    else ((DayLabel) monthGridPanel.getComponent(i - 1)).setDay(null);
                    continue;
                } // end if

                // 'blank' days in the last week (or two).
                if (rollover) { // blank out trailing days
                    if (firstTime) monthGridPanel.add(new DayLabel());
                    else ((DayLabel) monthGridPanel.getComponent(i - 1)).setDay(null);
                    continue;
                } // end if

                // Set the date
                if (firstTime) {
                    tmpDayLabel = new DayLabel(tmpDate);
                    monthGridPanel.add(tmpDayLabel);
                } else {
                    tmpDayLabel = (DayLabel) monthGridPanel.getComponent(i - 1);
                    tmpDayLabel.setDay(null); // remove previous date and MouseListener.
                    tmpDayLabel.setDay(tmpDate);
                } // end if
                //---------------------------------------------------

                // Now go to the next day, check for month rollover.
                tmpDate = tmpDate.plusDays(1);
                if (month != tmpDate.getMonthValue() - 1) rollover = true;
            } // end for i
        } // end recalc

        // Turn on the highlight for the specified Day.
        public void showDay(int theDay) {
            DayLabel tmpDl;

            for (int i = 0; i < 37; i++) {
                tmpDl = (DayLabel) monthGridPanel.getComponent(i);
                // System.out.println("Comparing to: " + tmpDl.day);
                if (tmpDl.getText().equals(String.valueOf(theDay))) {
                    if (dayLabel != null) dayLabel.reset();
                    dayLabel = tmpDl;
                    dayLabel.highlight();
                    return;
                } // end if
            } // end for i
        } // end showDay

        public Dimension getPreferredSize() {
            return new Dimension(160, 150);
        } // end getPreferredSize
    } // end MonthCanvas

    class DayLabel extends JLabel implements MouseListener {
        private static final long serialVersionUID = 1L; // Demanded by JLabel

        int day;
        LocalDate myDate;

        DayLabel(LocalDate cal) {
            this();
            myDate = cal;
            addMouseListener(this);
            day = myDate.getDayOfMonth();
            setText(String.valueOf(day));
        } // end constructor

        DayLabel() {
            super("", CENTER);
            setBackground(Color.lightGray);
            setFont(Font.decode("Dialog-plain-14"));
            setText("");
        } // end constructor

        public void highlight() {
            setForeground(Color.red);
            setFont(Font.decode("Dialog-bold-18"));
        } // end highlight

        public void reset() {
            setFont(Font.decode("Dialog-plain-14"));
            setForeground(Color.black);
        } // end reset

        public void setDay(LocalDate ld) {
            if (ld == null) {
                myDate = null;
                removeMouseListener(this);
                setText("");
            } else {
                myDate = ld;
                addMouseListener(this);
                day = myDate.getDayOfMonth();
                setText(String.valueOf(day));
            }
        } // end setDay

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            theChoice = myDate;
            // System.out.println("Choice is: " + choice);
            // System.out.println("dcal is: " + dcal.getTime());

            // Manage the highlighting.
            if (dayLabel == this) {  // Selected this one again.
                // This implements an on/off 'toggle'.
                dayLabel.reset();
                dayLabel = null;
                theChoice = null;
            } else {  // A different previous selection, or none at all.
                // Turn off the previous, if any.
                if (dayLabel != null) dayLabel.reset();

                dayLabel = this;
                dayLabel.highlight();
            } // end if

            if (twimc != null) {
                assert theChoice != null;
                twimc.dateSelected(theChoice);
            }

        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
        }

    } // end DayLabel

    public LocalDate getChoice() {
        return theChoice;
    } // end getChoice

} // end class ThreeMonthColumn


