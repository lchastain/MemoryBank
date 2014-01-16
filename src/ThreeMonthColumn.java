// File name: ThreeMonthColumn.java  	by D. Lee Chastain
//                                                                        
// Description:  A control to select a date from a column 
//    that displays three months at a time.  it allows the user 
//    to advance or regress the month shown.
//                                                                       
// Modification History:
// ---------------------
// 07/05/2005 Changed reference to images location.
// 10/07/2004 Removed the inner class AlterButton.

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;

public class ThreeMonthColumn extends JPanel {
    private static final long serialVersionUID = 6264848411961477735L;

    private static final int BOTH = 1000;
    private static final int DOWN = 1001;
    private static final int UP = 1002;

    // Required for use by 'static' section -
    private static String[] monthNames;
    private static String[] weekNames;

    // Variables needed by more than one method -
    private Date choice;            // constructor, event handling, numerous
    private DayLabel dl;            // recalc, event handling
    private MonthColumn monthColumn;

    private GregorianCalendar cal;
    private DateSelection twimc; // To Whom It May Concern
    // The DateSelection interface is defined in EventNoteGroup
    // Why not do it here?

    public ThreeMonthColumn() {
        super(new BorderLayout());

        cal = (GregorianCalendar) Calendar.getInstance();
        // Note: getInstance at this time returns a Calendar that
        //   is actually a GregorianCalendar, but since the return
        //   type is Calendar, it must be cast in order to assign.

        cal.setGregorianChange(new GregorianCalendar(1752,
                Calendar.SEPTEMBER, 14).getTime());

        // The container for the 3 months in one column.
        monthColumn = new MonthColumn();

        add(monthColumn, BorderLayout.CENTER);

        twimc = null;
    } // end constructor


    // Set the value of the 'middle' month.
    public void setBaseDate(Date d) {
        cal.setTime(d);
        monthColumn.recalc();
    } // end setBaseDate

    // Set the current choice.
    // Called internally after a up/down click, and from external.
    public void setChoice(Date d) {
        choice = d;
        if (choice == null) {
            if (dl != null) {
                dl.reset();   // turn off current highlight
                dl = null;
            } // end if
        } else {
            monthColumn.showChoice(choice);
        } // end if
    } // end setChoice

    public void setSubscriber(DateSelection ds) {
        twimc = ds;
    }

    class MonthColumn extends JPanel {
        private static final long serialVersionUID = 2846961344308086506L;

        GregorianCalendar preCal;
        GregorianCalendar middleCal;
        GregorianCalendar postCal;
        MonthCanvas preMonth;
        MonthCanvas middleMonth;
        MonthCanvas postMonth;

        public MonthColumn() {
            super(new GridLayout(3, 1));
            preCal = (GregorianCalendar) Calendar.getInstance();
            middleCal = (GregorianCalendar) Calendar.getInstance();
            postCal = (GregorianCalendar) Calendar.getInstance();

            // Create the three month views
            preMonth = new MonthCanvas(preCal);
            preMonth.showAlterButtons(UP);
            middleMonth = new MonthCanvas(middleCal);
            postMonth = new MonthCanvas(postCal);
            postMonth.showAlterButtons(DOWN);

            // Add them to the panel
            add(preMonth);
            add(middleMonth);
            add(postMonth);

            recalc();
        } // end constructor

        public void recalc() {
            // 'cal' is external to this inner class, and should always be
            // set correctly prior to calling recalc.

            preCal.setTime(cal.getTime());  // Initial
            preCal.add(Calendar.MONTH, -1); // less one month

            middleCal.setTime(cal.getTime());  // Already correct

            postCal.setTime(cal.getTime());  // Initial
            postCal.add(Calendar.MONTH, 1);  // plus one month

            preMonth.recalc(preCal);
            middleMonth.recalc(middleCal);
            postMonth.recalc(postCal);

        } // end recalc

        public void showChoice(Date d) {

            //----------------------------------------------------------
            // Make a temporary Calendar that we can use to get the
            //  Year, Month, and Date out of the input date.
            //----------------------------------------------------------
            GregorianCalendar tmpCal;
            int theYear;
            int theMonth;
            int theDay;

            tmpCal = (GregorianCalendar) Calendar.getInstance();
            tmpCal.setTime(d);

            theYear = tmpCal.get(Calendar.YEAR);
            theMonth = tmpCal.get(Calendar.MONTH);
            theDay = tmpCal.get(Calendar.DAY_OF_MONTH);
            //----------------------------------------------------------

            //---------------------------------------------------------------
            // Now use the Year and Month to acquire the correct MonthCanvas.
            //---------------------------------------------------------------
            MonthCanvas mc = null;
            if (theYear == preCal.get(Calendar.YEAR))
                if (theMonth == preCal.get(Calendar.MONTH))
                    mc = (MonthCanvas) getComponent(0);

            if (theYear == middleCal.get(Calendar.YEAR))
                if (theMonth == middleCal.get(Calendar.MONTH))
                    mc = (MonthCanvas) getComponent(1);

            if (theYear == postCal.get(Calendar.YEAR))
                if (theMonth == postCal.get(Calendar.MONTH))
                    mc = (MonthCanvas) getComponent(2);
            //---------------------------------------------------------------

            if (mc == null) return;
            // System.out.println("Highlighting " + theDay + " of " + mc.monthLabel.getText());
            mc.showDay(theDay);
        } // end showChoice
    } // end class MonthColumn

