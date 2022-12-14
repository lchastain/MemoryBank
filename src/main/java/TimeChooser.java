//
// Description:  User interface to select a time.
//                                                                       
// Modification History:
// ---------------------
//  7/10/2005 Changed all 'Global' to 'MemoryBank'.
// 10/05/2004 Rewrote isFocusTraversable calls for jdk1.5.0

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Calendar;

public class TimeChooser extends JPanel implements ActionListener {
    private static final long serialVersionUID = 1L;

    private static Border up;
    private static Border down;
    private static TimeAlterButton hoursButton;
    static TimeAlterButton minutesButton;
    private static TimeAlterButton secondsButton;
    private static Font buttonFont;

    // Variables needed by more than one method -
    private LocalTime initialTime;
    private LocalTime theNewTime;

    private boolean showSeconds;
    private boolean iAmClear;
    JButton ampmButton;
    JButton nowButton;
    JButton clearButton;
    JButton resetButton;

    static {
        up = new BevelBorder(BevelBorder.RAISED);
        down = new BevelBorder(BevelBorder.LOWERED);
        buttonFont = Font.decode("Dialog-Bold-14");
    } // end static section

    TimeChooser() {
        this(LocalTime.now());
    } // end constructor

    TimeChooser(LocalTime initialTime) {
        super(new BorderLayout());
        this.initialTime = initialTime;
        showSeconds = false;
        iAmClear = false;

        theNewTime = this.initialTime; // The same reference, to start.

        ampmButton = new JButton("AM") {
            private static final long serialVersionUID = 1L;

            public Insets getMargin() {
                return new Insets(2, 2, 2, 2);
            } // end getMargin

            public Dimension getPreferredSize() {
                Dimension d = super.getPreferredSize();
                d.width = 50;
                return d;
            } // end getPreferredSize
        };
        ampmButton.setFocusable(false);
        ampmButton.setFont(Font.decode("Dialog-Bold-18"));
        ampmButton.addActionListener(TimeChooser.this);
        ampmButton.setToolTipText("Toggles AM or PM");

        // constructor, actionPerformed
        nowButton = new JButton("Now");
        nowButton.setMargin(new Insets(2, 2, 2, 2));
        nowButton.setFocusable(false);
        nowButton.setFont(buttonFont);
        nowButton.addActionListener(TimeChooser.this);
        nowButton.setToolTipText("Set to current time");

        // constructor, actionPerformed
        clearButton = new JButton(" ");
        clearButton.setFocusable(false);
        clearButton.setFont(buttonFont);
        clearButton.addActionListener(TimeChooser.this);
        clearButton.setToolTipText("Clear the time fields");

        // constructor, actionPerformed
        resetButton = new JButton("Reset");
        resetButton.setMargin(new Insets(2, 2, 2, 2));
        resetButton.setFocusable(false);
        resetButton.setFont(buttonFont);
        resetButton.addActionListener(TimeChooser.this);
        resetButton.setToolTipText("Reset to the initial time");

        hoursButton = new TimeAlterButton(ChronoUnit.HOURS);
        minutesButton = new TimeAlterButton(ChronoUnit.MINUTES);
        secondsButton = new TimeAlterButton(ChronoUnit.SECONDS);

        JPanel northPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));

        northPanel.add(hoursButton);
        northPanel.add(minutesButton);
        northPanel.add(secondsButton);
        northPanel.add(new Spacer(5, 1));
        northPanel.add(ampmButton);
        southPanel.add(nowButton);
        southPanel.add(clearButton);
        southPanel.add(resetButton);
        add(northPanel, "North");
        add(southPanel, "South");

        recalc();
    } // end constructor

    public void actionPerformed(ActionEvent e) {
        JButton source = (JButton) e.getSource();

        switch (source.getActionCommand()) {
            case "AM":
            case "PM":
                theNewTime = theNewTime.plusHours(12);
                break;
            case "Now":
                theNewTime = LocalTime.now();
                iAmClear = false;
                break;
            case "Reset":
                theNewTime = initialTime;
                iAmClear = false;
                break;
            case " ":
                iAmClear = true;
                hoursButton.setText("  ");
                minutesButton.setText("  ");
                secondsButton.setText("  ");
                ampmButton.setText("  ");
                break;
        }
        recalc();
    } // end actionPerformed

    boolean getClearBoolean() {
        return iAmClear;
    }

    // Returns the selection, without the 'noise' of additional
    // small trailing time bits.
    LocalTime getChoice() {
        if(iAmClear) return null;

        if(showSeconds) {
            return theNewTime.truncatedTo(ChronoUnit.SECONDS);
        } else {
            return theNewTime.truncatedTo(ChronoUnit.MINUTES);
        }
    }

    public void recalc() {
        if (iAmClear) return;

        int theHours = theNewTime.getHour();
        String theHoursString = String.valueOf(theHours);
        if(MemoryBank.appOpts.timeFormat == AppOptions.TimeFormat.MILITARY) {
            if(theHours < 10) theHoursString = "0" + theHoursString;
            if(theHours == 0) theHoursString = "00";
        } else {
            if(theHours > 12) theHoursString = String.valueOf(theHours - 12);
            if(theHours == 0) theHoursString = "12";
        }

        hoursButton.setText(theHoursString);

        int theMinutes = theNewTime.getMinute();
        String theMinutesString = String.valueOf(theMinutes);
        if(theMinutes < 10) theMinutesString = "0" + theMinutesString;
        minutesButton.setText(theMinutesString);

        if (showSeconds) {
            int theSeconds = theNewTime.getSecond();
            String theSecondsString = String.valueOf(theSeconds);
            if(theSeconds < 10) theSecondsString = "0" + theSecondsString;
            secondsButton.setText(theSecondsString);
            secondsButton.setVisible(true);
        } else {
            secondsButton.setVisible(false);
        } // end if

        // Check AM / PM
        int meridian = Calendar.AM;
        if (theNewTime.getHour() > 11) {
            meridian = Calendar.PM;
        }
        Color c;
        if (meridian == Calendar.AM) {
            c = MemoryBank.amColor;
            ampmButton.setText("AM");
        } else {
            c = MemoryBank.pmColor;
            ampmButton.setText("PM");
        } // end if

        hoursButton.setForeground(c);
        minutesButton.setForeground(c);
        secondsButton.setForeground(c);

    } // end recalc

    void setShowSeconds() {
        showSeconds = true;
        recalc();
    } // end setShowSeconds

    // Inner class
    //----------------------------------------------------------------
    class TimeAlterButton extends JLabel implements MouseListener {
        private static final long serialVersionUID = 1L;

        private boolean iAmDepressed;
        private Depressed dp;   // A Thread to keep responding
        ChronoUnit timeField;

        // The 't' is Calendar.   HOUR, MINUTE, or SECOND
        TimeAlterButton(ChronoUnit t) {
            timeField = t;
            setHorizontalAlignment(JLabel.CENTER);
            setBorder(up);
            setFont(Font.decode("Dialog-bold-38"));
            setToolTipText("Use right mouse button to decrease");

            iAmDepressed = false;
            addMouseListener(this);
        } // end constructor

        public Dimension getPreferredSize() {
            Dimension d = super.getPreferredSize();
            d.width = 60;
            return d;
        } // end getPreferredSize

        //---------------------------------------------------------
        // MouseListener methods
        //---------------------------------------------------------
        public void mouseClicked(MouseEvent e) {
        } // end mouseClicked

        public void mouseEntered(MouseEvent e) {
        } // end mouseEntered

        public void mouseExited(MouseEvent e) {
            if (iAmDepressed) dp.stopit();
            iAmDepressed = false;
            setBorder(up);
        } // end mouseExited

        public void mousePressed(MouseEvent e) {
            int direction;

            iAmDepressed = true;
            setBorder(down);

            // Determine whether it is the left or right mouse button.
            //int m = e.getModifiersEx();
            //if ((m & InputEvent.BUTTON3_DOWN_MASK) != 0) direction = -1;
            if(e.getButton() == MouseEvent.BUTTON3) direction = -1;
            else direction = 1;

            // Since a Thread will die upon return from the run method,
            //  need to start a new one each time.
            dp = new Depressed(timeField, direction);
            dp.start();
        } // end mousePressed

        public void mouseReleased(MouseEvent e) {
            if (iAmDepressed) dp.stopit();
            iAmDepressed = false;
            setBorder(up);
        } // end mouseReleased
        //---------------------------------------------------------
    } // end class TimeAlterButton

    class Depressed extends Thread {
        private int delay = 300; // milliseconds
        private boolean iAmRunning;
        ChronoUnit timeField;
        private final int direction;

        Depressed(ChronoUnit t, int d) {
            timeField = t;
            direction = d;
            iAmRunning = true;
        } // end constructor

        // A thread 'dies' upon return from run.
        public void run() {
            while (iAmRunning) {
                if(direction == 1) {
                    theNewTime = theNewTime.plus(1, timeField);
                } else {
                    theNewTime = theNewTime.minus(1, timeField);
                }
                recalc();

                try {
                    //noinspection BusyWait
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

} // end class TimeChooser
