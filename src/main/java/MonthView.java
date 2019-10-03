/* User interface to choose a Date from a view of a Month.
 */

import com.fasterxml.jackson.core.type.TypeReference;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Vector;

public class MonthView extends JLayeredPane {
    private static final long serialVersionUID = -1L;
    // As a container, this class has only two items:
    //   a MonthCanvas (this one is quite complex) and a JLabel (shows the current date selection).
    //
    //   Because the container has no layout manager, the Bounds
    //   of both items must be set explicitly.

    // No need to have more than one of the vars below (so static),
    // but need them to be visible to more than one method here.
    private static String[] monthNames;
    private static String[] dayNames;
    private static JPanel monthGrid;
    private static DateTimeFormatter dtf;
    private static LocalDate choice;
    private static Color hasDataColor = Color.blue;
    private static Color noDataColor = Color.black;
    private static Font hasDataFont = Font.decode("Dialog-bold-20");
    private static Font noDataFont = Font.decode("Dialog-bold-16");
    private static int visibleYear;
    private static int visibleMonth;
    private static LocalDate displayedMonth;  // Of course it also holds a year and date

    // Variables needed by more than one method -
    private DayCanvas activeDayCanvas;   // recalc, event handling
    private MonthCanvas monthCanvas;
    private JLabel choiceLabel;
    private int heightOffset = 0;
    private AppTreePanel parent = null;
    private boolean[][] hasDataArray;  // for a year.  index 0 = month, index 1 = days, values have data True or False
    private Dimension minSize;

    private static final int borderWidth = 2;
    private static LineBorder theBorder;

    static {
        theBorder = new LineBorder(Color.black, borderWidth);

        // Initialize month names.
        monthNames = new String[]{"January", "February", "March",
                "April", "May", "June", "July", "August", "September",
                "October", "November", "December"};

        // Initialize day of week names.
        dayNames = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday"};

        // Create a grid for the days (6 rows, 7 columns).
        monthGrid = new JPanel(new GridLayout(6, dayNames.length));

        dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        choice = null;
    } // end static

    //--------------------------------------------------------------------
    // The MonthView constructors -
    //
    //--------------------------------------------------------------------
    MonthView() {
        this(LocalDate.now());
    }