    // A MonthCanvas is a component that represents a given month of
    //  a given year.  The Month name and 4-digit numeric year are
    //  displayed in a heading line, above the labels for the days
    //  of the week.  Below the two-character day labels is the
    //  7 columns by n rows grid of individual numeric days.
    public class MonthCanvas extends JPanel {
        private static final long serialVersionUID = -3123913094831825936L;

        JPanel p0;      // Container for the AlterButtons
        JPanel p1;      // Container for the two Header lines.
        JPanel p2;      // Contains the month grid of days.
        JLabel monthLabel;
        LabelButton upAb;
        LabelButton downAb;

        MonthCanvas(GregorianCalendar init_cal) { // constructor
            // The Month/Year label is at the CENTER of a BorderLayout JPanel.
            // In the EAST of that same JPanel is another JPanel
            // that contains the AlterButtons in their own FlowLayout.

            setLayout(new BorderLayout());
            setBorder(BorderFactory.createLineBorder(Color.black, 2));
            setBackground(Color.lightGray);

            MouseAdapter ma = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    LabelButton source = (LabelButton) e.getSource();

                    if (dl != null) dl.reset();   // turn off current highlight

                    if (source == downAb) cal.add(Calendar.MONTH, 1);
                    if (source == upAb) cal.add(Calendar.MONTH, -1);

                    monthColumn.recalc();

                    // Re-highlight the current choice, if possible.
                    if (choice != null) monthColumn.showChoice(choice);
                } // end mouseClicked
            };// end of new MouseAdapter

            // Create the Alter Buttons
            char c = java.io.File.separatorChar;
            String iString = MemoryBank.logHome + c + "images" + c;

            downAb = new LabelButton();
            downAb.addMouseListener(ma);
            downAb.setIcon(new ImageIcon(iString + "down.gif"));
            downAb.setPreferredSize(new Dimension(28, 28));

            upAb = new LabelButton();
            upAb.addMouseListener(ma);
            upAb.setIcon(new ImageIcon(iString + "up.gif"));
            upAb.setPreferredSize(new Dimension(28, 28));

            p0 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            p0.add(downAb);
            p0.add(upAb);
            downAb.setVisible(false); // Default is off.
            upAb.setVisible(false);   // Default is off.

            // Add the month label.
            p1 = new JPanel(new BorderLayout());
            p1.setBackground(Color.black);
            monthLabel = new JLabel("   ", JLabel.CENTER);
            monthLabel.setFont(Font.decode("Serif-bold-18"));
            monthLabel.setBackground(Color.blue);
            monthLabel.setOpaque(true);
            monthLabel.setForeground(Color.white);

            JPanel head1 = new JPanel(new BorderLayout());
            head1.setBackground(Color.blue);
            head1.add(p0, "East");
            head1.add(monthLabel, "Center");
            p1.add(head1, "North");

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
            p1.add(head2, "South");

            // Create a grid for the days (up to 6 rows, 7 columns).
            p2 = new JPanel();
            p2.setLayout(new GridLayout(0, weekNames.length));

