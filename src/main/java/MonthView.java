/* User interface to choose a Date from a view of a Month.
 */

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.text.WordUtils;

import javax.swing.*;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
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
    private static String[] dayNames;
    private static JPanel monthGrid;
    static DateTimeFormatter dtf;
    private static LocalDate theChoice;
    private static Color hasDataColor = Color.blue;
    private static Color noDataColor = Color.black;
    private static Font hasDataFont = Font.decode("Dialog-bold-20");
    private static Font noDataFont = Font.decode("Dialog-bold-16");
    static LocalDate displayedMonth;  // Of course it also holds a year and date

    // Variables needed by more than one method -
    private DayCanvas activeDayCanvas;   // recalc, event handling
    private MonthCanvas monthCanvas;
    private JLabel choiceLabel;
    private int heightOffset = 0;
    private AppTreePanel appTreePanel = null;
    private boolean[][] hasDataArray;  // for a year.  index 0 = month, index 1 = days, values have data True or False
    private Dimension minSize;

    private static final int borderWidth = 2;
    private static LineBorder theBorder;

    static {
        theBorder = new LineBorder(Color.black, borderWidth);

        // Initialize day of week names.
        dayNames = new String[]{"Sunday", "Monday", "Tuesday", "Wednesday",
                "Thursday", "Friday", "Saturday"};

        // Create a grid for the days (6 rows, 7 columns).
        monthGrid = new JPanel(new GridLayout(6, dayNames.length));

        dtf = DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy");
        theChoice = null;
    } // end static

    MonthView() {
        this(LocalDate.now());
    }

    MonthView(LocalDate initialChoice) {
        super();
        displayedMonth = initialChoice;
//        setChoice(initialChoice); // We need to do it this way, so we also get the right 'choice' label.
        theChoice = initialChoice;

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

        monthCanvas = new MonthCanvas();
        monthCanvas.setBorder(theBorder);

        choiceLabel = new JLabel();
        choiceLabel.setFont(Font.decode("Dialog-bold-18"));
        choiceLabel.setForeground(Color.red);
        setChoiceLabel();

        // Respond to a click on the choiceLabel, to set the view back to the
        // month of the selected date (if/when the view is on a different month)
        MouseAdapter ma = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                setView(theChoice);
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

        setView(initialChoice); // The choice will not always be the displayedMonth.
        MonthView.this.setLabelBounds(); // adjust the label.
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

        String theFilename = NoteGroupFile.foundFilename(ld, "D");
        if (!new File(theFilename).exists()) return null;

        MemoryBank.debug("Loading: " + theFilename);
        Image[] returnArray = new Image[5];

        int index = 0;
        String iconFileString;

        Object[] theDayGroup = NoteGroupFile.loadFileData(theFilename);
        BaseData.loading = true; // We don't want to affect the lastModDates!

        // relatively new 'adjustment', to adapt to addition of properties to data files
        // and the possibility that a properties is all that the file contains.
        Object theObject = theDayGroup[theDayGroup.length-1];
        String theClass = theObject.getClass().getName();
        System.out.println("The DayGroup class type is: " + theClass);
        if(!theClass.equals("java.util.ArrayList")) return null;

        Vector<DayNoteData> theDayNotes = AppUtil.mapper.convertValue(theObject, new TypeReference<Vector<DayNoteData>>() { });
        BaseData.loading = false; // Restore normal lastModDate updating.

        for (DayNoteData tempDayData : theDayNotes) {
            if (tempDayData.getShowIconOnMonthBoolean()) {
                iconFileString = tempDayData.getIconFileString();
                if (iconFileString == null) { // Then show the default icon
                    iconFileString = DayNoteGroupPanel.dayNoteDefaults.defaultIconFileName;
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
        return theChoice;
    }

    // Used by tests
    String getChoiceLabelText() {
        return choiceLabel.getText().trim();
    }

    public void setChoice(LocalDate theNewChoice) {
        // Was tempted (for better performance) to avoid the recalc, if the new choice was still on the same month
        // as the previous choice, but that doesn't work here - while 'away', new notes (with icons) might have been
        // added to any given day of this month, and if we are not on the exact same day then the choice would
        // also be wrong.  If we want to first check to see if a recalc-worthy change was made, then we would need to
        // setNotes new flags in various places and that introduces more complexity to the feature, making it more fragile
        // and harder to maintain, with questionable improvement to performance when displaying any month with a lower
        // number of icons.  So - sorry, the recalc IS needed every time.  But this is open to reevaluation once we
        // get to a point where the app is being load-tested.  Max number of icons per month would be 31 x 5 = 155.
        // Another point of streamlining could be to separate the 'drawing' of the month from icon overlays, day
        // highlighting and choice labeling, calling each one more discretely only as needed.
        activeDayCanvas.reset(); // Turn off any previous highlighting.

        // Accept the new value (other than null)
        theChoice = theNewChoice;
        hasDataArray = AppUtil.findDataDays(theChoice.getYear());
        displayedMonth = theChoice;
        setChoiceLabel();

        // Highlight the selected day, IF it appears in the currently displayed month.
        monthCanvas.recalc(); // only way to find the day object
    } // end setDate

    void setChoiceLabel() {
        choiceLabel.setText(dtf.format(theChoice) + " ");
        setLabelBounds(); // adjust the label.
    }

    public void setView(LocalDate theNewMonthToView) {
        activeDayCanvas.reset(); // Turn off any previous highlighting.

        hasDataArray = AppUtil.findDataDays(theNewMonthToView.getYear());
        displayedMonth = theNewMonthToView;
        monthCanvas.recalc(); // only way to find the day object
    } // end setView

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
        appTreePanel = atp;
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
                    int currentYear = displayedMonth.getYear(); // Get this before we setNotes/subtract

                    if (buttonText.equals("-")) displayedMonth = displayedMonth.minusMonths(1);
                    if (buttonText.equals("+")) displayedMonth = displayedMonth.plusMonths(1);
                    if(appTreePanel != null) appTreePanel.setViewedDate(displayedMonth, ChronoUnit.MONTHS);

                    // If we have scrolled into a new year, we need to update the 'hasData' info.
                    if (currentYear != displayedMonth.getYear()) {
                        hasDataArray = AppUtil.findDataDays(displayedMonth.getYear());
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
                        if (appTreePanel == null) return;
                        // When WeekView is implemented, redo this similarly to how MonthView does.
                        // for now, showWeek is dateless.
                        appTreePanel.showWeek(LocalDate.now());
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
            String theMonthName, labelText;

            // Generate new title with month and year.
            theMonthName = WordUtils.capitalizeFully(displayedMonth.getMonth().toString());
            labelText = theMonthName + " " + displayedMonth.getYear();
            MemoryBank.debug("Displayed Month: " + labelText);
            monthLabel.setText(labelText);

            // Get a temp date that we can advance a day at a time,
            //  both as a source for the DayCanvases below and for a rollover test.
            LocalDate tmpLocalDate = displayedMonth; // This gets us to the right year and month.
            // Since Java Dates are immutable, we don't lose the value of displayedMonth when we
            // start adjusting the tmpLocalDate below, even though it looks like we are using a reference.

            // Get the day of the week of the first day.
            tmpLocalDate = tmpLocalDate.withDayOfMonth(1);
            int dayOfWeek = AppUtil.getDayOfWeekInt(tmpLocalDate); // my values; theirs suck.

            boolean rollover = false;

            // Cycle thru all the (possible) days in our month layout.
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

                // Label this day
                tempDayCanvas.setDate(tmpLocalDate);
                tempDayCanvas.addMouseListener(tempDayCanvas);

                // Highlight the current choice, if found in this month.
                // For this to be true, we would have clicked the +/- buttons
                //  to get back to this month, after having gone away.
                if(theChoice != null) {
                    if (theChoice.getYear() == tmpLocalDate.getYear() &&
                            theChoice.getMonthValue() == tmpLocalDate.getMonthValue() &&
                            theChoice.getDayOfMonth() == tmpLocalDate.getDayOfMonth()) {
                        activeDayCanvas = tempDayCanvas;
                        activeDayCanvas.highlight();
                    }
                } // end if there is a choice

                // Go forward in time by one day.
                tmpLocalDate = tmpLocalDate.plusDays(1);

                // If this is true then we went into next month.
                if (MonthView.displayedMonth.getMonth() != tmpLocalDate.getMonth()) rollover = true;

            } // end for i
        } // end recalc
    } // end class MonthCanvas

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
            setChoiceLabel(); // may not be needed here, if a call to it can be made from a better loc.
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
            theChoice = myDate;
            if(appTreePanel != null) {
                appTreePanel.setSelectedDate(theChoice);
            }
            activeDayCanvas.reset();
            highlight();
            activeDayCanvas = this;
            if (appTreePanel == null) return;
            if (e.getClickCount() == 2) appTreePanel.showDay();
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

            if (hasDataArray[displayedMonth.getMonthValue() - 1][thisDay - 1]) {
                offFont = hasDataFont;
                offColor = hasDataColor;
                Image[] thisDayIcons =
                        getIconArray(displayedMonth.getYear(), displayedMonth.getMonthValue(), thisDay);
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