    MonthView(LocalDate initial) {
        super();
        displayedMonth = initial;

        minSize = new Dimension(480, 200);  // 450
        // The values of minSize were derived simply by T&E.
        // With the new variable number of icons, it turns out that the limiting
        //   shrinking factor at this time is the length of the day names.
        //   ie - 'Wednesday'.

        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                // Note - Do NOT call getPreferredSize from either
                //   here or setLabelBounds; it will produce loops
                //   and undetermined results.
                Dimension d = getParent().getSize();
                int width, height;

                width = d.width;
                if (d.width < minSize.width) width = minSize.width;

                height = d.height - heightOffset;
                if (d.height < minSize.height) height = minSize.height;

                monthCanvas.setBounds(0, 0, width, height);

                setLabelBounds();  // for choiceLabel
                validate(); // needed in standalone -
            } // end componentResized
        });

        for (int i = 1; i <= 37; i++) {
            monthGrid.add(new DayCanvas());
        } // end for i

        int initialYear = initial.getYear();
        int initialMonth = initial.getMonthValue() - 1;

        visibleYear = initialYear;
        visibleMonth = initialMonth;
        hasDataArray = AppUtil.findDataDays(visibleYear);

        monthCanvas = new MonthCanvas();
        monthCanvas.setBorder(theBorder);

        choiceLabel = new JLabel();
        choiceLabel.setFont(Font.decode("Dialog-bold-18"));
        choiceLabel.setForeground(Color.red);

        MouseAdapter ma = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setChoice(choice);
            } // end mouseClicked
        };// end of new MouseAdapter
        choiceLabel.addMouseListener(ma);

        // These 'adds' NEED to be adding Integer, not int.
        // If you use 'int' it will appear to work but will
        // not have the desired Z-effect, and the choiceLabel
        // will fall behind the monthCanvas and appear to
        // not be showing.
        add(monthCanvas, new Integer(0));
        add(choiceLabel, new Integer(1));

        setChoice(initial);
    } // end constructor


    //---------------------------------------------------------------------
    // Method Name: getIconArray
    //
    // Returns an array of 5 LogIcons that are read from a file
    //   of data for the specified day.  There may be one or more
    //   null placeholders in the array.
    //---------------------------------------------------------------------
    private Image[] getIconArray(int year, int month, int day) {
        LocalDate ld = LocalDate.of(year, month, day);
//        MemoryBank.tempCalendar.set(year, month, day);

        String theFilename = AppUtil.findFilename(ld, "D");
//        String theFilename = AppUtil.OldFindFilename(MemoryBank.tempCalendar, "D");
        if (!new File(theFilename).exists()) return null;

        MemoryBank.debug("Loading: " + theFilename);
        Image[] returnArray = new Image[5];

        int index = 0;
//        boolean doit;
        String iconFileString;

        Object[] theDayGroup = AppUtil.loadNoteGroupData(theFilename);
        Vector<DayNoteData> theDayNotes = AppUtil.mapper.convertValue(theDayGroup[0], new TypeReference<Vector<DayNoteData>>() {
        });
        for (DayNoteData tempDayData : theDayNotes) {
            if (tempDayData.getShowIconOnMonthBoolean()) {
                iconFileString = tempDayData.getIconFileString();
                if (iconFileString == null) {
                    // The default
                    iconFileString = DayNoteGroup.defaultIconFileName;
                } // end if

                if (iconFileString.equals("")) {
                    // Show this 'blank' on the month.
                    // Possibly as a 'spacer'.
                    returnArray[index] = null;
                } else {
                    returnArray[index] = new AppIcon(iconFileString).getImage();
                } // end if

                index++;
                MemoryBank.debug("MonthView - Set icon " + index);
                if (index > 4) break;
            } // end if
        }

        return returnArray;
    } // end getIconArray


    //-------------------------------------------------------------------
    // Method Name:  getPreferredSize
    //
    // This method is called by the containing scrollpane to determine
    //   need for scrollbars.
    //-------------------------------------------------------------------
    public Dimension getPreferredSize() {
        Dimension d = getParent().getSize();
        if (d.width == 0) return minSize;
        int width, height;

        width = d.width;
        if (d.width < minSize.width) width = minSize.width;

        height = d.height - heightOffset;
        if (d.height < minSize.height) height = minSize.height;

        return new Dimension(width, height);
    } // end getPreferredSize

    public LocalDate getChoice() {
        return choice;
    }

    public void setChoice(LocalDate theChoice) {
        // Was tempted (for better performance) to avoid the recalc, if the new choice was still on the same month
        // as the previous choice, but that doesn't work here - while 'away', new notes (with icons) might have been
        // added to any given day of the visible month, and if we are not on the exact same day then the choice would
        // also be wrong.  If we want to first check to see if a recalc-worthy change was made, then we would need to
        // add new flags in various places and that introduces more complexity to the feature, making it more fragile
        // and harder to maintain, with questionable improvement to performance when displaying any month with a lower
        // number of icons.  So - sorry, the recalc IS needed every time.  But this is open to reevaluation once we
        // get to a point where the app is being load-tested.  Max number of icons per month would be 31 x 5 = 155.
        // Another point of streamlining could be to separate the 'drawing' of the month from icon overlays, day
        // highlighting and choice labeling, calling each one more discretely only as needed.
        activeDayCanvas.reset(); // Turn off any previous highlighting.
        choice = theChoice;
        visibleYear = choice.getYear();
        hasDataArray = AppUtil.findDataDays(visibleYear);
        visibleMonth = theChoice.getMonthValue() - 1; // Adjusted (for now) to the old zero-based value.
        displayedMonth = theChoice;
        monthCanvas.recalc(); // only way to find the day object
    } // end setChoice

    private void setLabelBounds() {
        if (getSize().width == 0) return;
        // No need to show the label if the MonthCanvas has not yet
        //   been shown, especially since it will just be a flash on
        //   the screen, and in the wrong location as well.

        int width = choiceLabel.getPreferredSize().width;
        int height = choiceLabel.getPreferredSize().height;
        int x = getSize().width - width - borderWidth;
        int y = getSize().height - height - borderWidth * 2;
        choiceLabel.setBounds(x, y, width, height);
    } // end setLabelBounds

    void setParent(AppTreePanel atp) {
        parent = atp;
    }

    //--------------------------------------------------
    // Additional classes (same file but not inner)
    //--------------------------------------------------

    class MonthCanvas extends JPanel {
        private static final long serialVersionUID = 1L;

        JLabel monthLabel;

        MonthCanvas() { // constructor
            super(new BorderLayout());

            MouseAdapter ma = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    LabelButton source = (LabelButton) e.getSource();
                    String buttonText = source.getText();

                    activeDayCanvas.reset();     // turn off current choice highlight

                    if (buttonText.equals("-")) displayedMonth = displayedMonth.minusMonths(1);
                    if (buttonText.equals("+")) displayedMonth = displayedMonth.plusMonths(1);

                    visibleMonth = displayedMonth.getMonthValue() - 1;
                    if (displayedMonth.getYear() != visibleYear) {
                        visibleYear = displayedMonth.getYear();
                        hasDataArray = AppUtil.findDataDays(visibleYear);
                    } // end if
                    monthCanvas.recalc();
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

            // Create the month label (no text, yet).
            monthLabel = new JLabel();
            monthLabel.setHorizontalAlignment(JLabel.CENTER);
            monthLabel.setForeground(Color.white);
            monthLabel.setFont(Font.decode("Serif-bold-20"));

            JPanel head1 = new JPanel(new BorderLayout());
            head1.setBackground(Color.blue);
            head1.add(p0, "West");
            head1.add(monthLabel, "Center");
            head1.add(new Spacer(56, 1), "East");

            JPanel p1 = new JPanel();
            p1.setLayout(new BorderLayout());
            p1.setBackground(Color.black);
            p1.add(head1, "North");

            // Add the day of the week labels.
            JPanel head2 = new JPanel();
            JLabel l;
            head2.setLayout(new GridLayout(1, dayNames.length));
            head2.setBackground(Color.gray);
            for (String dayName : dayNames) {
                l = new JLabel(dayName, JLabel.CENTER);
                l.addMouseListener(new MouseAdapter() {
                    public void mousePressed(MouseEvent e) {
                        choice = activeDayCanvas.myDate;  // correct?   10/1/19
                        if (parent == null) return;
                        if (e.getClickCount() == 2) parent.showWeek();
                    } // end mousePressed
                });//end addMouseListener
                l.setForeground(Color.white);
                l.setFont(Font.decode("Dialog-bold-12"));
                l.setBackground(Color.gray);
                l.setOpaque(true);
                head2.add(l);
            } // end for i
            p1.add(head2, "South");
            add(p1, BorderLayout.NORTH);
            add(monthGrid, BorderLayout.CENTER);
        } // end constructor

        public void recalc() {
            DayCanvas tempDayCanvas;
            String labelText;

            // Generate new title with month and year.
            labelText = monthNames[visibleMonth] + " " + visibleYear;
            monthLabel.setText(labelText);

            // Get a temp date that we can advance a day at a time,
            //  both as a source for the DayCanvases below and for a rollover test.
            LocalDate tmpDate = displayedMonth; // This gets us to the right year and month.
            MemoryBank.debug("Initial calendar: " + dtf.format(displayedMonth));

            // Get the day of the week of the first day.
            tmpDate = tmpDate.withDayOfMonth(1);
            int dayOfWeek = getDayOfWeekInt(tmpDate); // my values; theirs suck.

            boolean rollover = false;

            // Cycle thru the days.  (allow for preceeding blanks in 'top' week)
            for (int i = 1; i <= 37; i++) {

                tempDayCanvas = (DayCanvas) monthGrid.getComponent(i - 1);
                tempDayCanvas.clear(); //clear first, then override if necess.

                // 'blank' days in the first week, before the 1st.
                if (i < dayOfWeek) {
                    tempDayCanvas.bottomLine();
                    if (i == (dayOfWeek - 1)) tempDayCanvas.rightLine();
                    continue;
                } // end if

                if (rollover) continue; // 'blank' days in the last week (or two).

                // leave the declaration inside the loop; we want a new instance
                //   each time because each 'day' gets its own in setCal().
                LocalDate thisDay = tmpDate;
//                GregorianCalendar c = (GregorianCalendar) tmpCal.clone();

                // Month rollover test - if true, then this 'c' is the last day
                //    of this month.
                tmpDate = tmpDate.plusDays(1);
//                tmpCal.add(Calendar.DATE, 1);
                if (visibleMonth != tmpDate.getMonthValue() - 1) rollover = true;
//                if (visibleMonth != tmpCal.get(Calendar.MONTH)) rollover = true;

                tempDayCanvas.setDate(thisDay);
                tempDayCanvas.addMouseListener(tempDayCanvas);
                //---------------------------------------------------

                // Highlight the current choice
                if (choice != null) {
                    // This section is looking for 'choice' in this month - if found,
                    //  it will be highlighted.
                    // For this to be true, we would have clicked the +/- buttons
                    //  to get back to this month, after having gone away.
                    if (choice.getYear() == thisDay.getYear() &&
                            choice.getMonthValue() == thisDay.getMonthValue() &&
                            choice.getDayOfMonth() == thisDay.getDayOfMonth()) {
                        activeDayCanvas = tempDayCanvas;
                        activeDayCanvas.highlight();
                    } // end if there is a choice
                } // end if - if choice not set
            } // end for i
        } // end recalc
    } // end class MonthCanvas

    // This is my own conversion, to numbers that matched these
    // that were being returned by Calendar queries, now deprecated.
    // Put this in place as a temporary remediation along the way
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

    //============================================================
    // Description:  Representation of a Day in a month 'view'
    //============================================================
    public class DayCanvas extends JPanel implements MouseListener {
        private static final long serialVersionUID = 1L;

        private JLabel dayLabel;
        private AppImage icon1 = new AppImage();
        private AppImage icon2 = new AppImage();
        private AppImage icon3 = new AppImage();
        private AppImage icon4 = new AppImage();
        private AppImage icon5 = new AppImage();
        private Color offColor;
        private Font offFont;

        private Spacer ssV, ssH;
        private JPanel dayGrid;
        private LocalDate myDate;
        //private GregorianCalendar cal;

        DayCanvas() {
            super(new BorderLayout());

            ssH = new Spacer(350, 2);
            ssV = new Spacer(2, 350);
            dayLabel = new JLabel("", JLabel.CENTER);
            dayLabel.setFont(Font.decode("Dialog-bold-16"));

            dayGrid = new JPanel();

            dayGrid.setLayout(new DayCanvasLayout());
            dayGrid.add(dayLabel);
            dayGrid.add(icon1);
            dayGrid.add(icon2);
            dayGrid.add(icon3);
            dayGrid.add(icon4);
            dayGrid.add(icon5);

            add(dayGrid, BorderLayout.CENTER);
            add(ssH, BorderLayout.SOUTH);
            add(ssV, BorderLayout.EAST);

            if (activeDayCanvas == null) activeDayCanvas = this;
        } // end constructor

        public void clear() {
            ssH.resetColor();
            ssV.resetColor();
            dayGrid.setVisible(false);
            removeMouseListener(this);
            setBorder(null);
        } // end clear


        public void highlight() {
            dayLabel.setForeground(Color.red);
            dayLabel.setFont(Font.decode("Dialog-bold-24"));

            choiceLabel.setText(dtf.format(choice) + " ");
//            choiceLabel.setText(sdf.format(choice) + " ");
            MonthView.this.setLabelBounds(); // adjust the label.
        } // end highlight


        public void reset() {
            dayLabel.setFont(offFont);
            dayLabel.setForeground(offColor);
        } // end reset

        public void setDate(LocalDate ld) {
            myDate = ld;
            update(myDate);
            dayGrid.setVisible(true);
            bottomLine();
            rightLine();
        }

//        public void setCal(GregorianCalendar cal) {
//            this.cal = cal;
//            update(cal);
//            dayGrid.setVisible(true);
//            bottomLine();
//            rightLine();
//        } // end setCal


        void bottomLine() {
            ssH.setColor(Color.black);
        } // end bottomLine

        void rightLine() {
            ssV.setColor(Color.black);
        } // end rightLine


        //---------------------------------------------------------
        // MouseListener methods
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
        }

        public void mouseEntered(MouseEvent e) {
        }

        public void mouseExited(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            choice = myDate;
//            choice = cal.getTime();
            activeDayCanvas.reset();
            highlight();
            activeDayCanvas = this;
            if (parent == null) return;
            if (e.getClickCount() == 2) parent.showDay();
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
        }
        //---------------------------------------------------------

        public void update(LocalDate ld) {
            int thisDay = ld.getDayOfMonth();
            dayLabel.setText(String.valueOf(thisDay));
            // System.out.println("DayCanvas update was called " +
            //     dayLabel.getText());

            icon1.setImage(null);
            icon2.setImage(null);
            icon3.setImage(null);
            icon4.setImage(null);
            icon5.setImage(null);

            if (hasDataArray[visibleMonth][thisDay - 1]) {
                offFont = hasDataFont;
                offColor = hasDataColor;
                Image[] thisDayIcons =
                        getIconArray(visibleYear, visibleMonth + 1, thisDay);
                if (thisDayIcons != null) {
                    icon1.setImage(thisDayIcons[0]);
                    icon2.setImage(thisDayIcons[1]);
                    icon3.setImage(thisDayIcons[2]);
                    icon4.setImage(thisDayIcons[3]);
                    icon5.setImage(thisDayIcons[4]);
                } // end if
            } else {
                offFont = noDataFont;
                offColor = noDataColor;
            } // end if
            dayLabel.setFont(offFont);
            dayLabel.setForeground(offColor);

        } // end update

