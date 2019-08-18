/** User interface to choose a Date from a view of a Month.
 */

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

public class MonthView extends JLayeredPane {
    private static final long serialVersionUID = -1L;
    // As a container, this class has only two items:
    //   a MonthCanvas and a JLabel.  Because it has no layout
    //   manager, the Bounds of both items must be set explicitly.

    // No need to have more than one of these, but need them to be
    //  visible to more than one method here.
    private static String[] monthNames;
    private static String[] dayNames;
    private static JPanel monthGrid;
    private static SimpleDateFormat sdf;
    private static Date choice;
    private static boolean choiceWasSet;
    private static Color hasDataColor = Color.blue;
    private static Color noDataColor = Color.black;
    private static Font hasDataFont = Font.decode("Dialog-bold-20");
    private static Font noDataFont = Font.decode("Dialog-bold-16");
    private static int visibleYear;
    private static int visibleMonth;

    // Variables needed by more than one method -
    private Date initial;           // constructor, event handling
    private DayCanvas dc;           // recalc, event handling
    private GregorianCalendar cal;
    private MonthCanvas monthCanvas;
    private JLabel choiceLabel;
    private int heightOffset = 0;
    private int initialMonth;
    private int initialYear;
    private int initialDay;
    private AppTreePanel parent;
    private boolean hasDataArray[][];
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

