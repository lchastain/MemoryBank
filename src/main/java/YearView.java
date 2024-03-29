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

public class YearView extends JPanel {
    private static final long serialVersionUID = 1L;

    // Required for use by 'static' section -
    private static final String[] monthNames;
    private static final String[] weekdayNames;

    // Variables needed by more than one method -
    private LocalDate theChoice;    // constructor, event handling, numerous
    private LocalDate choice2;      // when 2 choices are allowed
    private DayLabel activeDayLabel;            // recalc, event handling
    private final JLabel titleLabel;       // constructor, event handling
    private final JTextField yearTextField;  // User entry of the year
    private final JLabel choiceLabel;      // constructor, highlight
    private final JPanel yearPanel;
    private int theYear;            // numerous
    static DateTimeFormatter dtf;
    private AlteredDateListener alteredDateListener = null;
    LocalDate displayedYear;  // Of course it holds more than just the Year.
    private TreePanel treePanel;
    private static final Color hasDataColor = Color.blue;
    private static final Color noDataColor = Color.black;
    private static final Font hasDataFont = Font.decode("Dialog-bold-16");
    private static final Font noDataFont = Font.decode("Dialog-plain-14");
    private boolean[][] hasDataArray;
    private JDialog dateSelectionDialog;
    private int intNumSelections = 0;
    private int intSelectionCount;
    private boolean alterButtonDepressed;
    private Depressed depressedThread;   // A Thread to keep responding to year up/down
    private LocalDate archiveDate;

    private static final int borderWidth = 2;
    private static final LineBorder theBorder;
    private final JPanel headerPanel;
    private final JPanel titlePanel;

    // Package-private accesses; may be used by tests.
    LabelButton prev;
    LabelButton todayButton;
    LabelButton next;


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
        super(new BorderLayout());
        dateSelectionDialog = null;
        choice2 = null;
        intSelectionCount = 0;

        setBorder(theBorder);

        theChoice = initial;
        displayedYear = initial;
        theYear = displayedYear.getYear();