//        public void update(Calendar cal) {
//            int thisDay = cal.get(Calendar.DAY_OF_MONTH);
//            dayLabel.setText(String.valueOf(thisDay));
//            // System.out.println("DayCanvas update was called " +
//            //     dayLabel.getText());
//
//            icon1.setImage(null);
//            icon2.setImage(null);
//            icon3.setImage(null);
//            icon4.setImage(null);
//            icon5.setImage(null);
//
//            if (hasDataArray[visibleMonth][thisDay - 1]) {
//                offFont = hasDataFont;
//                offColor = hasDataColor;
//                Image[] thisDayIcons =
//                        getIconArray(visibleYear, visibleMonth, thisDay);
//                if (thisDayIcons != null) {
//                    icon1.setImage(thisDayIcons[0]);
//                    icon2.setImage(thisDayIcons[1]);
//                    icon3.setImage(thisDayIcons[2]);
//                    icon4.setImage(thisDayIcons[3]);
//                    icon5.setImage(thisDayIcons[4]);
//                } // end if
//            } else {
//                offFont = noDataFont;
//                offColor = noDataColor;
//            } // end if
//            dayLabel.setFont(offFont);
//            dayLabel.setForeground(offColor);
//
//        } // end update
    } // end class DayCanvas


    static final class DayCanvasLayout extends GridLayout {
        private static final long serialVersionUID = 1L;
        private static final int standardIconSize = 32; // Actually, 32x32

        int columns, rows;
        int oneWidth, oneHeight;
        int x, y;
        Component comp;

        @SuppressWarnings("IntegerDivisionInFloatingPointContext")
        public void layoutContainer(Container parent) {
            super.layoutContainer(parent);

            synchronized (parent.getTreeLock()) {
                Dimension d = parent.getSize();
                if (d.width < (int) (.8 * standardIconSize)) return;
                int siblings = parent.getComponentCount();
                if (siblings < 6) return;

                // We'll start by trying for a 2 row, 3 column area.  If we
                //   don't have that much to work with, it could go to 1x1.
                rows = 2;
                columns = 3;
                if ((d.width / columns) < (standardIconSize * .8)) columns--;
                if ((d.width / columns) < (standardIconSize * .8)) columns--;
                if ((d.height / rows) < (standardIconSize * .8)) rows--;

                oneWidth = d.width / columns;
                oneHeight = d.height / rows;

                // Size and place the components.
                // We don't need a loop here - there are 6 and always 6.
                // If you're looking here again and having trouble following
                //   the logic, try drawing out the 'truth table'
                //   for all the possibilities.

                // The numeric text.
                x = 0;
                y = 0;
                comp = parent.getComponent(0);
                if (comp.isVisible()) {
                    comp.setBounds(x, y, oneWidth, oneHeight);
                } // end if

                // May not be room for even one more component.
                if ((columns == 1) && (rows == 1)) {
                    parent.getComponent(1).setBounds(0, 0, 0, 0);
                    parent.getComponent(2).setBounds(0, 0, 0, 0);
                    parent.getComponent(3).setBounds(0, 0, 0, 0);
                    parent.getComponent(4).setBounds(0, 0, 0, 0);
                    parent.getComponent(5).setBounds(0, 0, 0, 0);
                    return;
                } // end if

                // Calculate position of component 2
                if (columns > 1) x += oneWidth; // and y stays the same.
                else y += oneHeight; // and x stays the same.
                comp = parent.getComponent(1);
                if (comp.isVisible()) {
                    comp.setBounds(x, y, oneWidth, oneHeight);
                } // end if

                // Only continue if there's room for another..
                if ((columns + rows) < 4) {  // so we have a 3x1 or 2x2
                    parent.getComponent(2).setBounds(0, 0, 0, 0);
                    parent.getComponent(3).setBounds(0, 0, 0, 0);
                    parent.getComponent(4).setBounds(0, 0, 0, 0);
                    parent.getComponent(5).setBounds(0, 0, 0, 0);
                    return;
                } // end if

                // Calculate position of component 3
                if (columns == 3) x += oneWidth; // and y still stays the same.
                else {
                    y += oneHeight;
                    x = 0;            // and x gets a carriage-return.
                } // end if
                comp = parent.getComponent(2);
                if (comp.isVisible()) {
                    comp.setBounds(x, y, oneWidth, oneHeight);
                } // end if

                // Only continue if there's room for another..
                if ((columns == 3) && (rows == 1)) {
                    parent.getComponent(3).setBounds(0, 0, 0, 0);
                    parent.getComponent(4).setBounds(0, 0, 0, 0);
                    parent.getComponent(5).setBounds(0, 0, 0, 0);
                    return;
                } // end if

                // Calculate position of component 4
                if (columns == 3) { // We have a 2x3
                    x = 0;             // x gets a CR.
                    y += oneHeight;    // and y increments.
                } else x += oneWidth; // 2x2 and y still stays the same.
                comp = parent.getComponent(3);
                if (comp.isVisible()) {
                    comp.setBounds(x, y, oneWidth, oneHeight);
                } // end if

                // Only continue if there's room for another..
                if ((columns == 2) && (rows == 2)) {
                    parent.getComponent(4).setBounds(0, 0, 0, 0);
                    parent.getComponent(5).setBounds(0, 0, 0, 0);
                    return;
                } // end if

                // If we're here, components 5 AND 6 are coming, and
                //   their locations are fixed.

                // Component 5
                x += oneWidth;
                comp = parent.getComponent(4);
                if (comp.isVisible()) {
                    comp.setBounds(x, y, oneWidth, oneHeight);
                } // end if

                // Component 6
                x += oneWidth;
                comp = parent.getComponent(5);
                if (comp.isVisible()) {
                    comp.setBounds(x, y, oneWidth, oneHeight);
                } // end if

            } // end synchronized section
        } // end layoutContainer
    } // end class DayCanvasLayout

} // end class MonthView