        sdf = new SimpleDateFormat();
        sdf.applyPattern("EEEE, MMMM d, yyyy");
        choiceWasSet = false;
        choice = null;
    } // end static


    //--------------------------------------------------------------------
    // The MonthView constructor -
    //
    // Note: construction by itself will not be enough; you will need to
    //   call 'setChoice' afterwards, prior to display.
    //--------------------------------------------------------------------
    MonthView(AppTreePanel l) {
        super();
        parent = l;
        initial = new Date();

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

        cal = (GregorianCalendar) Calendar.getInstance();
        // Note: getInstance at this time returns a Calendar that
        //   is actually a GregorianCalendar, but since the return
        //   type is Calendar, it must be cast in order to assign.

        cal.setGregorianChange(new GregorianCalendar(1752,
                Calendar.SEPTEMBER, 14).getTime());

        reset(); // sets cal to initial -
        initialYear = cal.get(Calendar.YEAR);
        initialMonth = cal.get(Calendar.MONTH);
        initialDay = cal.get(Calendar.DAY_OF_MONTH);

        visibleYear = initialYear;
        visibleMonth = initialMonth;
        hasDataArray = AppUtil.findDataDays(visibleYear);

        monthCanvas = new MonthCanvas();
        monthCanvas.setBorder(theBorder);

        choiceLabel = new JLabel();
        choiceLabel.setFont(Font.decode("Dialog-bold-18"));
        choiceLabel.setForeground(Color.red);

        add(monthCanvas, new Integer(0));
        add(choiceLabel, new Integer(1));
    } // end constructor


    //---------------------------------------------------------------------
    // Method Name: getIconArray
    //
    // Returns an array of 5 LogIcons that are read from a file
    //   of data for the specified day.  There may be one or more
    //   null placeholders in the array.
    //---------------------------------------------------------------------
    public Image[] getIconArray(int year, int month, int day) {
        Exception e = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;
        DayNoteData tempDayData;
        //noinspection MagicConstant
        MemoryBank.tempCalendar.set(year, month, day);

        String FileName = AppUtil.findFilename(MemoryBank.tempCalendar, "D");
        if (!new File(FileName).exists()) return null;

        MemoryBank.debug("Loading: " + FileName);
        Image returnArray[] = new Image[5];

        int index = 0;
        boolean doit;
        String iconFileString;

        try {
            fis = new FileInputStream(FileName);
            ois = new ObjectInputStream(fis);

            while (index < 5) {
                tempDayData = (DayNoteData) ois.readObject();
                doit = tempDayData.getShowIconOnMonthBoolean();
                if (doit) {
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
                } // end if
            } // end while
        } catch (ClassCastException cce) {
            e = cce;
        } catch (ClassNotFoundException cnfe) {
            e = cnfe;
        } catch (InvalidClassException ice) {
            e = ice;
        } catch (EOFException eofe) {
            // System.out.println("End of file reached!");
        } catch (IOException ioe) {
            e = ioe;
        } finally {
            try {
                assert ois != null;
                ois.close();
                fis.close();
            } catch (IOException ioe) {
                e = ioe;
            } // end try/catch
        } // end try/catch

        if (e != null) {
            MemoryBank.debug("Error in loading " + FileName + " !");
            return null;
        } // end if

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


    // Reset cal and choice to the initial date, sdf to cal
    public void reset() {
        cal.setTime(initial);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        sdf.setCalendar(cal);
        choice = cal.getTime();
    } // end reset


    public Date getChoice() {
        return choice;
    }


    public void setChoice(Date d) {
        dc.reset();
        cal.setTime(d);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        choice = cal.getTime();
        choiceWasSet = true;
        visibleYear = cal.get(Calendar.YEAR);
        hasDataArray = AppUtil.findDataDays(visibleYear);
        visibleMonth = cal.get(Calendar.MONTH);
        monthCanvas.recalc(); // only way to find the day object
    } // end setChoice


    public void setLabelBounds() {
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


    //--------------------------------------------------
    // Additional classes (same file but not inner)
    //--------------------------------------------------

    class MonthCanvas extends JPanel {
        private static final long serialVersionUID = -6283279801106943344L;

        JLabel monthLabel;
        int whichOf12;

        MonthCanvas() { // constructor
            super(new BorderLayout());
            whichOf12 = cal.get(Calendar.MONTH);

            MouseAdapter ma = new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    LabelButton source = (LabelButton) e.getSource();
                    String s = source.getText();

                    dc.reset();     // turn off current choice highlight

                    if (s.equals("-")) cal.add(Calendar.MONTH, -1);
                    if (s.equals("+")) cal.add(Calendar.MONTH, 1);

                    choice = null; // will be set again in recalc...

                    visibleMonth = cal.get(Calendar.MONTH);
                    if (cal.get(Calendar.YEAR) != visibleYear) {
                        visibleYear = cal.get(Calendar.YEAR);
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
//            setChoice(cal.getTime());
                        choice = cal.getTime();
                        // System.out.println("Choice is: " + choice);
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
            DayCanvas dc;
            String labelText;

            // Generate new title with month and year.
            labelText = monthNames[visibleMonth] + " " + String.valueOf(visibleYear);
            monthLabel.setText(labelText);

            // Clone the calendar so we can advance it a day at a time,
            //  both as a source for the 'c's below and for a rollover test.
            GregorianCalendar tmpCal = (GregorianCalendar) cal.clone();
            tmpCal.set(Calendar.MINUTE, 0);
            tmpCal.set(Calendar.SECOND, 0);
            MemoryBank.debug("Initial calendar: " + sdf.format(tmpCal.getTime()));

            // Get the day of the week of the first day.
            tmpCal.set(Calendar.DAY_OF_MONTH, 1);
            int dayOfWeek = tmpCal.get(Calendar.DAY_OF_WEEK);

            boolean rollover = false;

            // Cycle thru the days.  (allow for preceeding blanks in 'top' week)
            for (int i = 1; i <= 37; i++) {

                dc = (DayCanvas) monthGrid.getComponent(i - 1);
                dc.clear(); //clear first, then override if necess.

                // 'blank' days in the first week, before the 1st.
                if (i < dayOfWeek) {
                    dc.bottomLine();
                    if (i == (dayOfWeek - 1)) dc.rightLine();
                    continue;
                } // end if

                if (rollover) continue; // 'blank' days in the last week (or two).

                // leave the declaration inside the loop; we want a new instance
                //   each time because each 'day' gets its own in setCal().
                GregorianCalendar c = (GregorianCalendar) tmpCal.clone();

                // Month rollover test - if true, then this 'c' is the last day
                //    of this month.
                tmpCal.add(Calendar.DATE, 1);
                if (visibleMonth != tmpCal.get(Calendar.MONTH)) rollover = true;

                dc.setCal(c);
                dc.addMouseListener(dc);
                //---------------------------------------------------

                // System.out.println("choiceWasSet: " + choiceWasSet);

                // Highlight the current choice
                if (choice == null) {  // Take the first available day
                    choice = c.getTime();
                    MonthView.this.dc = dc;
                    dc.highlight();
                } else {  // there is a choice -
                    if (choiceWasSet) {  // Year and Month are N/A since it's been 'set'.
                        if (c.get(Calendar.DAY_OF_MONTH) == cal.get(Calendar.DAY_OF_MONTH)) {
                            MonthView.this.dc.reset();
                            MonthView.this.dc = dc;
                            dc.highlight();
                        } // end if
                    } else {
                        // This section is looking for 'today' in this month - if found,
                        //  it will be highlighted as the current selection.
                        // For this to be true, we would have clicked the +/- buttons
                        //  to get back to this month, after having gone away.
                        if (initialYear == c.get(Calendar.YEAR) &&
                                initialMonth == c.get(Calendar.MONTH) &&
                                initialDay == c.get(Calendar.DAY_OF_MONTH)) {
                            MonthView.this.dc.reset();

                            choice = c.getTime();
                            MonthView.this.dc = dc;
                            dc.highlight();
                        } // end if initial
                    } // end if choiceWasSet
                } // end if - if choice not set
            } // end for i
            choiceWasSet = false;
        } // end recalc
    } // end class MonthCanvas

    //============================================================
    // Description:  Representation of a Day in a month 'view'
    //============================================================
    public class DayCanvas extends JPanel implements MouseListener {
        private static final long serialVersionUID = 7499179306493480030L;

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
        private GregorianCalendar cal;

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

            if (dc == null) dc = this;
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

            choiceLabel.setText(sdf.format(choice) + " ");
            MonthView.this.setLabelBounds(); // adjust the label.
        } // end highlight


        public void reset() {
            dayLabel.setFont(offFont);
            dayLabel.setForeground(offColor);
        } // end reset


        public void setCal(GregorianCalendar cal) {
            this.cal = cal;
            update(cal);
            dayGrid.setVisible(true);
            bottomLine();
            rightLine();
        } // end setCal


        public void bottomLine() {
            ssH.setColor(Color.black);
        } // end bottomLine


        public void rightLine() {
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
            choice = cal.getTime();
            dc.reset();
            highlight();
            dc = this;
            if (parent == null) return;
            if (e.getClickCount() == 2) parent.showDay();
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
        }
        //---------------------------------------------------------


        public void update(Calendar cal) {
            int thisDay = cal.get(Calendar.DAY_OF_MONTH);
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
                Image thisDayIcons[] =
                        getIconArray(visibleYear, visibleMonth, thisDay);
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


    final class DayCanvasLayout extends GridLayout {
        private static final long serialVersionUID = 2527300679990907663L;

        private static final int standardIconSize = 32; // Actually, 32x32

        int columns, rows;
        int oneWidth, oneHeight;
        int x, y;
        Component comp;

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


    // Test method for the class -
    public static void main(String[] args) {
        JFrame f = new JFrame("Month View Test");
        f.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent we) {
                System.exit(0);
            }
        });

        MemoryBank.debug = true;

        MonthView mv = new MonthView(null);
        mv.setChoice(new Date());
        f.getContentPane().add(mv, "Center");
        f.pack();
        f.setVisible(true);

        System.out.println("Month View size: " + f.getSize());
    } // end main

} // end class MonthView