        // This MouseAdapter allows that the buttons may be held depressed, and the indicated action
        //   will repeat until the button is released.  This is an acknowledgement that the desired
        //   year may be well away from the one currently on display.
        MouseAdapter alterButtonHandler = new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(alteredDateListener != null) {
                    alteredDateListener.dateChanged(DateRelatedDisplayType.YEAR_VIEW, displayedYear);
                }
            }
            public void mouseExited(MouseEvent e) {
                if (alterButtonDepressed) depressedThread.stopit();
                alterButtonDepressed = false;
            } // end mouseExited

            public void mousePressed(MouseEvent e) {
                LabelButton source = (LabelButton) e.getSource();
                String s = source.getName();
                int direction = 1;
                if (s.equals("-")) direction = -1;

                alterButtonDepressed = true;

                // Since a Thread will die upon return from the run method,
                //  need to start a new one each time.
                depressedThread = new Depressed(direction);
                depressedThread.start();
            } // end mousePressed

            public void mouseReleased(MouseEvent e) {
                if (alterButtonDepressed) depressedThread.stopit();
                alterButtonDepressed = false;
                displayedYear = displayedYear.plusYears(theYear - displayedYear.getYear());
            } // end mouseReleased
        };// end of new MouseAdapter

        prev = makeAlterButton("-");
        prev.setIcon(LabelButton.leftIcon);
        prev.setText(null); // We don't want both text and icon.  The original text is preserved in the 'name'.
        prev.addMouseListener(alterButtonHandler);

        todayButton = makeAlterButton("T");
        // Only the 'T' (today) button gets this handling; the others will have the alterButtonHandler.
        MouseAdapter mouseAdapter = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                LabelButton source = (LabelButton) e.getSource();
                if (!source.isEnabled()) return; // It's not a JButton, so we need to check this first.

                LocalDate theYearDate = Objects.requireNonNullElseGet(archiveDate, LocalDate::now);
                if (intNumSelections > 0) {
                    // This is both a view-change and a selection.
                    setChoice(theYearDate); // This does a setView, which does the recalc.
                } else {
                    // This is a view-change only.
                    theYear = Objects.requireNonNullElseGet(archiveDate, LocalDate::now).getYear();
                    recalc(theYearDate);
                }
                if(alteredDateListener != null) {
                    alteredDateListener.dateChanged(DateRelatedDisplayType.YEAR_VIEW, theYearDate);
                }
            }
        };
        todayButton.addMouseListener(mouseAdapter);

        next = makeAlterButton("+");
        next.setIcon(LabelButton.rightIcon);
        next.setText(null); // We don't want both text and icon.  The original text is preserved in the 'name'.
        next.addMouseListener(alterButtonHandler);

        JPanel alterButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        alterButtonsPanel.add(prev);
        alterButtonsPanel.add(todayButton);
        alterButtonsPanel.add(next);

        titleLabel = new JLabel("Year " + theYear);
        titleLabel.setFont(Font.decode("Serif-bold-20"));
        titleLabel.setHorizontalAlignment(JLabel.CENTER);
        titleLabel.setBackground(Color.lightGray);
        titleLabel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                super.mouseClicked(e);
                headerPanel.remove(titleLabel);
                yearTextField.setText(String.valueOf(theYear)); // Needed after re-editing bad user input
                headerPanel.add(titlePanel, BorderLayout.CENTER);
                headerPanel.revalidate();
                headerPanel.repaint();
                yearTextField.requestFocus();
            }
        });
        yearTextField = new JTextField(String.valueOf(theYear), 4);
        yearTextField.setTransferHandler(null); // disables paste actions.
        yearTextField.setFont(Font.decode("Serif-bold-18"));
        yearTextField.setHorizontalAlignment(SwingConstants.CENTER);
        yearTextField.setPreferredSize(new Dimension(25, 22));
        yearTextField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent evt) {
                super.keyTyped(evt);
                char theChar = evt.getKeyChar();

                if (theChar == KeyEvent.VK_ENTER) {
                    String theEntry = yearTextField.getText();
                    if (!theEntry.isEmpty()) {
                        theYear = Integer.parseInt(yearTextField.getText());
                        if (theYear <= 0) theYear = 1; // Entry limited to 4 digits, but they can still be flaky.

                        int yearDiff = theYear - displayedYear.getYear();
                        // We use 'plusYears' to keep it 'legal', vs simply constructing a LocalDate from current day
                        //   and month and glommed onto the provided year.  This really only prevents one bad date -
                        //   going from Feb 29 on a leap year, to a non-leap year.  But what the heck, why not.
                        displayedYear = displayedYear.plusYears(yearDiff);

                        recalc(theYear);
                    }
                    transferFocusUpCycle(); // Otherwise it holds on, and key mappings don't work no mo.
                    headerPanel.remove(titlePanel);
                    headerPanel.add(titleLabel, BorderLayout.CENTER);
                    headerPanel.revalidate();
                    headerPanel.repaint();
                    evt.consume();
                }

                // Disallow non-numerics
                if (theChar < '0') evt.consume();
                if (theChar > '9') evt.consume();

                // Allow highlighted digits to be replaced
                int sStart = yearTextField.getSelectionStart();
                int sEnd = yearTextField.getSelectionEnd();
                if (sEnd - sStart > 0) return;

                // Allow up to 4 digits
                if (yearTextField.getText().length() >= 4) evt.consume();
            }
        });
        JLabel label4year = new JLabel("Year:");
        label4year.setFont(Font.decode("Serif-bold-20"));
        titlePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 0));
        titlePanel.add(label4year);
        titlePanel.add(yearTextField);

        headerPanel = new JPanel(new BorderLayout());
        headerPanel.add(alterButtonsPanel, BorderLayout.WEST);
        headerPanel.add(titleLabel, BorderLayout.CENTER);
        add(headerPanel, BorderLayout.NORTH);

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
        //   take the center and thereby expand/contract as needed.
        bottomPanel.add(choiceLabel, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        treePanel = AppTreePanel.theInstance;
        recalc(theYear);

        // Add key bindings to react to arrow keys
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_UP, 0), "upYear");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_RIGHT, 0), "upYear");
        getActionMap().put("upYear", new UpAction("upYear"));
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, 0), "downYear");
        getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, 0), "downYear");
        getActionMap().put("downYear", new DownAction("downYear"));

    } // end constructor


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

    // Used by tests
    String getChoiceLabelText() {
        return choiceLabel.getText().trim();
    }

    // This is my own conversion, to numbers that matched these
    // that were being returned by Calendar queries, now deprecated.
    // I put this in place as a temporary remediation along the way
    // to updating the app to new Java 8 date/time classes.
    private int getDayOfWeekInt(LocalDate tmpDate) {
        return switch (tmpDate.getDayOfWeek()) {
            case SUNDAY -> 1;
            case MONDAY -> 2;
            case TUESDAY -> 3;
            case WEDNESDAY -> 4;
            case THURSDAY -> 5;
            case FRIDAY -> 6;
            case SATURDAY -> 7;
        };
    }

    LabelButton makeAlterButton(String theText) {
        LabelButton theButton = new LabelButton(theText);
        theButton.setPreferredSize(new Dimension(28, 28));
        theButton.setFont(Font.decode("Dialog-bold-14"));
        return theButton;
    }

    private void recalc(LocalDate theDate) {
        recalc(theDate.getYear());
    }

    private void recalc(int year) {
        // Update the Year info
        titleLabel.setText("Year " + theYear);
        yearTextField.setText(String.valueOf(theYear));
        if(alteredDateListener != null) {
            alteredDateListener.dateChanged(DateRelatedDisplayType.YEAR_VIEW, displayedYear);
        }

        // Look for new day data, for color/font setting.
        hasDataArray = MemoryBank.dataAccessor.findDataDays(year);

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
        todayButton.setEnabled(!(year == Objects.requireNonNullElseGet(archiveDate, LocalDate::now).getYear()));
    } // end recalc


    void setArchiveDate(LocalDate theArchiveDate) {
        archiveDate = theArchiveDate;
        setChoice(theArchiveDate); // To get the right choiceLabel
        setView(theArchiveDate); // To show the right Year
    }


    public void setChoice(LocalDate theNewChoice) {
        if (activeDayLabel != null) activeDayLabel.reset(); // turn off any previous selection.
        // It is more efficient to do the above de-highlighting here rather than in the MonthCanvas recalc
        // because that recalc will happen for all 12 months of any given year, not just the one
        // containing the previous active day.

        // When this panel is used as a dialog, a null setting of the choice is allowed.
        theChoice = theNewChoice;
        if (theChoice == null) {  // Only happens when this is a dialog.
            intSelectionCount = 0;   // Used to control dialog visibility
        } else { // Normal usage, AND dialog usage.
            intSelectionCount = 1;   // Used to control dialog visibility
            setView(theChoice);
        }
    } // end setChoice


    public void setDialog(JDialog jd, int numSelections) {
        dateSelectionDialog = jd;
        intNumSelections = numSelections;
    } // end setDialog

    // Since the TreePanel is also the AlteredDateListener, this 'set' method does double-duty.
    void setTreePanel(TreePanel atp) {
        treePanel = atp;
        alteredDateListener = (AlteredDateListener) atp;
    }

    void setView(LocalDate viewDate) {
        displayedYear = viewDate;
        if (activeDayLabel != null) activeDayLabel.reset(); // turn off any previous selection.
        theYear = viewDate.getYear();
        recalc(viewDate);
    } // end setView


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
            tempLabel.addMouseListener(new MouseAdapter() { // Respond to a click on the Month name.
                public void mousePressed(MouseEvent e) {
                    if (treePanel == null) return;
                    myDate = LocalDate.of(theYear, monthLocalDate.getMonth().getValue(), treePanel.getViewedDate().getDayOfMonth());
                    if (archiveDate != null && myDate.isAfter(archiveDate)) myDate = archiveDate;
                    alteredDateListener.dateChanged(DateRelatedDisplayType.YEAR_VIEW, myDate);
                    treePanel.showMonthView();
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
                tempLabel.addMouseListener(new MouseAdapter() { // Respond to a click on the Weekdays line
                    public void mousePressed(MouseEvent e) {
                        if (treePanel == null) return;
                        myDate = LocalDate.of(theYear, monthLocalDate.getMonth().getValue(), 1);
                        // More needed here if/when a week view is ever provided; similar to the section above, for Month.
                        treePanel.showWeek(myDate);
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

            // Cycle thru all the slots (potential days) in our month layout.
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

                // Get the component (a DayLabel) for the layout slot currently under consideration
                tmpDayLabel = (DayLabel) monthMatrix.getComponent(i - 1);

                // Associate a specific Date with the DayLabel in the current layout slot
                tmpDayLabel.setDay(tmpLocalDate);

                // If this Date is the current choice then highlight it.
                if (null != theChoice && theChoice.isEqual(tmpLocalDate)) {
                    // If the choice is this day -
                    activeDayLabel = tmpDayLabel;
                    activeDayLabel.highlight();
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
            setFont(Font.decode("Dialog-bold-16")); // Same as hasData, but 18 is too big for Metal L&F ...
            // ... and that may only be on a Mac.  'too big == digit changes to '...' due to its bounds.
            choiceLabel.setText(dtf.format(theChoice) + " ");
        } // end highlight

        public void reset() {
            setFont(offFont);
            setForeground(offColor);
            choiceLabel.setText(" ");
        } // end reset

        public void setDay(LocalDate aLocalDate) {
            myDate = aLocalDate;
            day = myDate.getDayOfMonth();
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
            int m = e.getModifiersEx();
            if ((m & InputEvent.BUTTON3_DOWN_MASK) != 0) rightClick = true;

            if (rightClick) {
                // Turn 'off' this DayLabel if it is right-clicked.
                if (activeDayLabel == this) {
                    activeDayLabel.reset();
                    theChoice = null;
                    if (intSelectionCount > 0) intSelectionCount--;
                } // end if
            } else {
                // Both theChoice and choice2 are set to the same value each time so this might look confusing.
                // But this only happens when a calling context is using this panel as a date-range selection
                // UI and in that case it will show the dialog twice.  That calling context is responsible for
                // preserving the first (theChoice) date in a separate variable, before showing the dialog the
                // second time in order to set and retrieve choice2, so separation of the values is done there,
                // not here.
                choice2 = theChoice;  // TODO verify this is working - date ranges are set by SearchPanel
                theChoice = myDate;
                intSelectionCount++;
                // System.out.println("Choice is: " + choice);

                // A left-click on ANY DayLabel will turn off the previous one.
                activeDayLabel.reset();

                activeDayLabel = this;
                activeDayLabel.highlight();

                // If the YearView is acting as a date selection dialog -
                if (dateSelectionDialog != null) {
                    if (intSelectionCount >= intNumSelections) {
                        System.out.println("intSelectionCount = " + intSelectionCount);
                        dateSelectionDialog.setVisible(false);
                    }
                    return;
                } // end if this is a dialog

                if (treePanel == null) return;
                if (e.getClickCount() == 2) {
                    if (archiveDate != null && theChoice.isAfter(archiveDate)) {
                        theChoice = archiveDate;
                    }
                    if(alteredDateListener != null) {
                        alteredDateListener.dateChanged(DateRelatedDisplayType.YEAR_VIEW, theChoice);
                    }
                    treePanel.showDay();
                }
            } // end if/else
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
        }

    } // end DayLabel

    class Depressed extends Thread {
        private int delay = 300; // milliseconds
        private boolean iAmRunning;
        private final int direction;

        Depressed(int direction) {
            this.direction = direction;
            iAmRunning = true;
        } // end constructor

        // A thread 'dies' upon return from run.
        @SuppressWarnings("BusyWait")
        public void run() {
            while (iAmRunning) {

                theYear += direction;
                if (theYear > 9999) theYear = 1;
                if (theYear < 1) theYear = 9999;
                recalc(theYear);

                try {
                    sleep(delay);  // milliseconds
                } catch (InterruptedException ignored) {
                }
                if (delay > 50) delay -= 50;
                // else delay = 0;  // This lets it go tooo fast.
            } // end while
        } // end run

        void stopit() {
            iAmRunning = false;
        }
    } // end class Depressed

    public class UpAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public UpAction(String name) {
            super(name);
            putValue(SHORT_DESCRIPTION, "Increase the year");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            theYear += 1;
            if (theYear > 9999) theYear = 1;
            recalc(theYear);
        }
    }

    public class DownAction extends AbstractAction {
        private static final long serialVersionUID = 1L;

        public DownAction(String name) {
            super(name);
            putValue(SHORT_DESCRIPTION, "Decrease the year");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            theYear -= 1;
            if (theYear < 1) theYear = 9999;
            recalc(theYear);
        }
    }

} // end class YearView