            recalc(init_cal);
            add(p1, BorderLayout.NORTH);
            add(p2, BorderLayout.CENTER);
        } // end constructor

        public void showAlterButtons(int which) {
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

        public void recalc(GregorianCalendar mc_cal) {
            DayLabel tmp;
            boolean firstTime = false;  // first time?
            if (p2.getComponentCount() == 0) firstTime = true;

            // Generate new title with month and year.
            int theMonth = mc_cal.get(Calendar.MONTH);
            int theYear = mc_cal.get(Calendar.YEAR);
            String labelText = monthNames[theMonth] + " " + String.valueOf(theYear);
            monthLabel.setText(labelText);

            // Clone the calendar so we can advance it a day at a time,
            //  both as a source for the 'c's below and for a rollover test.
            GregorianCalendar tmpCal = (GregorianCalendar) mc_cal.clone();
            tmpCal.set(Calendar.HOUR, 0);
            tmpCal.set(Calendar.MINUTE, 0);
            tmpCal.set(Calendar.SECOND, 0);

            // Get the day of the week of the first day.
            tmpCal.set(Calendar.DAY_OF_MONTH, 1);
            int dayOfWeek = tmpCal.get(Calendar.DAY_OF_WEEK);
            int month = tmpCal.get(Calendar.MONTH);

            boolean rollover = false;

            // Add the days.  Enough room for 31 days across 6 rows.
            for (int i = 1; i <= 37; i++) {

                //---------------------------------------------------
                // 'blank' days in the first week, before the 1st.
                if (i < dayOfWeek) {
                    if (firstTime) p2.add(new DayLabel());
                    else ((DayLabel) p2.getComponent(i - 1)).setCal();
                    continue;
                } // end if

                // 'blank' days in the last week (or two).
                if (rollover) { // blank out trailing days
                    if (firstTime) p2.add(new DayLabel());
                    else ((DayLabel) p2.getComponent(i - 1)).setCal();
                    continue;
                } // end if

                // leave the declaration inside the loop;
                //    we want a new instance each time.
                GregorianCalendar c = (GregorianCalendar) tmpCal.clone();

                // Set the date - source cal is 'c'
                if (firstTime) {
                    tmp = new DayLabel(c);
                    p2.add(tmp);
                } else {
                    tmp = (DayLabel) p2.getComponent(i - 1);
                    tmp.setCal(); // remove previous date and MouseListener.
                    tmp.setCal(c);
                } // end if
                //---------------------------------------------------

                // Now go to the next day, check for month rollover.
                tmpCal.add(Calendar.DATE, 1);
                if (month != tmpCal.get(Calendar.MONTH)) rollover = true;
            } // end for i
        } // end recalc

        // Turn on the highlight for the specified Day.
        public void showDay(int theDay) {
            DayLabel tmpDl;

            for (int i = 0; i < 37; i++) {
                tmpDl = (DayLabel) p2.getComponent(i);
                // System.out.println("Comparing to: " + tmpDl.day);
                if (tmpDl.getText().equals(String.valueOf(theDay))) {
                    if (dl != null) dl.reset();
                    dl = tmpDl;
                    dl.highlight();
                    return;
                } // end if
            } // end for i
        } // end showDay

        public Dimension getPreferredSize() {
            return new Dimension(160, 150);
        } // end getPreferredSize
    } // end MonthCanvas

    class DayLabel extends JLabel implements MouseListener {
        private static final long serialVersionUID = -4274327839537935918L;

        int day;
        GregorianCalendar dcal;

        DayLabel(GregorianCalendar cal) {
            this();
            dcal = cal;
            addMouseListener(this);
            day = dcal.get(Calendar.DAY_OF_MONTH);
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

        public void setCal(GregorianCalendar c) {
            dcal = c;
            addMouseListener(this);
            day = dcal.get(Calendar.DAY_OF_MONTH);
            if (day == -1) return;
            setText(String.valueOf(day));
        } // end setCal

        public void setCal() {
            dcal = null;
            removeMouseListener(this);
            setText("");
        } // end setCal

        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            choice = new Date();
            choice.setTime(dcal.getTime().getTime());
            // System.out.println("Choice is: " + choice);
            // System.out.println("dcal is: " + dcal.getTime());

            // Manage the highlighting.
            if (dl == this) {  // Selected this one again.
                // This implements an on/off 'toggle'.
                dl.reset();
                dl = null;
                choice = null;
            } else {  // A different previous selection, or none at all.
                // Turn off the previous, if any.
                if (dl != null) dl.reset();

                dl = this;
                dl.highlight();
            } // end if

            if (twimc != null) twimc.dateSelected(choice);

        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
        }

    } // end DayLabel

    public Date getChoice() {
        return choice;
    } // end getChoice

    static {
        // Initialize month names.
        monthNames = new String[]{"January", "February", "March",
                "April", "May", "June", "July", "August", "September",
                "October", "November", "December"};

        // Initialize day of week names.
        weekNames = new String[]{"Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"};

    } // end static

    public static void main(String[] args) {
        Frame dcFrame = new Frame("ThreeMonthColumn test");
        ThreeMonthColumn dc = new ThreeMonthColumn();

        // Just as a test of the ability to initialize to a value -
        dc.setBaseDate(new GregorianCalendar(1999, 4, 12).getTime());

        dcFrame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        // Needed to override the 'metal' L&F for Swing components.
        String laf = UIManager.getSystemLookAndFeelClassName();
        Exception e = null;
        try {
            UIManager.setLookAndFeel(laf);
        } catch (UnsupportedLookAndFeelException ulafe) {
            e = ulafe;
        } catch (InstantiationException iee) {
            e = iee;
        } catch (ClassNotFoundException cnfe) {
            e = cnfe;
        } catch (IllegalAccessException iae) {
            e = iae;
        } // end try/catch
        if(e != null) System.out.println("Exception: " + e.getMessage());
        SwingUtilities.updateComponentTreeUI(dc);

        dcFrame.add(dc);
        dcFrame.pack();
        dcFrame.setVisible(true);
    } // end main

} // end class ThreeMonthColumn


//Interface for operations that should occur when a date is selected.
interface DateSelection {
    public abstract void dateSelected(Date d);
} // end DateSelection

